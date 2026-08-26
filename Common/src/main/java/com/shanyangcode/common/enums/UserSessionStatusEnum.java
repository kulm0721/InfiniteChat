package com.shanyangcode.common.enums;

/**
 * 用户会话状态枚举
 * <p>
 * 用于定义 user_session 表中的 status 字段值
 * 统一所有服务对用户会话状态的判断
 * <p>
 * 数据库存储值说明：
 * - 0: 正常状态
 * - 1: 已删除
 */
public enum UserSessionStatusEnum {

    /**
     * 状态：正常
     */
    NORMAL(0, "正常"),

    /**
     * 状态：已删除
     */
    DELETED(1, "已删除");

    private final int code;
    private final String description;

    UserSessionStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

}
