package com.shanyangcode.redpacketservice.consumer;

import com.shanyangcode.redpacketservice.config.KafkaConfig;
import com.shanyangcode.redpacketservice.constant.KafkaConstant;
import com.shanyangcode.redpacketservice.service.RedPacketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 红包过期任务执行器
 * <p>
 * 负责消费红包过期事件，执行退款逻辑
 * <p>
 * 这是时间轮算法的最后一步：执行到期任务
 *
 */
@Component
@Slf4j
public class ExpirationTaskExecutor {
    @Autowired
    private RedPacketService redPacketService;

    /**
     * 消费红包过期事件
     * <p>
     * Topic: redpacket-expiration-topic
     * <p>
     * 消息格式：红包ID（String）
     * <p>
     * 处理逻辑：
     * 1. 查询红包信息，校验状态
     * 2. 使用Lua脚本计算Redis中的剩余金额
     * 3. 如果有剩余金额，退款给发送者：
     *    - 增加发送者余额
     *    - 记录余额变动日志（type=2 红包退回）
     * 4. 更新红包状态为"已过期"（status=2）
     * 5. 清理Redis缓存（金额池、领取记录、过期ZSET）
     *
     * @param redPacketId 红包ID
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_REDPACKET_EXPIRATION,
            groupId = KafkaConstant.GROUP_EXPIRATION_EXECUTOR,
            concurrency = KafkaConstant.DEFAULT_CONCURRENCY
    )
    public void handleRedPacketExpiration(String redPacketId) {
        try {
            log.info("收到红包过期事件，红包ID: {}", redPacketId);

            // 解析红包ID
            Long id = Long.parseLong(redPacketId);

            // 调用服务层处理过期逻辑
            redPacketService.handleRedPacketExpiration(id);

            log.info("红包过期事件处理成功，红包ID: {}", redPacketId);

        } catch (NumberFormatException e) {
            log.error("红包ID格式错误: {}", redPacketId, e);
        } catch (Exception e) {
            log.error("处理红包过期事件失败，红包ID: {}", redPacketId, e);
            // 注意：这里可以根据需要决定是否抛出异常来触发重试
            // 如果抛出异常，Kafka会根据配置进行重试
        }
    }
}
