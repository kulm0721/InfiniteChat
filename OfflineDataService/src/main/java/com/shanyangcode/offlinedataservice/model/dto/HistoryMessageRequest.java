package com.shanyangcode.offlinedataservice.model.dto;

import lombok.Data;

@Data
public class HistoryMessageRequest {
    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 查询该时间戳之前的消息（毫秒）
     */
    private Long beforeTime;

    /**
     * 每页数量，默认20
     */
    private Integer limit = 20;
}
