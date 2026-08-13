package com.shanyangcode.userservice.service;

import com.shanyangcode.userservice.model.dto.FriendApplicationNotificationDTO;

/**
 * 通知推送服务接口
 * <p>
 * 功能说明：
 * - 通过Kafka异步发送系统通知消息
 * - 由RealTimeService消费并推送给在线用户
 * - 支持多种类型的系统通知
 * - 符合IM项目通知消息设计方案
 */
public interface NotificationService {
    /**\
     * 推送好友申请通知
     * <p>
     * 场景：用户收到新的好友申请
     *
     * @param userId       接收通知的用户ID
     * @param notification 好友申请通知信息
     */
    void pushNewApply(Long userId, FriendApplicationNotificationDTO notification);
}
