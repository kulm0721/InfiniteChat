package com.shanyangcode.common.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花 ID 生成工具类
 * <p>
 * workerId / datacenterId 从各服务的 application.yml 读取（默认 1），
 * 不同服务/实例应配置不同的 workerId 以避免生成重复 ID。
 */
@Component
public class SnowflakeUtil {

    private static Snowflake SNOWFLAKE;

    public SnowflakeUtil(@Value("${snowflake.workerId:1}") long workerId,
                         @Value("${snowflake.datacenterId:1}") long datacenterId) {
        SNOWFLAKE = IdUtil.getSnowflake(workerId, datacenterId);
    }

    /**
     * 生成雪花 ID
     *
     * @return 雪花 ID
     */
    public static long nextId() {
        return SNOWFLAKE.nextId();
    }
}
