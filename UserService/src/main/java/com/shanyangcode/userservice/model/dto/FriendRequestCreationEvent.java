package com.shanyangcode.userservice.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 好友申请创建事件DTO
 *
 * 功能说明：
 * - 好友申请创建时发送到Kafka
 * - 消费者接收后注册到Redis ZSET延迟队列
 *
 * Topic: friend-request-creation-topic
 */
@Data
public class FriendRequestCreationEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 好友申请ID
     */
    private Long applyFriendId;

    /**
     * 创建时间戳（毫秒）
     */
    private Long createTime;

    /**
     * 过期时间戳（毫秒）
     * 默认：createTime + 24小时
     */
    private Long expireTime;
}
