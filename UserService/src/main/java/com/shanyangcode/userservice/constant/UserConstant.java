package com.shanyangcode.userservice.constant;


import java.util.concurrent.TimeUnit;

public class UserConstant {

    public static final String EMAIL_SUBJECT = "【验证码】";

    public static final String SEND_EMAIL_SUCCESS = "发送邮件成功";
    
    public static final Integer CAPTCHA_EXPIRE_TIME = 5;

    public static final String PASSWORD_SALT = "goat";

    public static final Integer WORKER_ID = 1;

    public static final Integer DATA_CENTER_ID = 1;

    public static final String TOKEN_SECRET_KEY = "shanyangcodebackendinitprojectshanyangcodebackendinitproject";

    public static final Integer ACCESS_TOKEN_EXPIRE_TIME =  30;

    public static final Integer REFRESH_TOKEN_EXPIRE_TIME =  7;

    public static final String ACCESS_TOKEN_PREFIX = "access:token:";

    public static final String REFRESH_TOKEN_PREFIX = "refresh:token:";

    public static final TimeUnit ACCESS_TOKEN_UNIT = TimeUnit.MINUTES; // 分钟

    public static final TimeUnit REFRESH_TOKEN_UNIT = TimeUnit.DAYS; // 天
}
