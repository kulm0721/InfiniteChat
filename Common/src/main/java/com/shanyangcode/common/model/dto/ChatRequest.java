package com.shanyangcode.common.model.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private Long sessionId;

    private Long userId;

    private String prompt;
}
