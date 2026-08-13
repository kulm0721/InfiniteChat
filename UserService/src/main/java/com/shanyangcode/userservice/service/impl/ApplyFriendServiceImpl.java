package com.shanyangcode.userservice.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.KafkaTopicConstant;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.common.utils.SnowflakeUtil;
import com.shanyangcode.userservice.constant.FriendApplicationStatusEnum;
import com.shanyangcode.userservice.constant.FriendStatusEnum;
import com.shanyangcode.userservice.mapper.ApplyFriendMapper;
import com.shanyangcode.userservice.model.dto.FriendApplicationNotificationDTO;
import com.shanyangcode.userservice.model.dto.FriendRequestCreationEvent;
import com.shanyangcode.userservice.model.entity.ApplyFriend;
import com.shanyangcode.userservice.model.entity.Friend;
import com.shanyangcode.userservice.model.entity.User;
import com.shanyangcode.userservice.service.ApplyFriendService;
import com.shanyangcode.userservice.service.FriendService;
import com.shanyangcode.userservice.service.NotificationService;
import com.shanyangcode.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ApplyFriendServiceImpl extends ServiceImpl<ApplyFriendMapper, ApplyFriend> implements ApplyFriendService {
    /**
     * 好友申请过期时间（24小时）
     */
    private static final long FRIEND_REQUEST_EXPIRATION_HOURS = 24L;
    private final UserService userService;
    private final FriendService friendService;
    private final NotificationService notificationService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ApplyFriendServiceImpl(FriendService friendService,
                                  UserService userService,
                                  NotificationService notificationService,
                                  KafkaTemplate<String, String> kafkaTemplate) {
        this.friendService = friendService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 发送好友申请
     * <p>
     * 处理流程：
     * 1. 验证发送者和接收者用户是否存在，是否为同一个用户
     * 2. 检查是否已经是好友关系
     * 3. 检查是否已有待处理的申请
     * 4a. 没有：插入新申请记录，同时异步发出 Kafka 通知（通知链路 + 过期链路）
     * 4b. 有且已通过：返回"已是好友"
     * 4c. 有但其它状态（已读 / 已拒绝 / 已过期）：复用记录、状态回写为 UNREAD、附言更新，同时异步发出 Kafka 通知（通知链路 + 过期链路）
     * 5. 返回 applyFriendId
     *
     * @param senderId   发送者用户ID
     * @param receiverId 接收者用户ID
     * @param message    申请消息
     * @return 好友申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendFriendRequest(Long senderId, Long receiverId, String message) {
        //1. 验证用户存在性
        User sender = userService.getById(senderId);
        ThrowUtils.throwIf(sender == null, ErrorCode.NOT_FOUND_ERROR, "发送者用户不存在");

        User receiver = userService.getById(receiverId);
        ThrowUtils.throwIf(receiver == null, ErrorCode.NOT_FOUND_ERROR, "接收者用户不存在");

        // 检查是否为同一个用户
        ThrowUtils.throwIf(senderId.equals(receiverId), ErrorCode.OPERATION_ERROR, "不能添加自己");

        //2. 检查是否已经是好友关系且是否拉黑对方
        Friend friend = friendService.lambdaQuery()
                .eq(Friend::getUserId, senderId)
                .eq(Friend::getFriendId, receiverId)
                .one();

        //已是正常好友
        ThrowUtils.throwIf(friend != null && friend.getStatus() == FriendStatusEnum.NORMAL.getCode(), ErrorCode.OPERATION_ERROR, "已经是好友关系，无需重复添加");

        //已拉黑对方
        ThrowUtils.throwIf(friend != null && friend.getStatus() == FriendStatusEnum.BLOCKED.getCode(), ErrorCode.FORBIDDEN_ERROR, "你已将对方拉黑，无法添加");

        //3. 检查是否已有待处理的申请
        LambdaQueryWrapper<ApplyFriend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApplyFriend::getSenderId, senderId)
                .eq(ApplyFriend::getReceiverId, receiverId);

        ApplyFriend existingApplyFriend = this.getOne(queryWrapper);

        Long applyFriendId = null;
        if (existingApplyFriend == null) {
            // 4a. 没有：插入新申请记录，同时异步发出 Kafka 通知（通知链路 + 过期链路）
            applyFriendId = handleNewFriendApplication(senderId, receiverId, message, sender);
        } else if (existingApplyFriend.getStatus().equals(FriendApplicationStatusEnum.ACCEPTED.getCode())) {
            // 4b. 有且已通过：返回"已是好友"
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "已经是好友关系，无需重复添加");
        } else {
            // 4c. 有但其它状态（已读 / 已拒绝 / 已过期）：复用记录、状态回写为 UNREAD、附言更新，同时异步发出 Kafka 通知（通知链路 + 过期链路）
            applyFriendId = handleExistingFriendApplication(existingApplyFriend, message, sender);
        }

        return applyFriendId;
    }

    /**
     * 处理新的好友申请
     *
     * @param senderId   发送者ID
     * @param receiverId 接收者ID
     * @param message    申请消息
     * @param sender     发送者用户对象
     * @return 好友申请ID
     */
    private Long handleNewFriendApplication(Long senderId, Long receiverId, String message, User sender) {
        //1.创建好友申请记录
        ApplyFriend applyFriend = new ApplyFriend();
        Long applyFriendId = SnowflakeUtil.nextId();
        applyFriend.setApplyFriendId(applyFriendId);
        applyFriend.setSenderId(senderId);
        applyFriend.setReceiverId(receiverId);
        applyFriend.setMessage(message);
        applyFriend.setStatus(FriendApplicationStatusEnum.UNREAD.getCode());
        applyFriend.setCreatedTime(LocalDateTime.now());
        applyFriend.setUpdatedTime(LocalDateTime.now());

        boolean saved = this.save(applyFriend);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "创建好友申请失败");

        //2. 发送Kafka通知（异步）
        sendFriendApplicationNotification(receiverId, sender, message);

        //3. 发送过期任务注册事件（异步）
        registerExpirationTask(applyFriendId);

        return applyFriendId;
    }

    /**
     * 处理已有的好友申请
     *
     * @param existingApplyFriend 现有的好友申请
     * @param message             申请消息
     * @param sender              发送者用户对象
     * @return 好友申请ID
     */
    private Long handleExistingFriendApplication(ApplyFriend existingApplyFriend, String message, User sender) {
        //1. 更新好友申请记录
        LambdaUpdateWrapper<ApplyFriend> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(ApplyFriend::getStatus, FriendApplicationStatusEnum.UNREAD.getCode())
                .set(ApplyFriend::getMessage, message)
                .set(ApplyFriend::getUpdatedTime, LocalDateTime.now())
                .eq(ApplyFriend::getApplyFriendId, existingApplyFriend.getApplyFriendId());
        boolean updated = this.update(updateWrapper);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "更新好友申请失败");

        //2.发送Kafka通知（异步）
        sendFriendApplicationNotification(existingApplyFriend.getReceiverId(), sender, message);

        //3.重新注册过期任务（异步）
        registerExpirationTask(existingApplyFriend.getApplyFriendId());

        return existingApplyFriend.getApplyFriendId();
    }


    /**
     * 发送好友申请通知（通过Kafka）
     *
     * @param receiverId 接收者ID
     * @param sender     发送者用户对象
     * @param message    申请附言
     */
    private void sendFriendApplicationNotification(Long receiverId, User sender, String message) {
        try {
            FriendApplicationNotificationDTO notification = new FriendApplicationNotificationDTO();
            notification.setApplyUserName(sender.getNickname());
            notification.setApplyUserId(sender.getUserId());
            notification.setApplyFriendAvatar(sender.getAvatar());
            notification.setMessage(message);

            notificationService.pushNewApply(receiverId, notification);
            log.info("发送好友申请通知成功，接收者ID：{}，发送者ID：{}", receiverId, sender.getUserId());
        } catch (Exception e) {
            log.warn("发送好友申请通知失败，接收者ID：{}，发送者ID：{}，原因：{}",
                    receiverId, sender.getUserId(), e.getMessage());
        }
    }


    /**
     * 注册好友申请过期任务（通过Kafka）
     *
     * @param applyFriendId 好友申请ID
     */
    private void registerExpirationTask(Long applyFriendId) {
        try {
            // 计算过期时间（当前时间 + 24小时）
            long createTime = System.currentTimeMillis();
            long expireTime = createTime + (FRIEND_REQUEST_EXPIRATION_HOURS * 60 * 60 * 1000);

            //构建过期事件
            FriendRequestCreationEvent event = new FriendRequestCreationEvent();
            event.setApplyFriendId(applyFriendId);
            event.setCreateTime(createTime);
            event.setExpireTime(expireTime);

            // 发送到Kafka topic
            kafkaTemplate.send(
                    KafkaTopicConstant.TOPIC_FRIEND_REQUEST_CREATION,
                    String.valueOf(applyFriendId),
                    JSONUtil.toJsonStr(event)
            );

            log.info("注册好友申请过期任务成功，申请ID：{}，过期时间：{}", applyFriendId, expireTime);
        } catch (Exception e) {
            log.error("注册好友申请过期任务失败，申请ID：{}，原因：{}", applyFriendId, e.getMessage());
        }
    }


}
