package com.shanyangcode.userservice.controller;

import cn.hutool.json.JSONUtil;
import com.shanyangcode.common.constant.KafkaTopicConstant;
import com.shanyangcode.userservice.model.dto.FriendRequestCreationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 好友申请创建事件消费者（延迟任务注册器）
 * <p>
 * 功能说明：
 * - 消费好友申请创建事件
 * - 将好友申请ID注册到Redis ZSET延迟队列
 * - ZSET的score为过期时间戳，便于定时任务扫描
 * <p>
 * 数据流：
 * ApplyFriendService创建好友申请 → Kafka: friend-request-creation-topic
 * → 本Consumer消费 → Redis ZSET: friend-request-expire-zset
 */
@Slf4j
@Component
public class FriendRequestExpirationEnqueuer {
    private static final String ZSET_KEY = "friend-request-expire-zset";
    private final StringRedisTemplate redisTemplate;

    public FriendRequestExpirationEnqueuer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 消费好友申请创建事件，注册延迟过期任务
     * <p>
     * 处理流程：
     * 1. 解析Kafka消息，获取好友申请ID和过期时间
     * 2. 使用ZADD将好友申请ID添加到ZSET
     * 3. score设置为过期时间戳（毫秒）
     * 4. 定时任务会定期扫描ZSET，取出已到期的ID
     *
     * @param message Kafka消息（JSON格式的FriendRequestCreationEvent）
     */

    @KafkaListener(
            topics = KafkaTopicConstant.TOPIC_FRIEND_REQUEST_CREATION,
            groupId = KafkaTopicConstant.GROUP_FRIEND_REQUEST_ENQUEUER,
            concurrency = "1" //单线程消费保证顺序
    )
    public void enqueueExpirationTask(String message) {
        try {
            log.info("收到好友申请创建事件: {}", message);

            //1.解析事件
            FriendRequestCreationEvent event = JSONUtil.toBean(message, FriendRequestCreationEvent.class);
            Long applyFriendId = event.getApplyFriendId();
            Long expireTime = event.getExpireTime();

            if (applyFriendId == null || expireTime == null) {
                log.error("好友申请事件数据不完整，applyFriendId: {}, expireTime: {}", applyFriendId, expireTime);
                return;
            }

            if (expireTime <= System.currentTimeMillis()) {
                log.warn("好友申请已过期，跳过注册，申请ID: {}, 过期时间: {}", applyFriendId, expireTime);
                return;
            }

            // 2. 注册到 Redis ZSET
            // ZADD friend-request-expire-zset <expireTime> <applyFriendId>

            Boolean result = redisTemplate.opsForZSet().add(ZSET_KEY, String.valueOf(applyFriendId), expireTime.doubleValue());


            if (Boolean.TRUE.equals(result)) {
                log.info("好友申请过期任务注册成功，申请ID: {}, 过期时间: {}", applyFriendId, expireTime);
            } else {
                log.info("好友申请过期任务注册失败，申请ID: {}, 过期时间: {}", applyFriendId, expireTime);
            }
        } catch (Exception e) {
            log.error("处理好友申请创建事件失败: {}, 错误: {}", message, e.getMessage(), e);
        }
    }
}
