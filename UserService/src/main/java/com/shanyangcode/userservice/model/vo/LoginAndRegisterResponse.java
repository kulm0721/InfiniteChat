package com.shanyangcode.userservice.model.vo;

import lombok.Data;

@Data
public class LoginAndRegisterResponse {

    private Long userId;

    private String email;

    private String nickname;

    private String avatar;

    private Integer gender;

    private String description;

    private String accessToken;

    private String refreshToken;
}