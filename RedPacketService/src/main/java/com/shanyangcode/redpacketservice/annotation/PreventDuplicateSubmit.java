package com.shanyangcode.redpacketservice.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreventDuplicateSubmit {
    /**
     * 防重复提交的 key 前缀
     */
    String prefix() default "prevent:duplicate:";

    /**
     * 过期时间（秒），默认 3 秒
     */
    int expireSeconds() default 3;
}
