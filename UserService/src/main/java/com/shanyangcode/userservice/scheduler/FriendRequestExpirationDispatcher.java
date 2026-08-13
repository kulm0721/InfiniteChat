package com.shanyangcode.userservice.scheduler;

import cn.hutool.json.JSONUtil;
import com.shanyangcode.common.constant.KafkaTopicConstant;
import com.shanyangcode.userservice.model.dto.FriendRequestExpirationEvent;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 好友申请过期任务调度器（时间轮调度）
 * <p>
 * 功能说明：
 * - 每秒执行一次，扫描Redis ZSET中已到期的好友申请
 * - 将已到期的好友申请ID投递到Kafka
 * - 由FriendRequestExpirationExecutor消费并执行过期逻辑
 * <p>
 * 实现原理（参考红包模块的时间轮算法）：
 * 1. Redis ZSET作为延迟任务池，score为过期时间戳
 * 2. 定时任务每秒"拨动时间轮"，扫描已到期的任务
 * 3. 使用Lua脚本原子性地获取并删除已到期的成员
 * 4. 将过期任务投递到Kafka，由独立的Consumer处理
 * <p>
 * 分布式协调：
 * - 使用ShedLock确保在集群环境下只有一个实例执行
 */
@Slf4j
@Component
public class FriendRequestExpirationDispatcher {
    private static final String ZSET_KEY = "friend-request-expire-zset"; // 延迟任务池 Redis ZSET key（score 为过期时间戳）
    private static final int BATCH_SIZE = 500;                           // 每批从 ZSET 取出的任务数量
    private static final int MAX_BATCHES_PER_TICK = 5;                   // 每个调度周期最多处理的批次数
    private static final long TIME_BUDGET_MS = 400;                      // 每个 tick 的时间预算上限（毫秒）

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DefaultRedisScript<List> scanExpiredScript;

    public FriendRequestExpirationDispatcher(
            StringRedisTemplate redisTemplate,
            KafkaTemplate<String, String> kafkaTemplate,
            DefaultRedisScript<List> scanExpiredScript) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.scanExpiredScript = scanExpiredScript;
    }


    /**
     * 定时扫描并投递过期任务
     * <p>
     * 执行策略：
     * - 执行频率：每秒1次（fixedRate = 1000ms）
     * - 分布式锁：lockAtMostFor = 800ms，确保任务不会被多个实例同时执行
     * - 批量处理：每次最多处理5批，每批最多500条
     * - 时间预算：单次执行不超过400ms，避免阻塞调度线程
     * <p>
     * 处理流程：
     * 1. 使用Lua脚本原子性地获取已到期的好友申请ID
     * 2. 遍历ID列表，逐条投递到Kafka
     * 3. 如果本批数据未满BATCH_SIZE，说明已拉空，退出循环
     * 4. 如果达到时间预算或批次限制，退出循环
     */
    @Scheduled(fixedRate = 1000)
    @SchedulerLock(
            name = "FriendRequestExpirationDispatcher",
            lockAtMostFor = "800ms",   // 最多锁定800ms
            lockAtLeastFor = "200ms"
    )
    public void dispatch() {
        long start = System.currentTimeMillis();
        int batches = 0;
        try {
            while (batches < MAX_BATCHES_PER_TICK && (System.currentTimeMillis() - start) < TIME_BUDGET_MS) {
                // 1. 执行Lua脚本，获取已到期的好友申请I
                List<String> expiredIds = redisTemplate.execute(
                        scanExpiredScript,
                        Collections.singletonList(ZSET_KEY),
                        "0",
                        String.valueOf(BATCH_SIZE)
                );
                if (expiredIds == null || expiredIds.isEmpty()) {
                    break;
                }
                log.info("扫描到{}个已过期的好友申请", expiredIds.size());

                // 2. 逐条投递到Kafka
                for (String applyFriendIdStr : expiredIds) {
                    try {
                        Long applyFriendId = Long.parseLong(applyFriendIdStr);

                        FriendRequestExpirationEvent event = new FriendRequestExpirationEvent();
                        event.setApplyFriendId(applyFriendId);
                        event.setExpireTime(System.currentTimeMillis());

                        String eventJson = JSONUtil.toJsonStr(event);
                        kafkaTemplate.send(
                                KafkaTopicConstant.TOPIC_FRIEND_REQUEST_EXPIRATION,
                                String.valueOf(applyFriendId),  // 使用applyFriendId作为key
                                eventJson
                        );
                        log.debug("好友申请过期事件已投递，申请ID: {}", applyFriendId);
                    } catch (Exception e) {
                        log.error("投递好友申请过期事件失败，申请ID: {}, 错误: {}",
                                applyFriendIdStr, e.getMessage(), e);
                    }
                }
                batches++;

                // 3. 如果本批数据未满，说明已拉空，退出
                if (expiredIds.size() < BATCH_SIZE) {
                    break;
                }
            }
            long elapsed = System.currentTimeMillis() - start;
            if (batches > 0) {
                log.info("好友申请过期任务调度完成，处理批次: {}, 耗时: {}ms", batches, elapsed);
            }
        } catch (Exception e) {
            log.error("好友申请过期任务调度失败: {}", e.getMessage(), e);
        }
    }

}
