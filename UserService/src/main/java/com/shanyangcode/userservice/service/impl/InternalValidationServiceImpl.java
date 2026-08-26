package com.shanyangcode.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanyangcode.common.constant.UserStateConstant;
import com.shanyangcode.common.constant.ValidationRedisKey;
import com.shanyangcode.common.enums.FriendStatusEnum;
import com.shanyangcode.common.enums.UserSessionStatusEnum;
import com.shanyangcode.common.enums.ValidationError;
import com.shanyangcode.common.model.dto.validation.UserStatusResponse;
import com.shanyangcode.userservice.mapper.FriendMapper;
import com.shanyangcode.userservice.mapper.UserMapper;
import com.shanyangcode.userservice.mapper.UserSessionMapper;
import com.shanyangcode.userservice.model.entity.Friend;
import com.shanyangcode.userservice.model.entity.User;
import com.shanyangcode.userservice.model.entity.UserSession;
import com.shanyangcode.userservice.service.InternalValidationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.shanyangcode.common.model.dto.validation.*;

import java.util.concurrent.TimeUnit;

/**
 * 内部校验服务实现
 * <p>
 * 提供用户状态查询和群成员校验功能
 */
@Service
@Slf4j
public class InternalValidationServiceImpl implements InternalValidationService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private UserSessionMapper userSessionMapper;

    @Resource
    private FriendMapper friendMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public UserStatusResponse getUserStatus(Long userId) {
        log.debug("查询用户状态, userId={}", userId);

        User user = userMapper.selectById(userId);
        UserStatusResponse response = new UserStatusResponse();
        response.setUserId(userId);

        if (user == null) {
            response.setState(null);
            response.setStateName("用户不存在");
            log.debug("用户不存在, userId={}", userId);
        } else {
            response.setState(user.getState());
            response.setStateName(UserStateConstant.getStateName(user.getState()));
            log.debug("用户状态查询成功, userId={}, state={}", userId, user.getState());
        }
        return response;
    }

    @Override
    public GroupMemberCountResponse getGroupMemberCount(Long sessionId) {
        log.debug("获取群成员数量, sessionId={}", sessionId);

        LambdaQueryWrapper<UserSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSession::getSessionId, sessionId);
        wrapper.eq(UserSession::getStatus, UserSessionStatusEnum.NORMAL.getCode());
        Long count = userSessionMapper.selectCount(wrapper);

        GroupMemberCountResponse response = new GroupMemberCountResponse();
        response.setSessionId(sessionId);
        response.setMemberCount(count.intValue());

        log.debug("群成员数量查询成功, sessionId={}, memberCount={}", sessionId, count);
        return response;
    }

    @Override
    public GroupMembershipResponse checkGroupMembership(Long userId, Long sessionId) {
        log.debug("验证群成员资格, userId={}, sessionId={}", userId, sessionId);

        LambdaQueryWrapper<UserSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSession::getUserId, userId)
                .eq(UserSession::getSessionId, sessionId)
                .eq(UserSession::getStatus, UserSessionStatusEnum.NORMAL.getCode());
        UserSession userSession = userSessionMapper.selectOne(wrapper);

        GroupMembershipResponse response = new GroupMembershipResponse();
        response.setUserId(userId);
        response.setSessionId(sessionId);
        response.setIsMember(userSession != null);
        response.setRole(userSession != null ? userSession.getRole() : null);

        log.debug("群成员资格验证完成, userId={}, sessionId={}, isMember={}",
                userId, sessionId, userSession != null);
        return response;
    }

    @Override
    public MessageValidateResponse validateSingleMessage(SingleMessageValidateRequest request) {
        Long senderId = request.getSenderId();
        Long receiverId = request.getReceiverId();

        // 1. 校验好友关系是否存在（发送者视角）
        Integer senderToReceiverStatus = getFriendStatusWithCache(senderId, receiverId);
        if (senderToReceiverStatus == null) {
            return MessageValidateResponse.reject(
                    ValidationError.NOT_FRIEND.getCode(),
                    ValidationError.NOT_FRIEND.name());
        }
        if (senderToReceiverStatus == FriendStatusEnum.DELETED.getCode()) {
            return MessageValidateResponse.reject(
                    ValidationError.FRIEND_DELETED.getCode(),
                    ValidationError.FRIEND_DELETED.name());
        }

        // 2. 核心校验：检查接收方是否拉黑了发送方
        // 正确逻辑：A 拉黑 B → B 不能给 A 发消息，但 A 可以给 B 发消息
        // 所以只需检查 receiverId → senderId 的状态
        Integer receiverToSenderStatus = getFriendStatusWithCache(receiverId, senderId);
        if (receiverToSenderStatus != null && receiverToSenderStatus == FriendStatusEnum.BLOCKED.getCode()) {
            return MessageValidateResponse.reject(
                    ValidationError.BLOCKED_BY_RECEIVER.getCode(),
                    ValidationError.BLOCKED_BY_RECEIVER.name());
        }

        log.debug("单聊消息校验通过: senderId={}, receiverId={}", senderId, receiverId);
        return MessageValidateResponse.allow();
    }

    /**
     * 获取好友关系状态（优先从 Redis，未命中则查库并写入缓存）
     *
     * @param userId   用户 ID
     * @param friendId 好友 ID
     * @return 好友状态（null 表示非好友）
     */
    private Integer getFriendStatusWithCache(Long userId, Long friendId) {
        String key = ValidationRedisKey.buildFriendStatusKey(userId, friendId);
        String cached = stringRedisTemplate.opsForValue().get(key);

        if (cached != null) {
            if (ValidationRedisKey.isNonFriendValue(cached)) {
                return null;  // 缓存的"非好友"状态
            }
            return Integer.parseInt(cached);
        }

        // 查询数据库
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);
        Friend friend = friendMapper.selectOne(wrapper);

        // 写入缓存（非好友用 -1 表示）
        String cacheValue = (friend == null)
                ? ValidationRedisKey.NON_FRIEND_CACHE_VALUE
                : String.valueOf(friend.getStatus());
        stringRedisTemplate.opsForValue().set(
                key,
                cacheValue,
                ValidationRedisKey.FRIEND_STATUS_TTL_MINUTES,
                TimeUnit.MINUTES);

        log.debug("好友关系缓存写入: key={}, value={}, ttl={}min",
                key, cacheValue, ValidationRedisKey.FRIEND_STATUS_TTL_MINUTES);

        return (friend == null) ? null : friend.getStatus();
    }


}
