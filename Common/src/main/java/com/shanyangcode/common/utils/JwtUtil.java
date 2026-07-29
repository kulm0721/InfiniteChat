package com.shanyangcode.common.utils;

import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;


import com.shanyangcode.common.constant.CommonConstant;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * JWT工具类（优化点：完善异常日志、统一过期时间配置）
 */
@Slf4j
public final class JwtUtil {

    /**
     * 生成JWT（支持自定义过期时间）
     */
    public static String generate(String userId, long timeout, TimeUnit unit) {
        Date now = new Date();
        //设置过期时间
        Date expiration = new Date(now.getTime() + unit.toMillis(timeout));
        System.out.println(expiration);
        return Jwts.builder()
                .setSubject(userId) // 存储用户唯一标识（如手机号/用户ID）
                .setIssuedAt(now) // 签发时间
                .setExpiration(expiration) // JWT自身过期时间长一点
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) // 签名算法
                .compact();
    }

    /**
     * 获取签名密钥（从常量类读取，避免硬编码）
     */
    public static Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(CommonConstant.TOKEN_SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 解析JWT（区分不同异常类型，便于排查）
     */
    public static Claims parse(String token) {
        if (StringUtils.isEmpty(token)) {
            log.warn("解析JWT失败：token 为空");
            return null;
        }
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT 已过期：{}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.error("JWT 签名无效或格式错误：{}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("JWT 解析发生未知错误：", e);
            return null;
        }
    }


}