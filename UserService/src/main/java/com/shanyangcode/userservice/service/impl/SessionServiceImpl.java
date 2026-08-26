package com.shanyangcode.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.SessionTypeConstant;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.common.utils.SnowflakeUtil;
import com.shanyangcode.common.enums.FriendStatusEnum;
import com.shanyangcode.userservice.mapper.FriendMapper;
import com.shanyangcode.userservice.mapper.SessionMapper;
import com.shanyangcode.userservice.mapper.UserSessionMapper;
import com.shanyangcode.userservice.model.dto.NewGroupSessionNotificationDTO;
import com.shanyangcode.userservice.model.dto.request.CreateGroupRequest;
import com.shanyangcode.userservice.model.dto.response.CreateGroupResponse;
import com.shanyangcode.userservice.model.entity.Friend;
import com.shanyangcode.userservice.model.entity.Session;
import com.shanyangcode.userservice.model.entity.User;
import com.shanyangcode.userservice.model.entity.UserSession;
import com.shanyangcode.userservice.service.NotificationService;
import com.shanyangcode.userservice.service.SessionService;
import com.shanyangcode.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {

    /**
     * 用户角色常量
     */
    private static final int USER_ROLE_GROUP_OWNER = 0;   // 群主
    private static final int USER_ROLE_GROUP_MEMBER = 2;  // 群成员

    /**
     * 用户状态常量
     */
    private static final int USER_STATUS_NORMAL = 0;

    /**
     * 会话状态常量
     */
    private static final int SESSION_STATUS_NORMAL = 0;

    /**
     * 默认群头像URL
     */
    private static final String DEFAULT_GROUP_AVATAR_URL = "https://video.shanyangcode.com/image/default/A9C9C83CCE043EC8253DB5D7545DCB4-6-2.png";
    private final UserService userService;
    private final FriendMapper friendMapper;
    private final SessionMapper sessionMapper;
    private final UserSessionMapper userSessionMapper;
    private final NotificationService notificationService;


    public SessionServiceImpl(UserService userService,
                              FriendMapper friendMapper,
                              SessionMapper sessionMapper,
                              UserSessionMapper userSessionMapper,
                              NotificationService notificationService) {
        this.userService = userService;
        this.friendMapper = friendMapper;
        this.sessionMapper = sessionMapper;
        this.userSessionMapper = userSessionMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateGroupResponse createGroup(CreateGroupRequest request) {
        Long creatorId = request.getCreatorId();
        List<Long> memberIds = request.getMemberIds();
        List<String> failedMemberIds = new ArrayList<>();

        //1. 参数校验（含去重、排除创建者本人）
        memberIds = validateCreateGroupParameters(creatorId, memberIds);

        //2. 确认创建者用户存在且状态正常
        getActiveUserById(creatorId);

        //3. 验证好友关系并获取有效成员ID（使用Lambda Wrapper）
        List<Long> validMemberIds = validateAndFilterMembers(creatorId, memberIds, failedMemberIds);

        ThrowUtils.throwIf(validMemberIds.isEmpty(), ErrorCode.OPERATION_ERROR, "没有有效的好友可加入群聊");

        //4. 生成 sessionId
        Long sessionId = SnowflakeUtil.nextId();

        //5. 生成群名称
        String groupName = generateGroupName(creatorId, validMemberIds);

        //6. 插入 session 表
        Session session = createSession(sessionId, groupName);
        sessionMapper.insert(session);

        // 7. 插入 user_session 表 - 创建者
        insertUserSession(sessionId, creatorId, USER_ROLE_GROUP_OWNER);

        // 8. 计算成员数量 = 有效成员数 + 群主
        int membersCount = validMemberIds.size() + 1;

        // 9. 构建推送新群会话消息
        NewGroupSessionNotificationDTO notification = buildNewGroupSessionNotification(creatorId, groupName, membersCount);

        // 10. 插入 user_session 表 - 其他成员并推送 Kafka 通知
        insertMembersAndPushNotifications(validMemberIds, sessionId, notification);

        // 11. 响应结果
        CreateGroupResponse response = new CreateGroupResponse();
        BeanUtils.copyProperties(notification, response);
        response.setCreatorId(String.valueOf(creatorId));
        response.setSessionId(String.valueOf(sessionId));
        response.setSessionType(SessionTypeConstant.GROUP_TYPE);
        response.setFailedMemberIds(failedMemberIds);
        return response;
    }

    private List<Long> validateCreateGroupParameters(Long creatorId, List<Long> memberIds) {
        ThrowUtils.throwIf(creatorId == null, ErrorCode.PARAMS_ERROR, "创建者ID不能为空");
        ThrowUtils.throwIf(memberIds == null || memberIds.isEmpty(),
                ErrorCode.PARAMS_ERROR, "成员ID列表不能为空");

        // 去重并排除创建者本人，防止重复插入 user_session / 自我入群
        List<Long> normalizedMemberIds = memberIds.stream()
                .distinct()
                .filter(id -> id != null && !creatorId.equals(id))
                .collect(Collectors.toList());
        ThrowUtils.throwIf(normalizedMemberIds.isEmpty(),
                ErrorCode.PARAMS_ERROR, "成员ID列表不能为空（仅包含创建者本人）");
        return normalizedMemberIds;
    }

    private User getActiveUserById(Long userId) {
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null || user.getState() != USER_STATUS_NORMAL,
                ErrorCode.NOT_FOUND_ERROR, "用户不存在或状态异常");
        return user;
    }

    private List<Long> validateAndFilterMembers(Long creatorId, List<Long> memberIds, List<String> failedMemberIds) {
        // 获取创建者所有好友 ID（使用 Lambda Wrapper）
        LambdaQueryWrapper<Friend> friendWrapper = new LambdaQueryWrapper<>();
        friendWrapper.eq(Friend::getUserId, creatorId)
                .eq(Friend::getStatus, FriendStatusEnum.NORMAL.getCode());
        List<Friend> friends = friendMapper.selectList(friendWrapper);

        Set<Long> friendIdSet = friends.stream().map(Friend::getFriendId).collect(Collectors.toSet());

        List<Long> validMemberIds = new ArrayList<>();

        for (Long memberId : memberIds) {
            if (friendIdSet.contains(memberId)) {
                validMemberIds.add(memberId);
            } else {
                failedMemberIds.add(String.valueOf(memberId));
                log.info("成员ID {} 不是创建者的好友，无法加入群聊", memberId);
            }
        }
        return validMemberIds;
    }

    private String generateGroupName(Long creatorId, List<Long> memberIds) {
        StringBuilder groupNameBuilder = new StringBuilder();
        List<Long> allMemberIds = new ArrayList<>(memberIds);
        allMemberIds.add(0, creatorId);

        //查询所有用户信息
        List<User> users = userService.listByIds(allMemberIds);

        // 构建 ID -> User 映射，确保顺序可控
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));

        // 按照 allMemberIds 的顺序拼接用户名
        for (Long userId : allMemberIds) {
            User user = userMap.get(userId);
            if (user != null) {
                if (groupNameBuilder.length() > 0) {
                    groupNameBuilder.append("、");
                }
                groupNameBuilder.append(user.getNickname());
                if (groupNameBuilder.length() >= 16) {
                    groupNameBuilder.setLength(16);
                    break;
                }
            }
        }
        return groupNameBuilder.toString();
    }

    private Session createSession(Long sessionId, String groupName) {
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setName(groupName);
        session.setType(SessionTypeConstant.GROUP_TYPE);   // 1
        session.setStatus(SESSION_STATUS_NORMAL);          // 0
        session.setAvatar(DEFAULT_GROUP_AVATAR_URL);
        session.setCreatedTime(new Date());
        session.setUpdatedTime(new Date());
        return session;
    }

    private void insertUserSession(Long sessionId, Long userId, int role) {
        UserSession userSession = new UserSession();
        userSession.setUserId(userId);
        userSession.setSessionId(sessionId);
        userSession.setRole(role);
        userSession.setStatus(SESSION_STATUS_NORMAL);
        userSession.setCreatedTime(new Date());
        userSession.setUpdatedTime(new Date());
        userSessionMapper.insert(userSession);
    }

    private void insertMembersAndPushNotifications(List<Long> memberIds,
                                                   Long sessionId,
                                                   NewGroupSessionNotificationDTO notification) {
        for (Long memberId : memberIds) {
            insertUserSession(sessionId, memberId, USER_ROLE_GROUP_MEMBER);

            //推送 Kafka 通知
            try {
                notificationService.pushGroupNewSession(memberId, sessionId, notification);
            } catch (Exception e) {
                log.error("推送群聊会话失败，成员ID {}，错误信息：{}", memberId, e.getMessage());
            }
        }
    }

    private NewGroupSessionNotificationDTO buildNewGroupSessionNotification(Long creatorId, String groupName, int membersCount) {
        NewGroupSessionNotificationDTO notification = new NewGroupSessionNotificationDTO();
        notification.setCreatorId(creatorId);
        notification.setSessionName(groupName);
        notification.setMembersCount(membersCount);
        notification.setAvatar(DEFAULT_GROUP_AVATAR_URL);
        return notification;
    }
}
