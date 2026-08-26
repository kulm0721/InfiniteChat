package com.shanyangcode.redpacketservice.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
/**
 * 抢红包 Lua 脚本
 * <p>
 * KEYS[1]: 红包金额池 List Key (使用 {@link com.shanyangcode.redpacketservice.constant.RedisKeyConstant#POOL_TEMPLATE})
 * KEYS[2]: 已领取记录 Hash Key (使用 {@link com.shanyangcode.redpacketservice.constant.RedisKeyConstant#RECORDS_TEMPLATE})
 * ARGV[1]: 当前用户ID (userId)
 * <p>
 * 返回值 (参见 {@link com.shanyangcode.redpacketservice.constant.ReceiveResultConstant}):
 * <ul>
 *   <li>{amount, completed}: 领取成功，amount 为分，completed 表示是否抢完</li>
 *   <li>ALREADY_RECEIVED (-1): 已领取</li>
 *   <li>EMPTY_POOL (-2): Redis 数据为空（需查询数据库获取真实状态）</li>
 * </ul>
 * completed 标识:
 * <ul>
 *   <li>COMPLETION_FLAG_COMPLETED (1): 红包已领完</li>
 *   <li>COMPLETION_FLAG_NOT_COMPLETED (0): 红包未领完</li>
 * </ul>
 */
public class RedisConfig {
    @Bean
    public DefaultRedisScript<List> receiveRedPacketScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        String luaScript = """
                local poolKey   = KEYS[1]
                local recordKey = KEYS[2]
                local userId    = ARGV[1]

                -- 1. 幂等性检查：判断用户是否已领取
                if redis.call('HEXISTS', recordKey, userId) == 1 then
                    return {-1}
                end

                -- 2. 从列表中弹出一个金额 (原子)
                local amount = redis.call('LPOP', poolKey)
                if not amount then
                    return {-2}
                end

                -- 3. 记录该用户已领取
                redis.call('HSET', recordKey, userId, amount)
                redis.call('EXPIRE', recordKey, 25 * 60 * 60)

                -- 4. 是否为最后一个
                local left = redis.call('LLEN', poolKey)
                local completed = 0
                if left == 0 then
                    completed = 1
                end

                -- 5. 返回抢到的金额（分）和领取完标识符
                return {tonumber(amount), completed}
                """;
        script.setScriptText(luaScript);
        script.setResultType(List.class);
        return script;
    }


    /**
     * 扫描过期红包 Lua 脚本
     * <p>
     * KEYS[1]: 过期任务的 ZSet 键名 (使用 {@link com.shanyangcode.redpacketservice.constant.RedisKeyConstant#EXPIRE_ZSET})
     * ARGV[1]: 当前时间戳（毫秒）。如果 <=0 或为 nil，则使用 Redis 的 TIME 命令获取当前时间
     * ARGV[2]: 最大获取条数（正整数，默认 500）
     * <p>
     * 返回值: 已到期的红包ID列表
     */
    @Bean
    public DefaultRedisScript<List> scanExpiredRedPacketsScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        String luaScript = """
                local zsetKey   = KEYS[1]
                local nowArg    = ARGV[1]
                local maxCount  = tonumber(ARGV[2])

                if not maxCount or maxCount <= 0 then
                    maxCount = 500
                end

                -- 如未显式传入"当前时间"，则使用 Redis TIME 决定"现在"的毫秒时间戳
                local nowMs
                if not nowArg or nowArg == '' or tonumber(nowArg) <= 0 then
                    local t = redis.call('TIME')
                    nowMs = (tonumber(t[1]) * 1000) + math.floor(tonumber(t[2]) / 1000)
                else
                    nowMs = tonumber(nowArg)
                end

                -- 1) 取出最多 maxCount 条已到期成员
                local expired = redis.call('ZRANGEBYSCORE', zsetKey, '-inf', nowMs, 'LIMIT', 0, maxCount)

                if #expired == 0 then
                    return {}
                end

                -- 2) 仅返回到期成员，由 Kafka 发送成功后的回调负责删除
                return expired
                """;
        script.setScriptText(luaScript);
        script.setResultType(List.class);
        return script;
    }

    /**
     * 计算红包剩余金额 Lua 脚本
     * <p>
     * KEYS[1]: 红包金额池 List Key (使用 {@link com.shanyangcode.redpacketservice.constant.RedisKeyConstant#POOL_TEMPLATE})
     * <p>
     * 返回值: 剩余总金额 (单位：分)
     */
    @Bean
    public DefaultRedisScript<Long> calculateRemainAmountScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        String luaScript = """
                local lkey = KEYS[1]
                local len = redis.call('LLEN', lkey)
                if len == 0 then
                    return 0
                end

                local sum = 0
                local batch = 200
                local startIdx = 0
                while startIdx < len do
                    local stopIdx = math.min(startIdx + batch - 1, len - 1)
                    local vals = redis.call('LRANGE', lkey, startIdx, stopIdx)
                    for i = 1, #vals do
                        sum = sum + tonumber(vals[i])
                    end
                    startIdx = stopIdx + 1
                end

                return sum
                """;
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        return script;
    }

    /**
     * ShedLock 分布式锁提供者
     */
    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory);
    }

    /**
     * StringRedisTemplate Bean
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
