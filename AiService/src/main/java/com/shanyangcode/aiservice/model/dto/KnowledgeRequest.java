package com.shanyangcode.aiservice.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class KnowledgeRequest implements Serializable {
    private String question;

    private String answer;

    private String sourceName;
}
