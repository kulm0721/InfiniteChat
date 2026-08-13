package com.shanyangcode.userservice.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class FriendRequestExpirationEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 好友申请ID
     */
    private Long applyFriendId;

    /**
     * 过期时间戳（毫秒）
     */
    private Long expireTime;
}
