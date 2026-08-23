package com.shanyangcode.userservice.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class GroupMemberDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String nickname;
    private String avatar;
}
