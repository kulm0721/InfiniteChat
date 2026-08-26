package com.shanyangcode.redpacketservice.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.KafkaTopicConstant;
import com.shanyangcode.common.constant.MessageTypeConstant;
import com.shanyangcode.common.constant.SessionTypeConstant;
import com.shanyangcode.common.exception.BusinessException;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.common.model.dto.MessageBody;
import com.shanyangcode.common.model.dto.MessageRequest;
import com.shanyangcode.common.utils.SnowflakeUtil;
import com.shanyangcode.redpacketservice.constant.BalanceLogConstant;
import com.shanyangcode.redpacketservice.constant.ReceiveResultConstant;
import com.shanyangcode.redpacketservice.constant.RedPacketConstant;
import com.shanyangcode.redpacketservice.constant.RedisKeyConstant;
import com.shanyangcode.redpacketservice.annotation.PreventDuplicateSubmit;
import com.shanyangcode.redpacketservice.config.KafkaConfig;
import com.shanyangcode.redpacketservice.mapper.BalanceLogMapper;
import com.shanyangcode.redpacketservice.mapper.RedPacketMapper;
import com.shanyangcode.redpacketservice.mapper.RedPacketReceiveMapper;
import com.shanyangcode.redpacketservice.mapper.UserBalanceMapper;
import com.shanyangcode.redpacketservice.model.dto.RedPacketBody;
import com.shanyangcode.redpacketservice.model.dto.RedPacketCreationEvent;
import com.shanyangcode.redpacketservice.model.dto.RedPacketReceiveRequest;
import com.shanyangcode.redpacketservice.model.dto.RedPacketSendRequest;
import com.shanyangcode.redpacketservice.model.entity.BalanceLog;
import com.shanyangcode.redpacketservice.model.entity.RedPacket;
import com.shanyangcode.redpacketservice.model.entity.RedPacketReceive;
import com.shanyangcode.redpacketservice.model.entity.UserBalance;
import com.shanyangcode.redpacketservice.model.vo.ReceiveResultVO;
import com.shanyangcode.redpacketservice.model.vo.RedPacketSendVO;
import com.shanyangcode.redpacketservice.service.RedPacketService;
import com.shanyangcode.redpacketservice.service.RedPacketValidationService;
import com.shanyangcode.redpacketservice.util.RedPacketAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RedPacketServiceImpl implements RedPacketService {
    private final RedPacketMapper redPacketMapper;

    private final UserBalanceMapper userBalanceMapper;

    private final BalanceLogMapper balanceLogMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final RedPacketValidationService redPacketValidationService;

    private final DefaultRedisScript<Long> calculateRemainAmountScript;

    private final DefaultRedisScript<List> receiveRedPacketScript;

    private final RedPacketReceiveMapper redPacketReceiveMapper;

    public RedPacketServiceImpl(RedPacketMapper redPacketMapper,
                                UserBalanceMapper userBalanceMapper,
                                BalanceLogMapper balanceLogMapper,
                                StringRedisTemplate stringRedisTemplate,
                                KafkaTemplate<String, String> kafkaTemplate,
                                RedPacketValidationService redPacketValidationService,
                                DefaultRedisScript<Long> calculateRemainAmountScript,
                                DefaultRedisScript<List> receiveRedPacketScript,
                                RedPacketReceiveMapper redPacketReceiveMapper) {
        this.redPacketMapper = redPacketMapper;
        this.userBalanceMapper = userBalanceMapper;
        this.balanceLogMapper = balanceLogMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.redPacketValidationService = redPacketValidationService;
        this.calculateRemainAmountScript = calculateRemainAmountScript;
        this.receiveRedPacketScript = receiveRedPacketScript;
        this.redPacketReceiveMapper = redPacketReceiveMapper;
    }

    @Override
    @PreventDuplicateSubmit
    @Transactional(rollbackFor = Exception.class)
    public RedPacketSendVO sendRedPacket(RedPacketSendRequest request) {
        // 1. 校验
        validateSendRequest(request);
        redPacketValidationService.validateSendPermission(request);

        // 2. 提取参数
        RedPacketBody body = request.getBody();
        Long senderId = request.getSenderId();
        Long totalAmount = body.getTotalAmount()
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact(); // 转为分
        Integer totalCount = body.getTotalCount();
        Integer redPacketType = body.getRedPacketType();

        // 3. 扣减发送者余额
        deductSenderBalance(senderId, totalAmount);

        // 4. 创建红包记录
        long redPacketId = createRedPacket(request, senderId, totalAmount, totalCount, redPacketType);

        // 5. 记录余额变动日志
        recordBalanceLog(senderId, -totalAmount, BalanceLogConstant.TYPE_SEND, redPacketId);

        Long messageId;
        try {
            // 6. 红包金额预分配并初始化 Redis 缓存
            initRedPacketCache(redPacketId, redPacketType, totalAmount, totalCount);

            // 7. 发送红包消息（消息持久化与推送）
            messageId = sendRedPacketMessage(request, redPacketId);

            // 8. 发送红包创建事件（过期处理注册）
            RedPacketCreationEvent creationEvent = new RedPacketCreationEvent();
            creationEvent.setRedPacketId(redPacketId);
            creationEvent.setCreateTime(System.currentTimeMillis());
            sendKafkaAndWait(KafkaConfig.TOPIC_REDPACKET_CREATION,
                    JSONUtil.toJsonStr(creationEvent));
        } catch (RuntimeException e) {
            cleanupRedPacketCache(redPacketId);
            throw e;
        }

        log.info("红包发送成功，红包ID: {}, 消息ID: {}, 发送者: {}, 金额: {}, 数量: {}",
                redPacketId, messageId, senderId, totalAmount, totalCount);

        // 9. 构建返回结果
        RedPacketSendVO result = new RedPacketSendVO();
        result.setRedPacketId(redPacketId);
        result.setMessageId(messageId);
        return result;

    }

    /**
     * 验证发送红包请求参数
     */
    private void validateSendRequest(RedPacketSendRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getBody() == null, ErrorCode.PARAMS_ERROR, "红包信息为空");
        ThrowUtils.throwIf(request.getSenderId() == null, ErrorCode.PARAMS_ERROR, "发送者ID为空");
        ThrowUtils.throwIf(request.getSessionId() == null, ErrorCode.PARAMS_ERROR, "会话ID为空");
        ThrowUtils.throwIf(request.getSessionType() == null
                        || (request.getSessionType() != SessionTypeConstant.SIGNAL_TYPE
                        && request.getSessionType() != SessionTypeConstant.GROUP_TYPE),
                ErrorCode.PARAMS_ERROR, "会话类型错误");

        RedPacketBody body = request.getBody();
        ThrowUtils.throwIf(body.getTotalAmount() == null,
                ErrorCode.PARAMS_ERROR, "红包金额不能为空");
        ThrowUtils.throwIf(body.getTotalAmount().scale() > 2,
                ErrorCode.PARAMS_ERROR, "红包金额最多保留两位小数");
        ThrowUtils.throwIf(body.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0,
                ErrorCode.PARAMS_ERROR, "红包金额必须大于0");
        ThrowUtils.throwIf(body.getTotalCount() == null || body.getTotalCount() <= 0,
                ErrorCode.PARAMS_ERROR, "红包数量必须大于0");
        ThrowUtils.throwIf(body.getRedPacketType() == null || !RedPacketConstant.isValidType(body.getRedPacketType()),
                ErrorCode.PARAMS_ERROR, "红包类型错误");

        // 验证金额和数量的关系（金额单位是元，每个红包至少1分=0.01元）
        // 计算红包数量×0.01元得到最小总金额，如果用户输入的总金额小于该值，则抛出参数错误异常
        BigDecimal minAmountYuan = new BigDecimal(body.getTotalCount()).multiply(new BigDecimal("0.01"));
        ThrowUtils.throwIf(body.getTotalAmount().compareTo(minAmountYuan) < 0,
                ErrorCode.PARAMS_ERROR, "红包总金额不能少于红包数量（每个红包至少1分）");

        // 验证单个红包金额上限（单个红包不超过 200 元）
        // 计算平均每个红包的金额（总金额÷数量，保留2位小数四舍五入），如果超过200元限制则抛出参数错误异常。
        BigDecimal singleAmount = body.getTotalAmount().divide(
                new BigDecimal(body.getTotalCount()), 2, RoundingMode.HALF_UP);
        ThrowUtils.throwIf(
                singleAmount.compareTo(BigDecimal.valueOf(RedPacketConstant.MAX_SINGLE_AMOUNT_YUAN)) > 0,
                ErrorCode.PARAMS_ERROR,
                "单个红包金额不能超过" + RedPacketConstant.MAX_SINGLE_AMOUNT_YUAN + "元");
    }


    /**
     * 扣减发送者余额
     * <p>
     * ge 条件保证余额充足（防止透支），setSql 原子自减，单条 UPDATE 并发安全。
     */
    private void deductSenderBalance(Long senderId, Long totalAmount) {
        LambdaUpdateWrapper<UserBalance> wrapper = Wrappers.<UserBalance>lambdaUpdate()
                .eq(UserBalance::getUserId, senderId)
                .ge(UserBalance::getBalance, totalAmount)
                .setSql("balance = balance - " + totalAmount);
        int result = userBalanceMapper.update(null, wrapper);
        ThrowUtils.throwIf(result == 0, ErrorCode.OPERATION_ERROR, "余额不足或扣款失败");
    }

    /**
     * 创建红包记录
     *
     * @return 红包 ID
     */
    private long createRedPacket(RedPacketSendRequest request, Long senderId,
                                 Long totalAmount, Integer totalCount, Integer redPacketType) {
        RedPacket redPacket = new RedPacket();
        redPacket.setRedPacketId(SnowflakeUtil.nextId());
        redPacket.setSenderId(senderId);
        redPacket.setSessionId(request.getSessionId());
        redPacket.setSessionType(request.getSessionType());
        redPacket.setRedPacketWrapperText(request.getBody().getRedPacketWrapperText());
        redPacket.setRedPacketType(redPacketType);
        redPacket.setTotalAmount(totalAmount);
        redPacket.setTotalCount(totalCount);
        redPacket.setStatus(RedPacketConstant.STATUS_NOT_COMPLETED);
        redPacket.setCreatedTime(new Date());
        redPacket.setUpdatedTime(new Date());
        int insertResult = redPacketMapper.insert(redPacket);
        ThrowUtils.throwIf(insertResult == 0, ErrorCode.SYSTEM_ERROR, "红包创建失败");
        return redPacket.getRedPacketId();
    }

    /**
     * 记录余额变动日志
     *
     * @param userId    用户 ID
     * @param amount    变动金额（发送红包为负数，领取/退回为正数）
     * @param type      变动类型，见 {@link BalanceLogConstant}
     * @param relatedId 关联业务 ID（如红包 ID）
     */
    private void recordBalanceLog(Long userId, Long amount, Integer type, Long relatedId) {
        BalanceLog balanceLog = new BalanceLog();
        balanceLog.setBalanceLogId(SnowflakeUtil.nextId());
        balanceLog.setUserId(userId);
        balanceLog.setAmount(amount);
        balanceLog.setType(type);
        balanceLog.setRelatedId(relatedId);
        balanceLog.setCreatedTime(new Date());
        balanceLog.setUpdatedTime(new Date());
        balanceLogMapper.insert(balanceLog);
    }

    /**
     * 红包金额预分配并初始化 Redis 缓存
     * <p>
     * 包含：金额分配 → 写入金额池 List → 设置过期时间。
     */
    private void initRedPacketCache(Long redPacketId, Integer redPacketType,
                                    Long totalAmount, Integer totalCount) {
        // 1. 金额预分配
        List<Long> amounts = redPacketType == RedPacketConstant.TYPE_NORMAL
                ? RedPacketAlgorithm.allocateNormalRedPacket(totalAmount, totalCount)
                : RedPacketAlgorithm.allocateRandomRedPacket(totalAmount, totalCount);

        // 2. 写入 Redis 金额池与领取记录占位
        String poolKey = RedisKeyConstant.getPoolKey(redPacketId);
        List<String> amountStrings = amounts.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        stringRedisTemplate.opsForList().rightPushAll(poolKey, amountStrings);
        stringRedisTemplate.expire(poolKey, RedPacketConstant.REDIS_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 发送红包消息到 Kafka（用于消息持久化和推送）
     *
     * @param request     红包发送请求
     * @param redPacketId 红包 ID
     * @return 消息 ID
     */
    private Long sendRedPacketMessage(RedPacketSendRequest request, Long redPacketId) {
        // 1) 构建 MessageRequest
        MessageRequest messageRequest = new MessageRequest();
        messageRequest.setSessionId(request.getSessionId());
        messageRequest.setSenderId(request.getSenderId());
        messageRequest.setType(MessageTypeConstant.RED_PACKET_MESSAGE);
        messageRequest.setSessionType(request.getSessionType());
        messageRequest.setClientMessageId(request.getClientMessageId());

        // - 单聊(SIGNAL_TYPE)：receiverId 必须不为 null
        // - 群聊(GROUP_TYPE)：receiverId 必须为null
        if (Objects.equals(request.getSessionType(), SessionTypeConstant.SIGNAL_TYPE)) {
            messageRequest.setReceiverId(request.getReceiverId());
        } else if (Objects.equals(request.getSessionType(), SessionTypeConstant.GROUP_TYPE)) {
            messageRequest.setReceiverId(null);
        }

        // 2) 构建红包消息体，包含 redPacketId 和 redPacketWrapperText
        MessageBody body = new MessageBody();
        body.setRedPacketId(String.valueOf(redPacketId));
        body.setRedPacketWrapperText(request.getBody().getRedPacketWrapperText());
        messageRequest.setBody(body);

        // 3) 交给统一入口（内部会：补 messageId/createdTime、check、发 store/push 两个 topic）
        Long messageId = sendMessageKafka(messageRequest);

        log.info("红包消息已入队，红包ID: {}, 消息ID: {}", redPacketId, messageId);
        return messageId;
    }
    /**
     * 发送消息到 Kafka
     *
     * @param messageRequest 消息请求
     * @return 消息 ID
     */
    public Long sendMessageKafka(MessageRequest messageRequest) {
        // 转成消息体
        Long messageId = SnowflakeUtil.nextId();
        messageRequest.setMessageId(messageId);
        messageRequest.setCreatedTime(new Date());

        // 校验消息
        checkMessage(messageRequest.getSessionType(), messageRequest.getReceiverId());

        // 消息存储, 存储只存储一次，避免重复消费
        sendKafkaAndWait(KafkaTopicConstant.TOPIC_MESSAGE_STORE,
                JSONUtil.toJsonStr(messageRequest));
        sendKafkaAndWait(KafkaTopicConstant.TOPIC_MESSAGE_PUSH,
                messageRequest.getSessionId().toString(), JSONUtil.toJsonStr(messageRequest));

        return messageId;
    }

    private void sendKafkaAndWait(String topic, String value) {
        sendKafkaAndWait(topic, null, value);
    }

    private void sendKafkaAndWait(String topic, String key, String value) {
        try {
            if (key == null) {
                kafkaTemplate.send(topic, value).get(10, TimeUnit.SECONDS);
            } else {
                kafkaTemplate.send(topic, key, value).get(10, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka 发送被中断", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Kafka 发送失败: " + topic, e);
        }
    }

    private void cleanupRedPacketCache(Long redPacketId) {
        try {
            stringRedisTemplate.delete(RedisKeyConstant.getPoolKey(redPacketId));
            stringRedisTemplate.delete(RedisKeyConstant.getRecordsKey(redPacketId));
            stringRedisTemplate.opsForZSet().remove(
                    RedisKeyConstant.EXPIRE_ZSET, String.valueOf(redPacketId));
        } catch (Exception cleanupException) {
            log.error("清理红包 Redis 缓存失败，红包ID: {}", redPacketId, cleanupException);
        }
    }

    public void checkMessage(Integer sessionType, Long receiverId) {
        ThrowUtils.throwIf(sessionType == SessionTypeConstant.SIGNAL_TYPE && receiverId == null, ErrorCode.SIGNAL_TYPE_ERROR);
        ThrowUtils.throwIf(sessionType == SessionTypeConstant.GROUP_TYPE && receiverId != null, ErrorCode.GROUP_TYPE_ERROR);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRedPacketExpiration(Long redPacketId) {
        // 1. 查询红包信息
        RedPacket redPacket = redPacketMapper.selectById(redPacketId);
        if (redPacket == null) {
            log.warn("红包不存在，红包ID: {}", redPacketId);
            return;
        }

        // 先用条件更新抢占过期处理权，避免重复消息导致重复退款
        LambdaUpdateWrapper<RedPacket> expireWrapper = Wrappers.<RedPacket>lambdaUpdate()
                .eq(RedPacket::getRedPacketId, redPacketId)
                .eq(RedPacket::getStatus, RedPacketConstant.STATUS_NOT_COMPLETED)
                .set(RedPacket::getStatus, RedPacketConstant.STATUS_EXPIRED)
                .set(RedPacket::getUpdatedTime, new Date());
        if (redPacketMapper.update(null, expireWrapper) == 0) {
            log.info("红包状态已变更，跳过过期处理。红包ID: {}, 状态: {}", redPacketId, redPacket.getStatus());
            return;
        }

        // 2. 计算剩余金额
        String poolKey = RedisKeyConstant.getPoolKey(redPacketId);
        List<String> keys = Collections.singletonList(poolKey);
        Long remainAmount = stringRedisTemplate.execute(calculateRemainAmountScript, keys);

        if (remainAmount == null) {
            remainAmount = 0L;
        }

        log.info("红包过期处理开始。红包ID: {}, 剩余金额: {}", redPacketId, remainAmount);

        // 3. 如果有剩余金额，退回给发送者
        if (remainAmount > 0) {
            Long senderId = redPacket.getSenderId();

            // 增加发送者余额
            addBalance(senderId, remainAmount);

            // 记录余额变动日志
            recordBalanceLog(senderId, remainAmount, BalanceLogConstant.TYPE_REFUND, redPacketId);

            log.info("红包过期，退回金额: {}，用户ID: {}", remainAmount, senderId);
        }

        // 4. 清理 Redis 缓存
        String recordsKey = RedisKeyConstant.getRecordsKey(redPacketId);
        stringRedisTemplate.delete(poolKey);
        stringRedisTemplate.delete(recordsKey);
        stringRedisTemplate.opsForZSet().remove(RedisKeyConstant.EXPIRE_ZSET, String.valueOf(redPacketId));

        log.info("红包过期处理完成。红包ID: {}", redPacketId);
    }

    /**
     * 增加用户余额（领取红包或退回）
     * <p>
     * Service 层构建 LambdaUpdateWrapper 调用 mapper.update：
     * - eq: user_id = userId
     * - setSql: balance = balance + amount（原子自增，amount 为 Long 类型，拼接安全）
     * 返回值为受影响行数，0 表示用户余额行不存在或更新失败，抛业务异常。
     */
    private void addBalance(Long userId, Long amount) {
        LambdaUpdateWrapper<UserBalance> wrapper = Wrappers.<UserBalance>lambdaUpdate()
                .eq(UserBalance::getUserId, userId)
                .setSql("balance = balance + " + amount);
        int result = userBalanceMapper.update(null, wrapper);
        ThrowUtils.throwIf(result == 0, ErrorCode.OPERATION_ERROR, "用户余额更新失败");
    }

    @Override
    public ReceiveResultVO receiveRedPacket(RedPacketReceiveRequest request) {
        Long userId = request.getUserId();
        Long redPacketId = request.getRedPacketId();

        String poolKey = RedisKeyConstant.getPoolKey(redPacketId);
        String recordsKey = RedisKeyConstant.getRecordsKey(redPacketId);

        // 执行 Lua 脚本领取红包
        List<String> keys = Arrays.asList(poolKey, recordsKey);

        List result = stringRedisTemplate.execute(
                receiveRedPacketScript,
                keys,
                String.valueOf(userId)
        );

        if (result == null || result.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "红包领取失败");
        }

        Object firstElement = result.get(0);
        int resultCode = ((Number) firstElement).intValue();

        ReceiveResultVO vo = new ReceiveResultVO();

        // 处理不同的返回值
        if (resultCode == ReceiveResultConstant.ALREADY_RECEIVED) {
            // 已领取过
            vo.setStatus(RedPacketConstant.STATUS_ALREADY_RECEIVED);
            vo.setMessage("您已经领取过该红包了");
            // 查询已领取的金额
            Object amountObj = stringRedisTemplate.opsForHash().get(recordsKey, String.valueOf(userId));
            if (amountObj != null) {
                vo.setAmount(convertFenToYuan(Long.parseLong(amountObj.toString())));
            }
            return vo;

        } else if (resultCode == ReceiveResultConstant.EMPTY_POOL) {
            // Redis 数据为空，查询数据库获取真实状态
            RedPacket redPacket = redPacketMapper.selectById(redPacketId);
            if (redPacket == null) {
                vo.setStatus(RedPacketConstant.STATUS_NOT_EXIST);
                vo.setMessage("红包不存在");
                return vo;
            }

            // 返回数据库中的真实状态
            vo.setStatus(redPacket.getStatus());
            if (redPacket.getStatus() == RedPacketConstant.STATUS_COMPLETED) {
                vo.setMessage("红包已被领完");
            } else if (redPacket.getStatus() == RedPacketConstant.STATUS_EXPIRED) {
                vo.setMessage("红包已过期");
            } else {
                // 理论上不应该出现，Redis 为空但数据库状态为未领完
                vo.setMessage("红包暂时无法领取");
            }
            return vo;
        }

        // 领取成功
        long amount = ((Number) firstElement).longValue();
        int completed = result.size() > 1 ? ((Number) result.get(1)).intValue() : ReceiveResultConstant.COMPLETION_FLAG_NOT_COMPLETED;

        vo.setStatus(RedPacketConstant.STATUS_NOT_COMPLETED);
        vo.setAmount(convertFenToYuan(amount));
        vo.setMessage("恭喜您，领取成功");

        // 发送领取记录到 Kafka
        Map<String, Object> receiveEvent = new HashMap<>();
        receiveEvent.put("userId", userId);
        receiveEvent.put("redPacketId", redPacketId);
        receiveEvent.put("receivedAmount", amount);
        receiveEvent.put("receiveTime", System.currentTimeMillis());
        sendKafkaAndWait(KafkaConfig.TOPIC_REDPACKET_RECEIVE, JSONUtil.toJsonStr(receiveEvent));

        // 如果红包已领完，返回状态为已领取完，发送领完事件
        if (ReceiveResultConstant.isCompleted(completed)) {
            vo.setStatus(RedPacketConstant.STATUS_COMPLETED);
            Map<String, Object> completedEvent = new HashMap<>();
            completedEvent.put("redPacketId", redPacketId);
            sendKafkaAndWait(KafkaConfig.TOPIC_REDPACKET_COMPLETED, JSONUtil.toJsonStr(completedEvent));
        }

        log.info("用户 {} 领取红包 {} 成功，金额: {}", userId, redPacketId, amount);

        return vo;
    }

    /**
     * 分转元
     *
     * @param amountFen 金额（分）
     * @return 金额（元）
     */
    private BigDecimal convertFenToYuan(Long amountFen) {
        if (amountFen == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(amountFen)
                .divide(BigDecimal.valueOf(RedPacketConstant.YUAN_TO_FEN_MULTIPLIER), 2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRedPacketReceive(Long userId, Long redPacketId, Long receivedAmount, Long receiveTime) {
        // 幂等性检查：判断是否已经插入过
        QueryWrapper<RedPacketReceive> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("red_packet_id", redPacketId);
        queryWrapper.eq("receiver_id", userId);
        Long count = redPacketReceiveMapper.selectCount(queryWrapper);

        if (count > 0) {
            log.warn("红包领取记录已存在，跳过处理。红包ID: {}, 用户ID: {}", redPacketId, userId);
            return;
        }

        // 1. 插入领取记录
        RedPacketReceive receive = new RedPacketReceive();
        receive.setRedPacketReceiveId(SnowflakeUtil.nextId());
        receive.setRedPacketId(redPacketId);
        receive.setReceiverId(userId);
        receive.setAmount(receivedAmount);
        receive.setReceivedAt(new Date(receiveTime));
        receive.setCreatedTime(new Date());
        receive.setUpdatedTime(new Date());
        try {
            redPacketReceiveMapper.insert(receive);
        } catch (DuplicateKeyException e) {
            // 依赖 (red_packet_id, receiver_id) 唯一索引兜底并发重复消息
            log.warn("红包领取记录已存在，跳过重复消息。红包ID: {}, 用户ID: {}", redPacketId, userId);
            return;
        }

        // 2. 增加用户余额
        addBalance(userId, receivedAmount);

        // 3. 记录余额变动日志
        recordBalanceLog(userId, receivedAmount, BalanceLogConstant.TYPE_RECEIVE, redPacketId);

        log.info("红包领取记录处理完成。红包ID: {}, 用户ID: {}, 金额: {}", redPacketId, userId, receivedAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRedPacketCompleted(Long redPacketId) {
        // 1. 更新红包状态为已领取完
        RedPacket redPacket = redPacketMapper.selectById(redPacketId);
        if (redPacket == null) {
            log.warn("红包不存在，红包ID: {}", redPacketId);
            return;
        }

        redPacket.setStatus(RedPacketConstant.STATUS_COMPLETED);
        redPacket.setUpdatedTime(new Date());
        redPacketMapper.updateById(redPacket);

        // 2. 清理 Redis 缓存
        String poolKey = RedisKeyConstant.getPoolKey(redPacketId);
        String recordsKey = RedisKeyConstant.getRecordsKey(redPacketId);

        stringRedisTemplate.delete(poolKey);
        stringRedisTemplate.delete(recordsKey);
        stringRedisTemplate.opsForZSet().remove(RedisKeyConstant.EXPIRE_ZSET, String.valueOf(redPacketId));

        log.info("红包已领取完，清理完成。红包ID: {}", redPacketId);
    }
}
