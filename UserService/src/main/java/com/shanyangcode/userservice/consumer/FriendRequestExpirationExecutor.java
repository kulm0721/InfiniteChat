package com.shanyangcode.userservice.consumer;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shanyangcode.common.constant.KafkaTopicConstant;
import com.shanyangcode.userservice.constant.FriendApplicationStatusEnum;
import com.shanyangcode.userservice.model.dto.FriendRequestExpirationEvent;
import com.shanyangcode.userservice.model.entity.ApplyFriend;
import com.shanyangcode.userservice.service.ApplyFriendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
/**
 * 好友申请过期事件消费者（过期处理执行器）
 *
 * 功能说明：
 * - 消费好友申请过期事件
 * - 执行过期逻辑：更新数据库中好友申请状态为"过期"
 * - 仅更新未被处理的申请（状态为UNREAD或READ）
 *
 * 数据流：
 * FriendRequestExpirationDispatcher扫描到过期任务 → Kafka: friend-request-expiration-topic
 * → 本Consumer消费 → 更新数据库状态为EXPIRED
 *
 * 注意事项：
 * - 已通过（ACCEPTED）或已拒绝（REJECTED）的申请不应再标记为过期
 * - 幂等性：如果状态已经是EXPIRED，则跳过处理
 */
@Slf4j
@Component
public class FriendRequestExpirationExecutor {

    private final ApplyFriendService applyFriendService;

    public FriendRequestExpirationExecutor(ApplyFriendService applyFriendService) {
        this.applyFriendService = applyFriendService;
    }


    /**
     * 消费好友申请过期事件，执行过期逻辑
     *
     * 处理流程：
     * 1. 解析Kafka消息，获取好友申请ID
     * 2. 查询数据库，获取好友申请记录
     * 3. 检查当前状态，只处理UNREAD和READ状态的申请
     * 4. 更新状态为EXPIRED
     * 5. 记录日志
     *
     * @param message Kafka消息（JSON格式的FriendRequestExpirationEvent）
     */

    @KafkaListener(
            topics = KafkaTopicConstant.TOPIC_FRIEND_REQUEST_EXPIRATION,
            groupId = KafkaTopicConstant.GROUP_FRIEND_REQUEST_EXECUTOR,
            concurrency = "3"  // 支持并发处理
    )
    public void executeExpiration(String message) {
        try{
            log.info("收到好友申请过期事件: {}", message);

            //1. 解析事件
            FriendRequestExpirationEvent event = JSONUtil.toBean(message, FriendRequestExpirationEvent.class);
            Long applyFriendId=event.getApplyFriendId();

            //2. 查询好友申请记录
            ApplyFriend applyFriend=applyFriendService.getById(applyFriendId);

            if(applyFriend==null) {
                log.warn("好友申请不存在，申请ID: {}", applyFriendId);
                return;
            }

            // 3. 检查当前状态
            Integer currentStatus = applyFriend.getStatus();

            if(currentStatus.equals(FriendApplicationStatusEnum.EXPIRED.getCode())) {
                // 已经是过期状态，幂等性处理
                log.info("好友申请已是过期状态，跳过处理，申请ID: {}", applyFriendId);
                return;
            }

            if(currentStatus.equals(FriendApplicationStatusEnum.ACCEPTED.getCode())) {
                // 已通过，不应标记为过期
                log.info("好友申请已通过，不应标记为过期，申请ID: {}", applyFriendId);
                return;
            }

            if (currentStatus.equals(FriendApplicationStatusEnum.REJECTED.getCode())) {
                // 已拒绝，不应标记为过期
                log.info("好友申请已拒绝，不应标记为过期，申请ID: {}", applyFriendId);
                return;
            }


            // 4. 更新状态为EXPIRED（只更新UNREAD和READ状态的申请）
            LambdaUpdateWrapper<ApplyFriend> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(ApplyFriend::getStatus,FriendApplicationStatusEnum.EXPIRED.getCode())
                    .eq(ApplyFriend::getApplyFriendId,applyFriendId)
                    .in(ApplyFriend::getStatus,FriendApplicationStatusEnum.UNREAD.getCode(),FriendApplicationStatusEnum.READ.getCode());

            boolean updated=applyFriendService.update(updateWrapper);


            if (updated) {
                log.info("好友申请已标记为过期，申请ID: {}", applyFriendId);
            } else {
                log.warn("好友申请状态更新失败（可能已被处理），申请ID: {}", applyFriendId);
            }
        }catch (Exception e){
            log.error("处理好友申请过期事件失败: {}, 错误: {}", message, e.getMessage(), e);
        }
    }
}
