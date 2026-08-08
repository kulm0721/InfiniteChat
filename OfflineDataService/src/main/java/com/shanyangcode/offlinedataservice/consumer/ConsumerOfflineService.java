package com.shanyangcode.offlinedataservice.consumer;

import cn.hutool.json.JSONUtil;
import com.shanyangcode.common.model.dto.MessageRequest;
import com.shanyangcode.offlinedataservice.service.MessageService;
import jakarta.annotation.Resource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ConsumerOfflineService {

    @Resource
    private MessageService messageService;

    @KafkaListener(topics = "store-topic", groupId = "infinite-chat-store-group")
    public void consume(String message) {
        System.out.println("Consumed message store :" + message);
        MessageRequest messageRequest = JSONUtil.toBean(message, MessageRequest.class);
        messageService.saveMessageToMySQL(messageRequest);
        System.out.println(messageRequest);
    }
}
