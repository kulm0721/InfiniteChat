package com.shanyangcode.userservice.common;

/**
 * 自定义错误码
 *
 * 注意：91xxx/92xxx 系列错误消息引用自 ValidationError 枚举，遵循单一数据源原则
 */
public enum ErrorCode {
    // ============ 通用错误码（40xxx）============
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    TOKEN_INVALID(40101, "无效的身份认证"),
    TOKEN_EXPIRED(40103, "Access Token 过期"),

    // ============ 系统错误码（50xxx）============
    SYSTEM_ERROR(50000, "系统内部异常"),

    // ============ 用户相关错误码（70xxx）============
    PHONE_EMAIL_ERROR(70000, "手机号/邮箱格式错误"),
    USER_ALREADY_EXISTS(70001, "用户已存在"),
    USER_NOT_EXISTS(70002, "用户不存在"),
    REGISTER_ERROR(70003, "注册失败"),
    LOGIN_ERROR_CODE(70004, "验证码错误/失效"),
    LOGIN_ERROR(70005, "登录失败, 用户名或密码错误"),
    LOGIN_PASSWORD_ERROR(70006, "密码不一致"),
    LOGIN_SEND_CODE_ERROR(70007, "验证码已发送，请稍后重试");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}