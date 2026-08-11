package com.shanyangcode.offlinedataservice.controller;

import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.common.ResultUtils;
import com.shanyangcode.common.model.vo.MessageResponse;
import com.shanyangcode.offlinedataservice.model.dto.HistoryMessageRequest;
import com.shanyangcode.offlinedataservice.model.dto.OfflineMessageRequest;
import com.shanyangcode.offlinedataservice.model.dto.SessionSummaryRequest;
import com.shanyangcode.offlinedataservice.service.MessageService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
public class MessageController {
    @Resource
    private MessageService messageService;

    /**
     * 获取离线消息（用户上线后调用）
     *
     * @return Map<sessionId, List<消息>>
     */
    @PostMapping("/offline")
    public BaseResponse<Map<Long, List<MessageResponse>>> getOfflineMessages(@RequestBody OfflineMessageRequest request) {
        return ResultUtils.success(messageService.getOfflineMessages(request));
    }

    /**
     * 获取历史消息（往上翻页）
     */
    @PostMapping("/history")
    public BaseResponse<List<MessageResponse>> getHistoryMessages(@RequestBody HistoryMessageRequest request) {
        return ResultUtils.success(messageService.getHistoryMessages(request));
    }

    @PostMapping("/summary")
    public BaseResponse<String> chatSummary(@RequestBody SessionSummaryRequest sessionSummaryRequest) {
        return ResultUtils.success(messageService.getSummary(sessionSummaryRequest));
    }
}
