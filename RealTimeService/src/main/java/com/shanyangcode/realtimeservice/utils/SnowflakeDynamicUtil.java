package com.shanyangcode.realtimeservice.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeDynamicUtil {

    private static Snowflake SNOWFLAKE;

    @Value("${snowflake.workerId}")
    private long workerId;

    @Value("${snowflake.datacenterId}")
    private long datacenterId;

    @PostConstruct
    public void init() {
        SNOWFLAKE = IdUtil.getSnowflake(workerId, datacenterId);
    }

    public static long nextId() {
        return SNOWFLAKE.nextId();
    }
}
