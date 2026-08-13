package com.shanyangcode.realtimeservice.consumer;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.shanyangcode.common.constant.CommonConstant;
import com.shanyangcode.common.constant.KafkaTopicConstant;
import com.shanyangcode.common.model.dto.MessageBody;
import com.shanyangcode.common.model.dto.MessageRequest;
import com.shanyangcode.common.model.vo.MessageResponse;
import com.shanyangcode.common.utils.FormatDateUtil;
import com.shanyangcode.realtimeservice.client.AiServiceClient;
import com.shanyangcode.realtimeservice.client.UserServiceClient;
import com.shanyangcode.common.constant.SessionTypeConstant;
import com.shanyangcode.common.model.dto.ChatRequest;
import com.shanyangcode.common.utils.SnowflakeUtil;
import com.shanyangcode.realtimeservice.websocket.ChannelManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class ConsumerMessageService {

    @Resource
    private UserServiceClient userServiceClient;

    @Resource
    private AiServiceClient aiServiceClient;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * AI 服务调用失败时的兜底回复
     */
    private static final String AI_ERROR_REPLY = "抱歉，AI 暂时不在线，请稍后再试～";

    @KafkaListener(topics = "message-topic", groupId = "infinite-chat-push-group-0")
    public void consume(String message) {
        log.info("收到消息 ： {}", message);
        MessageRequest messageRequest = JSONUtil.toBean(message, MessageRequest.class);
        log.info("收到信息 ： {}", messageRequest);
        if (messageRequest.getSessionType() == SessionTypeConstant.SIGNAL_TYPE) {
            signalMessage(messageRequest);
        } else if (messageRequest.getSessionType() == SessionTypeConstant.GROUP_TYPE) {
            groupMessage(messageRequest);
        }else if(messageRequest.getSessionType()==SessionTypeConstant.ROBOT_TYPE) {
            aiSignalMessage(messageRequest);
        }
    }

    public void signalMessage(MessageRequest messageRequest) {
        MessageResponse messageResponse = createMessageResponse(messageRequest);
        pushMessageToUser(messageResponse,messageRequest.getSenderId());
        pushMessageToUser(messageResponse,messageRequest.getReceiverId());
    }

    public void groupMessage(MessageRequest messageRequest) {
        List<Long> receiveUserIds;
        try {
            receiveUserIds = userServiceClient.getUserIdBySessionId(messageRequest.getSessionId());
        } catch (Exception e) {
            log.error("获取会话成员失败, sessionId={}", messageRequest.getSessionId(), e);
            return;
        }
        if (receiveUserIds == null || receiveUserIds.isEmpty()) {
            log.info("会话无成员, sessionId={}", messageRequest.getSessionId());
            return;
        }
        MessageResponse messageResponse = createMessageResponse(messageRequest);
        for (Long receiveUserId : receiveUserIds) {
            pushMessageToUser(messageResponse, receiveUserId);
        }
    }

    public void aiSignalMessage(MessageRequest messageRequest) {
        MessageResponse messageResponse = createMessageResponse(messageRequest);
        messageResponse.setMessageId(SnowflakeUtil.nextId());

        //获取Ai回复
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setPrompt(messageRequest.getBody().getContent());
        chatRequest.setSessionId(messageRequest.getSessionId());
        chatRequest.setUserId(messageRequest.getSenderId());
        String chat;
        try {
            chat = aiServiceClient.chat(chatRequest);
        } catch (Exception e) {
            //AI服务调用失败，推送兜底回复，避免异常抛出后消息被重复消费拖死消费链路
            log.error("调用AiService失败, sessionId={}, userId={}", messageRequest.getSessionId(), messageRequest.getSenderId(), e);
            chat = AI_ERROR_REPLY;
        }

        //AI 回复使用独立时间戳，避免与用户消息同分导致历史消息乱序
        messageResponse.setCreatedTime(FormatDateUtil.formatDate(new Date()));

        MessageBody messageBody = messageResponse.getBody();
        messageBody.setContent(chat);
        messageResponse.setSenderId(CommonConstant.AI_ID);

        pushMessageToUser(messageResponse, chatRequest.getUserId());
        BeanUtil.copyProperties(messageResponse, messageRequest);

        kafkaTemplate.send(KafkaTopicConstant.TOPIC_MESSAGE_STORE, JSONUtil.toJsonStr(messageRequest)).whenComplete((success, failure) -> {
            if (failure != null) {
                log.error("AI回复写入store-topic失败, messageId={}", messageRequest.getMessageId(), failure);
            } else {
                log.info("AI回复写入store-topic成功, messageId={}, offset={}", messageRequest.getMessageId(), success.getRecordMetadata().offset());
            }
        });
    }
    public MessageResponse createMessageResponse(MessageRequest messageRequest) {
        MessageResponse messageResponse = new MessageResponse();
        BeanUtil.copyProperties(messageRequest, messageResponse);
        messageResponse.setCreatedTime(FormatDateUtil.formatDate(messageRequest.getCreatedTime()));
        return messageResponse;
    }

    public void pushMessageToUser(MessageResponse messageResponse, Long receiverId) {
        Channel channel = ChannelManager.getChannelByUserId(receiverId.toString());
        if (channel != null) {
            TextWebSocketFrame frame = new TextWebSocketFrame(JSONUtil.toJsonStr(messageResponse));
            channel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("消息发送成功 : {}", messageResponse);
                } else {
                    log.error("消息发送失败 ： {}", future.cause() != null ? future.cause().getMessage() : "未知错误");
                }
            });
        } else {
            log.info("channel不存在");
        }
    }
}
