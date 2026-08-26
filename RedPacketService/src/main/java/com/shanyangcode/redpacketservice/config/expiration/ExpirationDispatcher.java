package com.shanyangcode.redpacketservice.config.expiration;

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

    @Value("${redpacket.expire.batchSize:500}")
    private int batchSize;

    @Value("${redpacket.expire.maxBatchesPerTick:5}")
    private int maxBatchesPerTick;

    @Value("${redpacket.expire.timeBudgetMs:400}")
    private long timeBudgetMs;

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
            while (batches < maxBatchesPerTick && (System.currentTimeMillis() - startTime) < timeBudgetMs) {
                List<String> expiredIds = stringRedisTemplate.execute(
                        scanExpiredRedPacketsScript,
                        Collections.singletonList(RedisKeyConstant.EXPIRE_ZSET),
                        "0",
                        String.valueOf(batchSize)
                );

                if (expiredIds == null || expiredIds.isEmpty()) {
                    break;
                }

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
