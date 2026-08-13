package com.shanyangcode.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

/**
 * Redis配置类
 * <p>
 * 功能说明：
 * - 配置Redis Template
 * - 配置Lua脚本Bean
 */
@Configuration
public class RedisConfig {

    /**
     * StringRedisTemplate Bean
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }


    /**
     * 好友申请过期任务扫描Lua脚本
     * <p>
     * 功能：原子性地从Redis ZSET中取出已到期的好友申请ID并删除
     * <p>
     * 参数：
     * - KEYS[1]: ZSET键名（friend-request-expire-zset）
     * - ARGV[1]: 当前时间戳（毫秒），如果<=0则使用Redis TIME命令获取
     * - ARGV[2]: 最大获取条数
     * <p>
     * 返回：已到期的好友申请ID列表
     */
    @Bean
    public DefaultRedisScript<List> scanExpiredFriendRequestsScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        String luaScript = """
                local zsetkey = KEYS[1]
                local nowArg = ARGV[1]
                local maxCount = tonumber(ARGV[2])
                if not maxCount or maxCount <= 0 then maxCount = 500 end
                local nowMs
                if (not nowArg) or nowArg == '' or tonumber(nowArg) <= 0 then
                    local t = redis.call('TIME')
                    nowMs = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
                else
                    nowMs = tonumber(nowArg)
                end
                local expired = redis.call('ZRANGEBYSCORE', zsetKey, '-inf', nowMs, 'LIMIT', 0, maxCount)
                if #expired == 0 then return {} end
                for i = 1, #expired do redis.call('ZREM', zsetKey, expired[i]) end
                return expired
                """;
        script.setScriptText(luaScript);
        script.setResultType(List.class);
        return script;
    }

}
