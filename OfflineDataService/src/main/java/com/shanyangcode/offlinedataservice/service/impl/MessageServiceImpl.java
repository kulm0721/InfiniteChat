package com.shanyangcode.offlinedataservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.common.model.dto.MessageRequest;
import com.shanyangcode.offlinedataservice.mapper.MessageMapper;
import com.shanyangcode.offlinedataservice.model.entity.Message;
import com.shanyangcode.offlinedataservice.service.MessageService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Override
    public void saveMessageToMySQL(MessageRequest messageRequest) {
        Message message = new Message();
        BeanUtils.copyProperties(messageRequest, message);
        message.setContent(messageRequest.getBody().getContent());
        message.setReplyId(messageRequest.getBody().getReplyId());
        ThrowUtils.throwIf(!this.save(message), ErrorCode.SYSTEM_ERROR);

    }
}
