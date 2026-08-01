package com.shanyangcode.gateway.filter;

import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.CommonConstant;
import com.shanyangcode.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class AuthorizeFilter implements GlobalFilter, Ordered {
    private static final List<String> EXCLUDE_PATHS = Arrays.asList("/api/user/login/code",
            "/api/user/register",
            "/api/user/sendCaptcha",
            "/api/user/login/password",
            "/api/user/refresh");
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (EXCLUDE_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String accessToken = request.getHeaders().getFirst("Access-Token");
        String refreshToken = request.getHeaders().getFirst("Refresh-Token");

        try {
            if (accessToken != null && !accessToken.isEmpty()) {
                Claims acClaims = JwtUtil.parse(accessToken);
                if (acClaims != null) {
                    String userId = acClaims.getSubject();
                    String redisAccessToken = stringRedisTemplate.opsForValue().get(CommonConstant.ACCESS_TOKEN_PREFIX + userId);
                    if (accessToken.equals(redisAccessToken)) {
                        return chain.filter(exchange);
                    }
                }
            }

            if (refreshToken != null && !refreshToken.isEmpty()) {
                Claims rfClaims = JwtUtil.parse(refreshToken);
                if (rfClaims != null) {
                    String userId = rfClaims.getSubject();
                    String redisRefreshToken = stringRedisTemplate.opsForValue().get(CommonConstant.REFRESH_TOKEN_PREFIX + userId);

                    if (refreshToken.equals(redisRefreshToken)) {
                        return buildErrorResponse(exchange, ErrorCode.TOKEN_EXPIRED);

                    }
                }
            }
            return buildErrorResponse(exchange,ErrorCode.NOT_LOGIN_ERROR);
        } catch (Exception e) {
            log.error("JWT 校验系统异常", e);
            return buildErrorResponse(exchange,ErrorCode.SYSTEM_ERROR,"登录状态确认失败");
        }
    }

    private Mono<Void> buildErrorResponse(ServerWebExchange exchange, ErrorCode errorCode) {
        return buildErrorResponse(exchange, errorCode, errorCode.getMessage());
    }

    private Mono<Void> buildErrorResponse(ServerWebExchange exchange, ErrorCode errorCode, String message) {
        String jsonResponse=String.format("{\"code\":%d,\"message\":\"%s\"}", errorCode.getCode(), message);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));

        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder(){
        return -1;
    }

}
