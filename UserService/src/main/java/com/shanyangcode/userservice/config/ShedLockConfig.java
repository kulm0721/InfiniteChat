package com.shanyangcode.userservice.config;


import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * ShedLock 分布式调度锁配置
 * <p>
 * 功能说明：
 * - 激活 @SchedulerLock 注解，使集群环境下同一调度任务只在一个实例执行
 * - 使用 Redis 作为锁存储后端，复用现有 Redis 连接
 * <p>
 * 注意：@EnableScheduling 已在 UserServiceApplication 主类上开启
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class ShedLockConfig {
    /** Redis 中 lock key 的前缀，按服务隔离 */
    private static final String LOCK_KEY_PREFIX = "UserService";

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, LOCK_KEY_PREFIX);
    }

}
