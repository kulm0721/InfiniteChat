package com.shanyangcode.common.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class MessageRequest {
    private Long sessionId;


    private Long receiverId;


    private Long senderId;


    private Integer type;


    private Integer sessionType;


    private Date createdTime;


    private Long messageId;


    private String clientMessageId;
}
