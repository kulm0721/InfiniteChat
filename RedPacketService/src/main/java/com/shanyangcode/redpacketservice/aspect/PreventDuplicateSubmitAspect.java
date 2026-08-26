package com.shanyangcode.redpacketservice.aspect;

import cn.hutool.json.JSONUtil;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.exception.BusinessException;
import com.shanyangcode.redpacketservice.annotation.PreventDuplicateSubmit;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交切面
 */
@Aspect
@Component
@Slf4j
public class PreventDuplicateSubmitAspect {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(com.shanyangcode.redpacketservice.annotation.PreventDuplicateSubmit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1.拿到被拦截的方法和它身上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        PreventDuplicateSubmit annotation = method.getAnnotation(PreventDuplicateSubmit.class);

        // 2. 生成请求"指纹"
        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        String argsJson = JSONUtil.toJsonStr(args);
        // 构建 Redis key
        String key = annotation.prefix() + method.getName() + ":" + argsJson.hashCode();

        // 3. setIfAbsent 原子占坑
        // 尝试设置 key，如果已存在则返回 false
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", annotation.expireSeconds(), TimeUnit.SECONDS);

        // 4. 占坑失败 ⇒ 拒绝请求
        if (Boolean.FALSE.equals(success)) {
            log.warn("检测到重复提交，方法：{}，参数：{}", method.getName(), argsJson);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");
        }

        // 5. 放行业务 + 失败善后
        try {
            // 执行业务逻辑
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // 如果业务逻辑执行失败，删除 key 允许重试
            stringRedisTemplate.delete(key);
            throw throwable;
        }
    }

}
