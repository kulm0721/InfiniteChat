package com.shanyangcode.offlinedataservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.common.model.dto.MessageRequest;
import com.shanyangcode.offlinedataservice.model.entity.Message;

public interface MessageService extends IService<Message> {
    void saveMessageToMySQL(MessageRequest messageRequest);
}
