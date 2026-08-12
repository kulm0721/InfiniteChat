package com.shanyangcode.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.CommonConstant;
import com.shanyangcode.common.constant.SessionTypeConstant;
import com.shanyangcode.common.exception.BusinessException;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.userservice.constant.FriendStatusEnum;
import com.shanyangcode.userservice.constant.UserConstant;
import com.shanyangcode.userservice.constant.UserStateEnum;
import com.shanyangcode.userservice.mapper.FriendMapper;
import com.shanyangcode.userservice.mapper.SessionMapper;
import com.shanyangcode.userservice.mapper.UserSessionMapper;
import com.shanyangcode.userservice.model.entity.Friend;
import com.shanyangcode.userservice.model.entity.Session;
import com.shanyangcode.userservice.model.entity.User;
import com.shanyangcode.userservice.model.entity.UserSession;
import com.shanyangcode.userservice.model.vo.FriendDetailVO;
import com.shanyangcode.userservice.service.FriendService;
import com.shanyangcode.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {

    private final UserService userService;
    private final SessionMapper sessionMapper;
    private final UserSessionMapper userSessionMapper;

    public FriendServiceImpl(UserService userService, SessionMapper sessionMapper, UserSessionMapper userSessionMapper) {
        this.userService = userService;
        this.sessionMapper = sessionMapper;
        this.userSessionMapper = userSessionMapper;
    }

    /**
     * 根据关键字搜索用户（自动识别手机号或邮箱）
     *
     * @param userId  当前用户ID
     * @param keyword 搜索关键字（手机号或邮箱）
     * @return FriendDetailVO 对象
     */
    @Override
    public FriendDetailVO searchUserByKeyword(String userId, String keyword) {
        ThrowUtils.throwIf(!StringUtils.hasText(keyword), ErrorCode.PARAMS_ERROR, "搜索关键字不能为空");

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword.matches(UserConstant.PHONE_REGEX)) {
            queryWrapper.eq(User::getPhone, keyword);
        } else if (keyword.matches(UserConstant.EMAIL_REGEX)) {
            queryWrapper.eq(User::getEmail, keyword);
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入有效的手机号或邮箱");
        }

        User user = userService.getOne(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        return getFriendDetails(userId, String.valueOf(user.getUserId()));
    }

    /**
     * 获取好友的详细信息
     *
     * @param userId   当前用户Id
     * @param friendId 好友Id
     * @return FriendDetailVO 对象
     */
    @Override
    public FriendDetailVO getFriendDetails(String userId, String friendId) {
        Long userId1 = parseUserId(userId);
        Long friendId1 = parseUserId(friendId);

        User friendUser = userService.getById(friendId1);
        validateFriendUser(friendUser);

        FriendDetailVO friendDetailVO = buildFriendDetailVO(friendUser);

        populateSessionId(userId1,friendId1,friendDetailVO);

        populateFriendStatus(userId1,friendId1,friendDetailVO);

        return friendDetailVO;
    }

    /**
     * 解析并验证用户ID
     *
     * @param userId 用户Id字符串
     * @return 解析后的用户ID
     */
    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID格式错误");
        }
    }

    /**
     * 验证好友用户是否存在及其状态
     *
     * @param friendUser 好友的User实体
     */
    private void validateFriendUser(User friendUser) {
        ThrowUtils.throwIf(friendUser == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 用户状态：0 正常，1 封禁，2 注销
        ThrowUtils.throwIf(friendUser.getState() == UserStateEnum.BANNED.getCode(), ErrorCode.FORBIDDEN_ERROR, "该用户已被封禁");

        ThrowUtils.throwIf(friendUser.getState() == UserStateEnum.CANCELLED.getCode(), ErrorCode.NOT_FOUND_ERROR, "该用户已注销");
    }

    /**
     * 构建好友详细信息VO
     *
     * @param friendUser 好友的User实体
     * @return FriendDetailVO 对象
     */
    private FriendDetailVO buildFriendDetailVO(User friendUser) {
        FriendDetailVO vo = new FriendDetailVO();
        vo.setUserId(String.valueOf(friendUser.getUserId()));
        vo.setNickname(friendUser.getNickname());
        vo.setAvatar(friendUser.getAvatar());
        vo.setEmail(friendUser.getEmail());
        vo.setPhone(friendUser.getPhone());
        vo.setSignature(friendUser.getDescription());
        vo.setGender(friendUser.getGender());
        return vo;
    }

    /**
     * 填充会话ID到FriendDetailVO
     *
     * @param userId         当前用户ID
     * @param friendId       好友ID
     * @param friendDetailVO FriendDetailVO 对象
     */
    private void populateSessionId(Long userId,Long friendId,FriendDetailVO friendDetailVO) {
        // 1. 查找两个用户共同的单聊会话
        LambdaQueryWrapper<UserSession> userSession1Wrapper = new LambdaQueryWrapper<>();
        userSession1Wrapper.eq(UserSession::getUserId, userId)
                .eq(UserSession::getStatus, CommonConstant.SESSION_STATUS);
        List<UserSession> userSessions1= userSessionMapper.selectList(userSession1Wrapper);

        LambdaQueryWrapper<UserSession> userSession2Wrapper = new LambdaQueryWrapper<>();
        userSession2Wrapper.eq(UserSession::getUserId, friendId)
                .eq(UserSession::getStatus, CommonConstant.SESSION_STATUS);
        List<UserSession> userSessions2 = userSessionMapper.selectList(userSession2Wrapper);

        List<Long> sessionIds1=userSessions1.stream().map(UserSession::getSessionId).collect(Collectors.toList());
        List<Long> sessionIds2=userSessions2.stream().map(UserSession::getSessionId).collect(Collectors.toList());

        sessionIds1.retainAll(sessionIds2);
        if(!sessionIds1.isEmpty()) {
            LambdaQueryWrapper<Session> sessionWrapper = new LambdaQueryWrapper<>();
            sessionWrapper.in(Session::getSessionId, sessionIds1)
                    .eq(Session::getType, SessionTypeConstant.SIGNAL_TYPE)
                    .orderByDesc(Session::getUpdatedTime)
                    .last("LIMIT 1");
            List<Session> sessions = sessionMapper.selectList(sessionWrapper);

            if(!sessions.isEmpty()) {
                friendDetailVO.setSessionId(String.valueOf(sessions.get(0).getSessionId()));
            }else{
                friendDetailVO.setSessionId(null);
            }
        }else{
            friendDetailVO.setSessionId(null);
        }
    }


    /**
     * 填充好友状态到FriendDetailVO
     *
     * @param userId         当前用户ID
     * @param friendId       好友ID
     * @param friendDetailVO FriendDetailVO 对象
     */
    private void populateFriendStatus(Long userId,Long friendId,FriendDetailVO friendDetailVO) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId,friendId);

        Friend friend=this.getOne(wrapper);

        if(friend!=null){
            friendDetailVO.setStatus(friend.getStatus());
        }else{
            friendDetailVO.setStatus(FriendStatusEnum.NON_FRIEND.getCode());
        }
    }
}
