package com.shanyangcode.realtimeservice.websocket;

import com.shanyangcode.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

@RequiredArgsConstructor
public class WebSocketAuthHeader extends ChannelInboundHandlerAdapter {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest request) {
            String authHeader = request.headers().get("Authorization");
            if (authHeader == null || authHeader.isEmpty()) {
                ctx.close();
                return;
            }
            try {
                Claims claims = JwtUtil.parse(authHeader);
                if (claims == null) {
                    ctx.close();
                    return;
                }
                String userId = claims.getSubject();
                if (userId == null || userId.isEmpty()) {
                    ctx.close();
                    return;
                }

                ChannelManager.addUserChannel(userId, ctx.channel());
                ChannelManager.addChannelUser(userId, ctx.channel());
                ctx.fireChannelRead(msg);
            } catch (Exception e) {
                ctx.close();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}

