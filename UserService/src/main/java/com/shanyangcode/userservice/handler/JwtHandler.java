package com.shanyangcode.userservice.handler;

import com.shanyangcode.userservice.common.ErrorCode;
import com.shanyangcode.userservice.constant.UserConstant;
import com.shanyangcode.userservice.exception.BusinessException;
import com.shanyangcode.userservice.utils.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class JwtHandler implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String accessToken = request.getHeader("Access-Token");
        String refreshToken = request.getHeader("Refresh-Token");

        try {
            if (accessToken != null) {
                Claims acParse = JwtUtil.parse(accessToken);
                if (acParse != null) {
                    String userId = acParse.getSubject();
                    String redisAccessToken = stringRedisTemplate.opsForValue().get(UserConstant.ACCESS_TOKEN_PREFIX + userId);
                    if (accessToken.equals(redisAccessToken)) {
                        return true;
                    }
                }
            }

            if (refreshToken != null) {
                Claims rfParse = JwtUtil.parse(refreshToken);
                if (rfParse != null) {
                    String userId = rfParse.getSubject();
                    String redisRefreshToken = stringRedisTemplate.opsForValue().get(UserConstant.REFRESH_TOKEN_PREFIX + userId);
                    if (refreshToken.equals(redisRefreshToken)) {
                        throw new BusinessException(ErrorCode.TOKEN_EXPIRED); // 触发前端自动刷新
                    }
                }
            }

        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("JWT 校验系统异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录状态确认失败");
        }

        throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
    }
}