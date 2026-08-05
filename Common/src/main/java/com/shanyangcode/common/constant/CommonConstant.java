package com.shanyangcode.common.constant;

import java.util.concurrent.TimeUnit;

public class CommonConstant {
    public static final String TOKEN_SECRET_KEY = "shanyangcodebackendinitprojectshanyangcodebackendinitproject";

    public static final Integer ACCESS_TOKEN_EXPIRE_TIME = 30;

    public static final Integer REFRESH_TOKEN_EXPIRE_TIME = 7;

    public static final String ACCESS_TOKEN_PREFIX = "access:token:";

    public static final String REFRESH_TOKEN_PREFIX = "refresh:token:";

    public static final TimeUnit ACCESS_TOKEN_UNIT = TimeUnit.MINUTES; // 分钟

    public static final TimeUnit REFRESH_TOKEN_UNIT = TimeUnit.DAYS; // 天

    public static final String KAFKA_MESSAGE_TOPIC_STORE = "store-topic";

    public static final String KAFKA_MESSAGE_TOPIC_PUSH = "message-topic";

    public static final String REDIS_NETTY_URI = "nettyUri";

    public static final String DISCOVERY_CLIENT_NAME = "RealTimeService";

    public static final String NETTY_SERVICE_URI = "/ws/netty";

    public static final String BUCKET_NAME = "infinitechat";

    public static final Integer PICTURE_EXPIRE_TIME = 3000;
}
