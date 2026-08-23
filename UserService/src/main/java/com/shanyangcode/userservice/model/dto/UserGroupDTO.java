package com.shanyangcode.userservice.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserGroupDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String name;
    private String avatar;
    private String creatorId;
    private Integer role;
    private Integer memberCount;
    private String createdTime;
}
