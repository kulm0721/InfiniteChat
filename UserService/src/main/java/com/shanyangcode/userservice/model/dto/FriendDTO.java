package com.shanyangcode.userservice.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 好友DTO
 *
 * 用于返回好友列表数据
 */
@Data
public class FriendDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 好友用户ID
     */
    private String userId;

    /**
     * 好友昵称
     */
    private String nickname;

    /**
     * 好友头像URL
     */
    private String avatar;

    /**
     * 好友状态 (0:好友 1:拉黑 2:删除)
     */
    private Integer status;

    /**
     * 个性签名
     */
    private String signature;

    /**
     * 会话ID
     */
    private String sessionId;
}
