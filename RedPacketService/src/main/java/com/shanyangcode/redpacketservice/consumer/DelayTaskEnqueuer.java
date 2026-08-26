package com.shanyangcode.redpacketservice.consumer;

import cn.hutool.json.JSONUtil;
import com.shanyangcode.redpacketservice.config.KafkaConfig;
import com.shanyangcode.redpacketservice.constant.KafkaConstant;
import com.shanyangcode.redpacketservice.constant.RedPacketConstant;
import com.shanyangcode.redpacketservice.constant.RedisKeyConstant;
import com.shanyangcode.redpacketservice.model.dto.RedPacketCreationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 延迟任务注册器
 * <p>
 * 负责监听红包创建事件，将过期时间注册到Redis ZSET中
 * <p>
 * 这是时间轮算法的第一步：将延迟任务入队
 */
@Component
@Slf4j
public class DelayTaskEnqueuer {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 消费红包创建事件
     * <p>
     * Topic: redpacket-creation-topic
     * <p>
     * 消息格式：
     * <pre>
     * {
     *     "redPacketId": 1001,
     *     "createTime": 1678886400000
     * }
     * </pre>
     * <p>
     * 处理逻辑：
     * 1. 计算红包的精确过期时间：expireTimestamp = createTime + 24小时
     * 2. 将红包ID和过期时间添加到Redis ZSET中
     * 3. ZADD redpacket-expire-zset {expireTimestamp} {redPacketId}
     *
     * @param message Kafka消息
     */

    @KafkaListener(
            topics = KafkaConfig.TOPIC_REDPACKET_CREATION,
            groupId = KafkaConstant.GROUP_DELAY_TASK_ENQUEUER,
            concurrency = KafkaConstant.DEFAULT_CONCURRENCY
    )
    public void handleRedPacketCreation(String message) {
        try {
            log.info("收到红包创建事件: {}", message);

            // 解析消息
            RedPacketCreationEvent event = JSONUtil.toBean(message, RedPacketCreationEvent.class);
            Long redPacketId = event.getRedPacketId();
            Long createTime = event.getCreateTime();

            // 计算过期时间戳
            long expireTimestamp = createTime + RedPacketConstant.EXPIRE_TIME_MS;

            // 注册到Redis ZSET
            stringRedisTemplate.opsForZSet().add(
                    RedisKeyConstant.EXPIRE_ZSET,
                    String.valueOf(redPacketId),
                    expireTimestamp
            );

            log.info("红包过期任务注册成功，红包ID: {}, 创建时间: {}, 过期时间: {}",
                    redPacketId, createTime, expireTimestamp);

        } catch (Exception e) {
            log.error("处理红包创建事件失败: {}", message, e);
            // 注意：如果处理失败，可以抛出异常触发Kafka重试
            // 这里捕获异常只是为了防止整个消费者组崩溃
            // 实际生产环境建议配合死信队列使用
        }
    }
}
