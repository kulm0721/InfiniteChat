package com.shanyangcode.redpacketservice.config.KafkaConfig;


import com.shanyangcode.redpacketservice.config.KafkaConfig;
import com.shanyangcode.redpacketservice.constant.RedisKeyConstant;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 红包过期调度器
 * <p>
 * 采用时间轮算法：定期扫描Redis ZSET，找出已到期的红包ID，投递到Kafka进行异步处理
 * <p>
 * 核心特性：
 * 1. 使用@Scheduled每秒执行一次
 * 2. 使用@ShedLock防止多实例重复执行
 * 3. 使用Lua脚本保证原子性（扫描+删除）
 * 4. 支持批量处理，避免单次扫描时间过长
 */
@Component
@Slf4j
public class ExpirationDispatcher {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private DefaultRedisScript<List> scanExpiredRedPacketsScript;

    /**
     * 每批最多扫描的红包数量
     */
    @Value("${redpacket.expire.batchSize:500}")
    private int batchSize;

    /**
     * 每轮最多处理的批次数
     */
    @Value("${redpacket.expire.maxBatchesPerTick:5}")
    private int maxBatchesPerTick;

    /**
     * 单次扫描的最大时间预算（毫秒）
     */
    @Value("${redpacket.expire.timeBudgetMs:400}")
    private long timeBudgetMs;

    /**
     * 扫描并投递到期红包任务
     * <p>
     * 执行频率：每秒一次（fixedRate = 1000ms）
     * <p>
     * 分布式锁配置：
     * - lockAtMostFor: 最多持有锁800ms（避免任务执行时间过长导致锁无法释放）
     * - lockAtLeastFor: 至少持有锁200ms（避免任务执行过快导致重复执行）
     * <p>
     * 处理逻辑：
     * 1. 使用Lua脚本扫描Redis ZSET，获取已到期的红包ID列表
     * 2. 从ZSET中删除这些红包ID（原子操作）
     * 3. 将每个红包ID发送到Kafka topic: redpacket-expiration-topic
     * 4. 支持批量处理，避免单次扫描时间过长
     */
    @Scheduled(fixedRate = 1000)
    @SchedulerLock(
            name = "dispatchExpiredRedPackets",
            lockAtMostFor = "800ms",
            lockAtLeastFor = "200ms"
    )
    public void dispatch() {
        final long startTime = System.currentTimeMillis();
        int totalExpiredCount = 0;
        int batches = 0;

        try {
            // 多批次处理，避免单次扫描时间过长
            while (batches < maxBatchesPerTick && (System.currentTimeMillis() - startTime) < timeBudgetMs) {

                // 执行Lua脚本：扫描到期的红包ID，发送成功后再删除
                // KEYS[1]: redpacket-expire-zset
                // ARGV[1]: 0（让脚本使用Redis TIME命令获取当前时间）
                // ARGV[2]: batchSize（本批次最多获取的数量）
                List<String> expiredIds = stringRedisTemplate.execute(
                        scanExpiredRedPacketsScript,
                        Collections.singletonList(RedisKeyConstant.EXPIRE_ZSET),
                        "0",
                        String.valueOf(batchSize)
                );

                // 如果没有到期的红包，退出循环
                if (expiredIds == null || expiredIds.isEmpty()) {
                    break;
                }

                // 将每个到期的红包ID投递到Kafka
                for (String redPacketId : expiredIds) {
                    try {
                        kafkaTemplate.send(
                                KafkaConfig.TOPIC_REDPACKET_EXPIRATION,
                                redPacketId,
                                redPacketId
                        ).whenComplete((result, failure) -> {
                            if (failure == null) {
                                stringRedisTemplate.opsForZSet().remove(
                                        RedisKeyConstant.EXPIRE_ZSET, redPacketId);
                            } else {
                                log.error("发送红包过期事件到Kafka失败，红包ID: {}", redPacketId, failure);
                            }
                        });
                    } catch (Exception e) {
                        log.error("发送红包过期事件到Kafka失败，红包ID: {}", redPacketId, e);
                    }
                }

                totalExpiredCount += expiredIds.size();
                batches++;

                log.info("第{}批扫描完成，本批到期红包数: {}", batches, expiredIds.size());

                // 如果本批次返回的数量小于batchSize，说明已经扫描完所有到期任务
                if (expiredIds.size() < batchSize) {
                    break;
                }
            }

            if (totalExpiredCount > 0) {
                long costTime = System.currentTimeMillis() - startTime;
                log.info("过期红包扫描完成，总共处理: {} 个，耗时: {} ms，批次数: {}",
                        totalExpiredCount, costTime, batches);
            }

        } catch (Exception e) {
            log.error("扫描过期红包任务执行失败", e);
        }
    }
}
