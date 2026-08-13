package com.shanyangcode.userservice.service.impl;

import cn.hutool.json.JSONUtil;
import com.shanyangcode.common.constant.KafkaTopicConstant;
import com.shanyangcode.common.constant.MessageTypeConstant;
import com.shanyangcode.common.utils.SnowflakeUtil;
import com.shanyangcode.userservice.model.dto.FriendApplicationNotificationDTO;
import com.shanyangcode.userservice.model.dto.SystemNotificationMessage;
import com.shanyangcode.userservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


/**
 * 通知推送服务实现类
 * <p>
 * 实现说明：
 * - 使用Kafka异步发送系统通知消息
 * - 替代原有的HTTP同步调用方式
 * - 符合IM项目通知消息设计方案
 * - 提高系统性能和可靠性
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public NotificationServiceImpl(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 推送好友申请通知
     * <p>
     * 实现逻辑：
     * 1. 构建完整的SystemNotificationMessage
     * 2. 生成唯一messageId
     * 3. 设置senderId为申请人ID
     * 4. 发送到Kafka的system-notification-topic
     * 5. RealTimeService消费后推送给在线用户
     *
     * @param userId       接收通知的用户ID
     * @param notification 好友申请通知信息
     */
    @Override
    public void pushNewApply(Long userId, FriendApplicationNotificationDTO notification) {
        try {
            SystemNotificationMessage message = new SystemNotificationMessage();
            message.setMessageId(generateMessageId());
            message.setSessionId(null);
            message.setSenderId(notification.getApplyUserId());
            message.setReceiverId(userId);
            message.setType(MessageTypeConstant.TYPE_SYSTEM_NEW_APPLY);
            message.setSessionType(null);
            message.setTimestamp(System.currentTimeMillis());

            //构建body
            Map<String, Object> body = new HashMap<>();
            body.put("nickname", notification.getApplyUserName());
            body.put("avatar", notification.getApplyFriendAvatar());
            body.put("msg", notification.getMessage());
            message.setBody(body);

            sendNotification(message, "好友申请通知");
        } catch (Exception e) {
            log.error("发送好友申请通知失败，用户ID: {}, 错误: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 生成消息唯一ID
     * <p>
     * 格式：msg_{timestamp}_{snowflakeId}
     *
     * @return 消息ID
     */
    private String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + SnowflakeUtil.nextId();
    }

    /**
     * 发送通知消息到 Kafka
     *
     * @param message          系统通知消息
     * @param notificationName 通知名称（用于日志）
     */
    private void sendNotification(SystemNotificationMessage message, String notificationName) {
        String messageJson = JSONUtil.toJsonStr(message);

        kafkaTemplate.send(
                KafkaTopicConstant.TOPIC_SYSTEM_NOTIFICATION,
                String.valueOf(message.getReceiverId()),
                messageJson).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("{}发送成功，messageId: {}, 用户ID: {}, type: {}",
                        notificationName, message.getMessageId(), message.getReceiverId(), message.getType());
            } else {
                log.error("{}发送失败，messageId: {}, 用户ID: {}, 错误: {}",
                        notificationName, message.getMessageId(), message.getReceiverId(), ex.getMessage());
            }
        });
    }
}
