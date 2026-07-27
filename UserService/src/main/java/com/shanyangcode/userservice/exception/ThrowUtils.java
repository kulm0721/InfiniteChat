package com.shanyangcode.userservice.exception;

import com.shanyangcode.userservice.common.ErrorCode;

/**
 * 抛异常工具类
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     *
     * @param condition 判断条件，若为 {@code true} 则抛出异常
     * @param runtimeException 要抛出的运行时异常实例
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition 判断条件，若为 {@code true} 则抛出异常
     * @param errorCode 业务错误码，用于构造 {@link BusinessException}
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition 判断条件，若为 {@code true} 则抛出异常
     * @param errorCode 业务错误码
     * @param message 自定义异常消息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}

