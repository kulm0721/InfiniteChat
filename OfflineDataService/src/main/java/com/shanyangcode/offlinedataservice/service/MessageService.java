package com.shanyangcode.offlinedataservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.common.model.dto.MessageRequest;
import com.shanyangcode.common.model.vo.MessageResponse;
import com.shanyangcode.offlinedataservice.model.dto.HistoryMessageRequest;
import com.shanyangcode.offlinedataservice.model.dto.OfflineMessageRequest;
import com.shanyangcode.offlinedataservice.model.entity.Message;

import java.util.List;
import java.util.Map;

public interface MessageService extends IService<Message> {
    void saveMessageToMySQL(MessageRequest messageRequest);
    Map<Long, List<MessageResponse>> getOfflineMessages(OfflineMessageRequest request);


    /**
     * 获取历史消息（冷热融合查询）
     */
    List<MessageResponse> getHistoryMessages(HistoryMessageRequest request);
}
