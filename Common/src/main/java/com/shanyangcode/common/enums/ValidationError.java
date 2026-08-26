package com.shanyangcode.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息校验错误枚举
 *
 * 统一管理所有校验相关的错误码和错误消息
 * 合并了原 ValidationErrorCode、ValidationErrorMessage、ValidationRejectReason 的功能
 *
 * 使用方式：
 * - 获取错误码：ValidationError.NOT_FRIEND.getCode() → 91002
 * - 获取消息：ValidationError.NOT_FRIEND.getMessage() → "对方不是您的好友"
 * - 获取标识符：ValidationError.NOT_FRIEND.name() → "NOT_FRIEND"
 * - 根据错误码查找：ValidationError.fromCode(91002) → NOT_FRIEND
 * - 根据名称查找：ValidationError.fromName("NOT_FRIEND") → NOT_FRIEND
 */
public enum ValidationError {

    // ===================== 消息校验错误码（91000-91099）=====================

    /**
     * 消息校验失败（通用）
     */
    VALIDATION_FAILED(91001, "消息校验失败"),

    /**
     * 对方不是您的好友
     */
    NOT_FRIEND(91002, "对方不是您的好友"),

    /**
     * 您已被对方拉黑
     */
    BLOCKED_BY_RECEIVER(91003, "您已被对方拉黑"),

    /**
     * 好友关系已删除
     */
    FRIEND_DELETED(91004, "好友关系已删除"),

    /**
     * 您不是该群成员
     */
    NOT_GROUP_MEMBER(91005, "您不是该群成员"),

    /**
     * 发送者账号异常
     */
    SENDER_DISABLED(91006, "发送者账号异常"),

    /**
     * 接收者账号异常
     */
    RECEIVER_DISABLED(91007, "接收者账号异常"),

    /**
     * 服务暂时不可用
     */
    SERVICE_UNAVAILABLE(91008, "服务暂时不可用，请稍后重试"),

    // ===================== 红包校验错误码（92000-92099）=====================

    /**
     * 红包数量不能超过群成员数量
     */
    REDPACKET_COUNT_EXCEED_MEMBERS(92001, "红包数量不能超过群成员数量"),

    /**
     * 单聊只能发送普通红包
     */
    REDPACKET_SINGLE_ONLY_NORMAL(92002, "单聊只能发送普通红包");

    /**
     * 错误码
     */
    private final int code;

    /**
     * 用户友好的错误消息
     */
    private final String message;

    /**
     * 错误码到枚举的映射缓存，避免每次 fromCode 都遍历
     */
    private static final Map<Integer, ValidationError> CODE_MAP;


    /**
     * 名称到枚举的映射缓存
     */
    private static final Map<String, ValidationError> NAME_MAP;

    static {
        CODE_MAP = new HashMap<>();
        NAME_MAP = new HashMap<>();
        for (ValidationError e : values()) {
            CODE_MAP.put(e.code, e);
            NAME_MAP.put(e.name(), e);
        }
    }

    ValidationError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据错误码查找枚举
     *
     * @param code 错误码
     * @return 对应的枚举，找不到时返回 VALIDATION_FAILED
     */
    public static ValidationError fromCode(int code) {
        return CODE_MAP.getOrDefault(code, VALIDATION_FAILED);
    }

    /**
     * 根据名称查找枚举（用于替代 ValidationRejectReason）
     *
     * @param name 枚举名称（如 "NOT_FRIEND"）
     * @return 对应的枚举，找不到时返回 VALIDATION_FAILED
     */
    public static ValidationError fromName(String name) {
        if (name == null) {
            return VALIDATION_FAILED;
        }
        return NAME_MAP.getOrDefault(name, VALIDATION_FAILED);
    }
}
