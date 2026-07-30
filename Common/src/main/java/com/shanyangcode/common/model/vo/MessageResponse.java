package com.shanyangcode.common.model.vo;

import lombok.Data;

@Data
public class MessageResponse {

    private Long sessionId;


    private Long senderId;


    private Integer type;


    private Integer sessionType;



    private String createdTime;


    private Long messageId;


    private String clientMessageId;


    private String nickname;


    private String avatar;


    private Integer role;
}
