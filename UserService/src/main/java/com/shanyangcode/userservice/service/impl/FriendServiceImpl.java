package com.shanyangcode.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.CommonConstant;
import com.shanyangcode.common.constant.SessionTypeConstant;
import com.shanyangcode.common.exception.BusinessException;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.common.utils.SnowflakeUtil;
import com.shanyangcode.common.enums.FriendStatusEnum;
import com.shanyangcode.userservice.constant.UserConstant;
import com.shanyangcode.userservice.constant.UserStateEnum;
import com.shanyangcode.userservice.mapper.ApplyFriendMapper;
import com.shanyangcode.userservice.mapper.FriendMapper;
import com.shanyangcode.userservice.mapper.SessionMapper;
import com.shanyangcode.userservice.mapper.UserSessionMapper;
import com.shanyangcode.userservice.model.dto.FriendDTO;
import com.shanyangcode.userservice.model.dto.ModifyFriendApplicationResponse;
import com.shanyangcode.userservice.model.dto.NewSessionNotificationDTO;
import com.shanyangcode.userservice.model.dto.PageRequest;
import com.shanyangcode.userservice.model.entity.*;
import com.shanyangcode.userservice.model.vo.FriendDetailVO;
import com.shanyangcode.userservice.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {

    /**
     * Redis Key 前缀（与 MessageValidationServiceImpl 保持一致）
     */
    private static final String FRIEND_STATUS_KEY_PREFIX = "msg:validate:friend:status:";
    /**
     * 会话用户角色常量
     */
    private static final int USER_ROLE_NORMAL = 2;
    /**
     * 会话状态常量
     */
    private static final int SESSION_STATUS_NORMAL = 0;
    private final UserService userService;
    private final SessionMapper sessionMapper;
    private final UserSessionMapper userSessionMapper;
    private final FriendMapper friendMapper;
    private final ApplyFriendMapper applyFriendMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SessionService sessionService;
    private final NotificationService notificationService;
    private final UserSessionService userSessionService;

    public FriendServiceImpl(UserService userService,
                             SessionMapper sessionMapper,
                             UserSessionMapper userSessionMapper,
                             FriendMapper friendMapper,
                             ApplyFriendMapper applyFriendMapper,
                             StringRedisTemplate stringRedisTemplate,
                             SessionService sessionService,
                             NotificationService notificationService,
                             UserSessionService userSessionService) {
        this.userService = userService;
        this.sessionMapper = sessionMapper;
        this.userSessionMapper = userSessionMapper;
        this.friendMapper = friendMapper;
        this.applyFriendMapper = applyFriendMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sessionService = sessionService;
        this.notificationService = notificationService;
        this.userSessionService = userSessionService;
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

        populateSessionId(userId1, friendId1, friendDetailVO);

        populateFriendStatus(userId1, friendId1, friendDetailVO);

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
    private void populateSessionId(Long userId, Long friendId, FriendDetailVO friendDetailVO) {
        // 1. 查找两个用户共同的单聊会话
        LambdaQueryWrapper<UserSession> userSession1Wrapper = new LambdaQueryWrapper<>();
        userSession1Wrapper.eq(UserSession::getUserId, userId)
                .eq(UserSession::getStatus, CommonConstant.SESSION_STATUS);
        List<UserSession> userSessions1 = userSessionMapper.selectList(userSession1Wrapper);

        LambdaQueryWrapper<UserSession> userSession2Wrapper = new LambdaQueryWrapper<>();
        userSession2Wrapper.eq(UserSession::getUserId, friendId)
                .eq(UserSession::getStatus, CommonConstant.SESSION_STATUS);
        List<UserSession> userSessions2 = userSessionMapper.selectList(userSession2Wrapper);

        List<Long> sessionIds1 = userSessions1.stream().map(UserSession::getSessionId).collect(Collectors.toList());
        List<Long> sessionIds2 = userSessions2.stream().map(UserSession::getSessionId).collect(Collectors.toList());

        sessionIds1.retainAll(sessionIds2);
        if (!sessionIds1.isEmpty()) {
            LambdaQueryWrapper<Session> sessionWrapper = new LambdaQueryWrapper<>();
            sessionWrapper.in(Session::getSessionId, sessionIds1)
                    .eq(Session::getType, SessionTypeConstant.SIGNAL_TYPE)
                    .orderByDesc(Session::getUpdatedTime)
                    .last("LIMIT 1");
            List<Session> sessions = sessionMapper.selectList(sessionWrapper);

            if (!sessions.isEmpty()) {
                friendDetailVO.setSessionId(String.valueOf(sessions.get(0).getSessionId()));
            } else {
                friendDetailVO.setSessionId(null);
            }
        } else {
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
    private void populateFriendStatus(Long userId, Long friendId, FriendDetailVO friendDetailVO) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);

        Friend friend = this.getOne(wrapper);

        if (friend != null) {
            friendDetailVO.setStatus(friend.getStatus());
        } else {
            friendDetailVO.setStatus(FriendStatusEnum.NON_FRIEND.getCode());
        }
    }

    @Override
    public IPage<FriendDTO> getFriends(String userId1, PageRequest pageRequest, String key) {
        Long userId = parseUserId(userId1);
        validateUserId(userId);

        int pageNum = pageRequest.getPageNum();
        int pageSize = pageRequest.getPageSize();

        // 1. 查询好友关系列表（使用Lambda Wrapper）
        LambdaQueryWrapper<Friend> friendWrapper = new LambdaQueryWrapper<>();
        friendWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getStatus, FriendStatusEnum.NORMAL.getCode())
                .orderByDesc(Friend::getCreatedTime);// 按创建时间降序排列

        List<Friend> friendList = friendMapper.selectList(friendWrapper);

        // 2. 获取好友ID列表，从好友列表中提取所有好友的ID，组成一个新的ID列表。
        List<Long> friendIds = friendList.stream().map(Friend::getFriendId).collect(Collectors.toList());

        if (friendIds.isEmpty()) {
            Page<FriendDTO> emptyPage = new Page<>(pageNum, pageSize);
            emptyPage.setTotal(0);
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        //3.查询好友用户信息
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(User::getUserId, friendIds);

        //如果有搜索关键字，添加搜索条件
        if (StringUtils.hasText(key)) {
            userWrapper.and(wrapper -> wrapper
                    .like(User::getNickname, key)
                    .or()
                    .like(User::getPhone, key));
        }

        List<User> users = userService.list(userWrapper);

        // 4. 查询好友与当前用户之间的会话ID映射（批量查询优化）
        Map<Long, String> friendSessionMap = buildFriendSessionMap(userId, friendIds);

        // 5. 构建好友ID到好友关系的映射（避免嵌套stream，O(n+m)），n = friendList 的大小（好友关系数量），m = users 的大小（查询到的好友用户信息数量）
        Map<Long, Friend> friendRelationMap = friendList.stream().collect(Collectors.toMap(Friend::getFriendId, f -> f));

        //6.构建FriendDTO列表
        List<FriendDTO> friendDTOList = users.stream()
                .map(user -> {
                    FriendDTO dto = new FriendDTO();
                    dto.setUserId(String.valueOf(user.getUserId())); // 设置dto的ID（好友用户ID）
                    dto.setNickname(user.getNickname());
                    dto.setAvatar(user.getAvatar());
                    dto.setSignature(user.getDescription());
                    Friend friendRelation = friendRelationMap.get(user.getUserId());
                    dto.setStatus(friendRelation != null ? friendRelation.getStatus() : FriendStatusEnum.NON_FRIEND.getCode()); // 如果存在好友关系则取其状态，否则标记为非好友
                    dto.setSessionId(friendSessionMap.get(user.getUserId())); // 从会话映射中获取该用户与当前用户的聊天会话ID
                    return dto;
                })
                .collect(Collectors.toList());

        // 7. 手动分页
        // 创建分页对象：初始化页码和每页大小
        Page<FriendDTO> page = new Page<>(pageNum, pageSize);
        page.setTotal(friendDTOList.size());

        // 计算索引范围：根据页码计算起始和结束位置，防止越界
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, friendDTOList.size());

        // 截取数据：使用 subList 提取当前页数据，若超出范围则返回空列表
        if (fromIndex < friendDTOList.size()) {
            page.setRecords(friendDTOList.subList(fromIndex, toIndex));
        } else {
            page.setRecords(List.of());
        }

        return page;

    }

    /**
     * 校验用户ID的有效性
     *
     * @param userId 用户ID
     */
    private void validateUserId(Long userId) {
        ThrowUtils.throwIf(userId == null || userId < 0, ErrorCode.PARAMS_ERROR, "用户ID无效");
    }

    /**
     * 批量构建好友与会话ID的映射关系
     * <p>
     * 优化查询性能，避免N+1问题
     *
     * @param userId    当前用户ID
     * @param friendIds 好友ID列表
     * @return 好友ID到会话ID的映射
     */
    private Map<Long, String> buildFriendSessionMap(Long userId, List<Long> friendIds) {
        Map<Long, String> friendSessionMap = new HashMap<>();
        if (friendIds.isEmpty()) {
            return friendSessionMap;
        }

        //查询当前用户所有的UserSession
        LambdaQueryWrapper<UserSession> currentUserSessionWrapper = new LambdaQueryWrapper<>();
        currentUserSessionWrapper.eq(UserSession::getUserId, userId);
        List<UserSession> currentUserSessions = userSessionMapper.selectList(currentUserSessionWrapper);

        if (currentUserSessions.isEmpty()) {
            return friendSessionMap;
        }

        // 2. 获取当前用户的所有会话ID
        // 把上面的 currentUserSessions 对象转换成会话ID列表
        List<Long> currentUserSessionIds = currentUserSessions.stream().map(UserSession::getSessionId).collect(Collectors.toList());

        // 3. 查询这些会话的详细信息，筛选出单聊会话（SIGNAL_TYPE）
        LambdaQueryWrapper<Session> sessionWrapper = new LambdaQueryWrapper<>();
        sessionWrapper.in(Session::getSessionId, currentUserSessionIds)
                .eq(Session::getType, SessionTypeConstant.SIGNAL_TYPE);
        List<Session> singleChatSessions = sessionMapper.selectList(sessionWrapper);

        if (singleChatSessions.isEmpty()) {
            return friendSessionMap;
        }

        //4.获取单聊会话id列表
        List<Long> singleChatSessionIds = singleChatSessions.stream().map(Session::getSessionId).collect(Collectors.toList());

        //5.查询所有好友在这些会话中的 UserSession 记录
        LambdaQueryWrapper<UserSession> friendSessionWrapper = new LambdaQueryWrapper<>();
        friendSessionWrapper.in(UserSession::getUserId, friendIds)
                .in(UserSession::getSessionId, singleChatSessionIds);
        List<UserSession> friendUserSessions = userSessionMapper.selectList(friendSessionWrapper);

        // 6. 构建 friendId -> sessionId 映射
        for (UserSession friendUserSession : friendUserSessions) {
            Long friendId = friendUserSession.getUserId();
            Long sessionId = friendUserSession.getSessionId();

            friendSessionMap.put(friendId, String.valueOf(sessionId));
        }
        return friendSessionMap;

    }

//=========================================================================================================================

    /**
     * 删除好友关系
     * <p>
     * 处理流程：
     * 1. 删除双向好友关系
     * 2. 删除相关的好友申请记录
     * 3. 删除会话和用户会话关系
     * 4. 清除好友关系缓存
     *
     * @param userId   当前用户ID
     * @param friendId 好友ID
     * @return 删除是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFriend(Long userId, Long friendId) {
        validateUserId(userId);
        validateUserId(friendId);

        // 不能删除自己
        ThrowUtils.throwIf(userId.equals(friendId), ErrorCode.OPERATION_ERROR, "不能删除自己");

        // 1. 删除好友关系记录，返回删除行数用于校验好友关系是否存在
        int deleted = deleteFriendRecords(userId, friendId);
        ThrowUtils.throwIf(deleted == 0, ErrorCode.OPERATION_ERROR, "对方不是你的好友");

        // 2. 删除好友申请记录
        deleteApplyFriendRecords(userId, friendId);

        // 3. 删除会话记录
        deleteSessionRecords(userId, friendId);

        // 4. 删除双向好友关系缓存
        evictFriendCache(userId, friendId);

        return true;
    }

    /**
     * 删除好友申请记录（使用Lambda Wrapper）
     *
     * @param userId   用户ID
     * @param friendId 好友ID
     */
    private void deleteApplyFriendRecords(Long userId, Long friendId) {
        LambdaQueryWrapper<ApplyFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .nested(nested -> nested.eq(ApplyFriend::getSenderId, userId)
                        .eq(ApplyFriend::getReceiverId, friendId))
                .or()
                .nested(nested -> nested.eq(ApplyFriend::getSenderId, friendId)
                        .eq(ApplyFriend::getReceiverId, userId))
        );
        applyFriendMapper.delete(wrapper);
    }

    /**
     * 删除好友关系记录（使用Lambda Wrapper）
     *
     * @param userId   用户ID
     * @param friendId 好友ID
     */
    private int deleteFriendRecords(Long userId, Long friendId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .nested(nested -> nested.eq(Friend::getUserId, userId)
                        .eq(Friend::getFriendId, friendId))
                .or()
                .nested(nested -> nested.eq(Friend::getUserId, friendId)
                        .eq(Friend::getFriendId, userId))
        );
        return friendMapper.delete(wrapper);
    }


    /**
     * 删除会话记录（使用Lambda Wrapper）
     *
     * @param userId   用户ID
     * @param friendId 好友ID
     */
    private void deleteSessionRecords(Long userId, Long friendId) {
        // 1. 查找两个用户共同的单聊会话
        LambdaQueryWrapper<UserSession> userSession1Wrapper = new LambdaQueryWrapper<>();
        userSession1Wrapper.eq(UserSession::getUserId, userId);
        List<UserSession> userSessions1 = userSessionMapper.selectList(userSession1Wrapper);

        LambdaQueryWrapper<UserSession> userSession2Wrapper = new LambdaQueryWrapper<>();
        userSession2Wrapper.eq(UserSession::getUserId, friendId);
        List<UserSession> userSessions2 = userSessionMapper.selectList(userSession2Wrapper);

        // 2. 找出共同的会话ID
        List<Long> sessionIds1 = userSessions1.stream()
                .map(UserSession::getSessionId)
                .collect(Collectors.toList());
        List<Long> sessionIds2 = userSessions2.stream()
                .map(UserSession::getSessionId)
                .collect(Collectors.toList());

        sessionIds1.retainAll(sessionIds2);
        List<Long> commonSessionIds = sessionIds1;

        // 3. 筛选出单聊会话
        if (!commonSessionIds.isEmpty()) {
            LambdaQueryWrapper<Session> sessionWrapper = new LambdaQueryWrapper<>();
            sessionWrapper.in(Session::getSessionId, commonSessionIds)
                    .eq(Session::getType, SessionTypeConstant.SIGNAL_TYPE);
            List<Session> sessions = sessionMapper.selectList(sessionWrapper);

            List<Long> singleChatSessionIds = sessions.stream()
                    .map(Session::getSessionId)
                    .collect(Collectors.toList());

            if (!singleChatSessionIds.isEmpty()) {
                // 4. 删除用户会话关系
                LambdaQueryWrapper<UserSession> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(UserSession::getSessionId, singleChatSessionIds);
                userSessionMapper.delete(wrapper);

                // 5. 删除会话
                LambdaQueryWrapper<Session> sessionDeleteWrapper = new LambdaQueryWrapper<>();
                sessionDeleteWrapper.in(Session::getSessionId, singleChatSessionIds);
                sessionMapper.delete(sessionDeleteWrapper);
            }
        }
    }

    /**
     * 清除好友关系缓存（双向）
     * <p>
     * 在好友关系发生变更时调用，确保缓存与数据库的一致性：
     * - 拉黑好友后
     * - 取消拉黑后
     * - 删除好友后
     *
     * @param userId   用户 ID
     * @param friendId 好友 ID
     */
    private void evictFriendCache(Long userId, Long friendId) {
        try {
            String key1 = FRIEND_STATUS_KEY_PREFIX + userId + ":" + friendId;
            String key2 = FRIEND_STATUS_KEY_PREFIX + friendId + ":" + userId;
            stringRedisTemplate.delete(Arrays.asList(key1, key2));
            log.info("已清除好友关系缓存: {} <-> {}", userId, friendId);
        } catch (Exception e) {
            // 缓存清除失败不影响主流程，记录日志即可
            log.warn("清除好友关系缓存失败: {} <-> {}, 原因: {}", userId, friendId, e.getMessage());
        }
    }


    //==========================================================================================================

    /**
     * 拉黑好友
     *
     * @param userId   当前用户ID
     * @param friendId 好友ID
     * @return 更新是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean blockFriend(Long userId, Long friendId) {
        validateUserId(userId);
        validateUserId(friendId);

        // 不能拉黑自己
        ThrowUtils.throwIf(userId.equals(friendId), ErrorCode.OPERATION_ERROR, "不能拉黑自己");

        // 1. 检查好友关系是否存在
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);
        Friend friend = this.getOne(queryWrapper);

        ThrowUtils.throwIf(friend == null, ErrorCode.NOT_FOUND_ERROR, "好友关系不存在");

        // 2. 使用Lambda Wrapper更新好友状态为拉黑（Friend表使用复合主键，不能使用updateById）
        LambdaUpdateWrapper<Friend> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Friend::getStatus, FriendStatusEnum.BLOCKED.getCode())
                .set(Friend::getUpdatedTime, LocalDateTime.now())
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);

        boolean result = this.update(updateWrapper);

        // 3. 删除双向好友关系缓存
        evictFriendCache(userId, friendId);

        return result;
    }


    /**
     * 取消拉黑好友
     *
     * @param userId   当前用户ID
     * @param friendId 好友ID
     * @return 更新是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unblockFriend(Long userId, Long friendId) {
        validateUserId(userId);
        validateUserId(friendId);

        // 1. 检查好友关系是否存在
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);
        Friend friend = this.getOne(queryWrapper);

        ThrowUtils.throwIf(friend == null, ErrorCode.NOT_FOUND_ERROR, "好友关系不存在");
        ThrowUtils.throwIf(friend.getStatus() != FriendStatusEnum.BLOCKED.getCode(),
                ErrorCode.OPERATION_ERROR, "该好友未被拉黑");

        // 2. 使用Lambda Wrapper更新好友状态为正常（Friend表使用复合主键，不能使用updateById）
        LambdaUpdateWrapper<Friend> updateWrapper =
                new LambdaUpdateWrapper<>();
        updateWrapper.set(Friend::getStatus, FriendStatusEnum.NORMAL.getCode())
                .set(Friend::getUpdatedTime, LocalDateTime.now())
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);

        boolean result = this.update(updateWrapper);

        // 3. 删除双向好友关系缓存
        evictFriendCache(userId, friendId);

        return result;
    }

    /**
     * 添加好友关系
     * <p>
     * 处理流程：
     * 1. 验证用户存在性
     * 2. 检查是否已是好友关系
     * 3. 创建双向好友关系
     * 4. 创建会话和用户会话关系
     * 5. 发送Kafka通知
     *
     * @param recipient 接收好友请求的用户
     * @param friendId  申请添加的好友ID
     * @return ModifyFriendApplicationResponse 响应对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModifyFriendApplicationResponse addFriend(User recipient, Long friendId) {
        validateUserId(friendId);
        ThrowUtils.throwIf(recipient == null, ErrorCode.NOT_FOUND_ERROR, "接收者用户不存在");

        // 1. 验证申请者用户是否存在
        User applicant = userService.getById(friendId);
        ThrowUtils.throwIf(applicant == null, ErrorCode.NOT_FOUND_ERROR, "好友申请者不存在");

        // 2. 检查是否已是好友关系
        boolean exists = this.lambdaQuery()
                .eq(Friend::getUserId, recipient.getUserId())
                .eq(Friend::getFriendId, friendId)
                .exists();
        ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "已经是好友关系");

        // 3. 创建双向好友关系
        createFriendRelations(recipient.getUserId(), friendId);
        evictFriendCache(recipient.getUserId(), friendId);

        // 4. 创建会话
        Long sessionId = createSession();

        // 5. 创建用户会话关系
        createUserSessions(recipient.getUserId(), friendId, sessionId);

        // 6. 发送Kafka通知给申请者
        sendNewSessionNotification(friendId, recipient, sessionId);

        // 7. 构建响应对象
        return buildModifyFriendApplicationResponse(applicant, sessionId);
    }


    /**
     * 创建双向好友关系
     *
     * @param userId   当前用户ID
     * @param friendId 好友ID
     */
    private void createFriendRelations(Long userId, Long friendId) {
        // 1. 创建第一条好友关系
        Friend friend1 = new Friend();
        friend1.setUserId(userId);
        friend1.setFriendId(friendId);
        friend1.setStatus(FriendStatusEnum.NORMAL.getCode());
        friend1.setCreatedTime(LocalDateTime.now());
        friend1.setUpdatedTime(LocalDateTime.now());

        // 2. 创建第二条好友关系
        Friend friend2 = new Friend();
        friend2.setUserId(friendId);
        friend2.setFriendId(userId);
        friend2.setStatus(FriendStatusEnum.NORMAL.getCode());
        friend2.setCreatedTime(LocalDateTime.now());
        friend2.setUpdatedTime(LocalDateTime.now());

        // 3. 使用Mapper的insert方法插入
        int inserted1 = friendMapper.insert(friend1);
        int inserted2 = friendMapper.insert(friend2);

        ThrowUtils.throwIf(inserted1 <= 0 || inserted2 <= 0, ErrorCode.SYSTEM_ERROR, "添加好友关系失败");
    }


    /**
     * 创建会话
     *
     * @return 会话ID
     */

    private Long createSession() {
        Long sessionId = SnowflakeUtil.nextId();
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setName("");
        session.setType(SessionTypeConstant.SIGNAL_TYPE);
        session.setStatus(SESSION_STATUS_NORMAL);
        session.setCreatedTime(new Date());
        session.setUpdatedTime(new Date());

        boolean sessionSaved = sessionService.save(session);
        ThrowUtils.throwIf(!sessionSaved, ErrorCode.SYSTEM_ERROR, "创建会话失败");

        return sessionId;
    }


    /**
     * 创建用户会话关系
     *
     * @param userId    当前用户ID
     * @param friendId  好友ID
     * @param sessionId 会话ID
     */
    private void createUserSessions(Long userId, Long friendId, Long sessionId) {
        // 1. 创建第一条用户会话关系
        UserSession userSession1 = new UserSession();
        userSession1.setUserId(userId);
        userSession1.setSessionId(sessionId);
        userSession1.setRole(USER_ROLE_NORMAL);
        userSession1.setStatus(SESSION_STATUS_NORMAL);
        userSession1.setCreatedTime(new Date());
        userSession1.setUpdatedTime(new Date());

        // 2. 创建第二条用户会话关系
        UserSession userSession2 = new UserSession();
        userSession2.setUserId(friendId);
        userSession2.setSessionId(sessionId);
        userSession2.setRole(USER_ROLE_NORMAL);
        userSession2.setStatus(SESSION_STATUS_NORMAL);
        userSession2.setCreatedTime(new Date());
        userSession2.setUpdatedTime(new Date());

        boolean userSessionSaved1 = userSessionService.save(userSession1);
        boolean userSessionSaved2 = userSessionService.save(userSession2);

        ThrowUtils.throwIf(!userSessionSaved1 || !userSessionSaved2,
                ErrorCode.SYSTEM_ERROR, "创建用户会话关系失败");
    }


    /**
     * 发送新会话通知（通过Kafka）
     *
     * @param recipientId 接收者ID
     * @param sender      发送者用户对象
     * @param sessionId   会话ID
     */
    private void sendNewSessionNotification(Long recipientId, User sender, Long sessionId) {
        try {
            NewSessionNotificationDTO notification = new NewSessionNotificationDTO();
            notification.setSessionName(sender.getNickname());
            notification.setAvatar(sender.getAvatar());

            notificationService.pushNewSession(sender.getUserId(), recipientId, sessionId, SessionTypeConstant.SIGNAL_TYPE, notification);
            log.info("发送新会话通知成功，接收者ID：{}，会话ID：{}", recipientId, sessionId);
        } catch (Exception e) {
            log.warn("发送新会话通知失败，接收者ID：{}，会话ID：{}，原因：{}",
                    recipientId, sessionId, e.getMessage());
        }
    }

    /**
     * 构建 ModifyFriendApplicationResponse 响应对象
     *
     * @param applicant 申请者User实体
     * @param sessionId 会话ID
     * @return ModifyFriendApplicationResponse 对象
     */
    private ModifyFriendApplicationResponse buildModifyFriendApplicationResponse(User applicant, Long sessionId) {
        ModifyFriendApplicationResponse response = new ModifyFriendApplicationResponse();
        response.setUserId(String.valueOf(applicant.getUserId()));
        response.setSessionId(String.valueOf(sessionId));
        response.setSessionType(SessionTypeConstant.SIGNAL_TYPE);
        response.setSessionName(applicant.getNickname());
        response.setAvatar(applicant.getAvatar());
        return response;
    }
}
