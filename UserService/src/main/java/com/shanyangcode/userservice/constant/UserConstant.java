package com.shanyangcode.userservice.constant;


import java.util.concurrent.TimeUnit;

public class UserConstant {

    public static final String EMAIL_SUBJECT = "【验证码】";

    public static final String SEND_EMAIL_SUCCESS = "发送邮件成功";
    
    public static final Integer CAPTCHA_EXPIRE_TIME = 5;

    public static final String PASSWORD_SALT = "goat";

    public static final Integer WORKER_ID = 1;

    public static final Integer DATA_CENTER_ID = 1;

    public static final String EMAIL_REGEX =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$";
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    public static final Integer USER_TIME = 3;

}
