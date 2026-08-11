package com.shanyangcode.offlinedataservice.model.dto;

import lombok.Data;

@Data
public class SessionSummaryRequest {

    private Integer hours;


    private Long sessionId;


    private Long senderId;
}
