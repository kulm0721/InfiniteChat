package com.shanyangcode.offlinedataservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.common.model.dto.MessageRequest;
import com.shanyangcode.common.model.vo.MessageResponse;
import com.shanyangcode.offlinedataservice.model.dto.HistoryMessageRequest;
import com.shanyangcode.offlinedataservice.model.dto.OfflineMessageRequest;
import com.shanyangcode.offlinedataservice.model.dto.SessionSummaryRequest;
import com.shanyangcode.offlinedataservice.model.entity.Message;

import java.util.List;
import java.util.Map;

public interface MessageService extends IService<Message> {

    void saveMessageToMySQL(MessageRequest messageRequest);

    /**
     * 获取离线消息
     *
     * @return Map<sessionId, 消息列表>
     */
    Map<Long, List<MessageResponse>> getOfflineMessages(OfflineMessageRequest request);


    /**
     * 获取历史消息（冷热融合查询）
     */
    List<MessageResponse> getHistoryMessages(HistoryMessageRequest request);

    String getSummary(SessionSummaryRequest sessionSummaryRequest);
}
