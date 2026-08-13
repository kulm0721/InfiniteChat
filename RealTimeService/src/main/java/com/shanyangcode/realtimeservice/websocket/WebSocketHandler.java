package com.shanyangcode.realtimeservice.websocket;


import cn.hutool.json.JSONUtil;
import com.shanyangcode.common.constant.CommonConstant;
import com.shanyangcode.common.constant.KafkaTopicConstant;
import com.shanyangcode.common.model.dto.MessageRequest;
import com.shanyangcode.realtimeservice.constant.WebSocketConstant;
import com.shanyangcode.common.utils.SnowflakeUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Date;

@Slf4j
@AllArgsConstructor
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private final StringRedisTemplate stringRedisTemplate;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) {
        Channel channel = channelHandlerContext.channel();
        String msg = textWebSocketFrame.text();

        try {
            if (WebSocketConstant.HEARTBEAT_PING.equals(msg)) {
                if (channel.isActive()) {
                    log.debug("Received heartbeat ping from {}", channel.id());
                    channel.writeAndFlush(new TextWebSocketFrame(WebSocketConstant.HEARTBEAT_PONG));
                }
            } else {
                if (channel.isActive()) {
                    sendMessageKafka(msg, channel);
                } else {
                    log.warn("Channel {} inactive,skip message: {}", channel.id(), msg);
                }
            }
        } catch (Exception e) {
            log.error("Error handling message from {} : {}", channel.id(), msg, e);
            clearChannel(channel);
            channelHandlerContext.close();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        //处理心跳
        if (evt instanceof IdleStateEvent event) {
            switch (event.state()) {
                case READER_IDLE:
                    log.error("读空闲超时");
                    clearChannel(ctx.channel());
                    ctx.close();
                    break;
                case WRITER_IDLE:
                    log.error("写空闲超时");
                    break;
                case ALL_IDLE:
                    log.error("读写空闲超时");
                    break;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception caught in channel pipeline", cause);
        ctx.close();
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        System.out.println("channel active");
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        clearChannel(ctx.channel());
        super.channelInactive(ctx);
        System.out.println("channel inActive");
    }
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        System.out.println("handler added");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        clearChannel(ctx.channel());
        super.handlerRemoved(ctx);
        System.out.println("handler removed");
    }

    public void sendMessageKafka(String message, Channel channel) {
        MessageRequest messageRequest = JSONUtil.toBean(message, MessageRequest.class);
        messageRequest.setMessageId(SnowflakeUtil.nextId());
        messageRequest.setCreatedTime(new Date());

        //消息存储
        kafkaTemplate.send(KafkaTopicConstant.TOPIC_MESSAGE_STORE, JSONUtil.toJsonStr(messageRequest)).whenComplete((success, failure) -> {
            if (failure != null) {
                log.error("生产者生产失败 ：{}", failure.getMessage());
            } else {
                log.info("生产者生产成功 offset :" + success.getRecordMetadata().offset());
            }
        });

        // 消息推送消息
        kafkaTemplate.send(KafkaTopicConstant.TOPIC_MESSAGE_PUSH, messageRequest.getSessionId().toString(), JSONUtil.toJsonStr(messageRequest)).whenComplete((success, failure) -> {
            if (failure != null) {
                // 生产者生产失败
                log.error("生产者生产失败: " + failure.getMessage());
                // 记录日志、告警、补偿等
            } else {
                // 生产者生产成功
                log.info("生产者生产成功消息推送，offset:{}", success.getRecordMetadata().offset());
            }
        });
    }

    public void clearChannel(Channel channel) {
        if (channel == null) {
            return;
        }
        log.info("clearChannel: {}", channel.id());

        String userId = ChannelManager.getUserIdByChannel(channel);
        try {
            if (userId != null) {
                ChannelManager.removeUserChannel(userId);
                saveOfflineTime(userId);
            }
            ChannelManager.removeChannelUser(channel);
        } catch (Exception e) {
            log.error("clearChannel failed for channel: {}, userId: {}", channel.id(), userId, e);
        } finally {
            if (channel.isActive()) {
                channel.close();
            }
        }
    }

    private void saveOfflineTime(String userId) {
        String key=CommonConstant.OFFLINE_KEY_REDIS+userId;
        String timestamp=String.valueOf(System.currentTimeMillis());
        stringRedisTemplate.opsForValue().set(key, timestamp);
        log.debug("记录用户离线时间: userId={}, timestamp={}", userId, timestamp);
    }
}
