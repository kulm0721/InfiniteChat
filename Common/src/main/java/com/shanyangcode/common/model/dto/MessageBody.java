package com.shanyangcode.common.model.dto;

import lombok.Data;

@Data
public class MessageBody {
    private String content;

    private Long replyId;
}
