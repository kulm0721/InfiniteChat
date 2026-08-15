package com.shanyangcode.userservice.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改好友申请响应DTO
 * <p>
 * 用于返回通过好友申请后的会话信息
 */
@Data
public class ModifyFriendApplicationResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 对方用户ID
     */
    private String userId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话类型 (0:单聊 1:群聊)
     */
    private Integer sessionType;

    /**
     * 会话名称
     */
    private String sessionName;

    /**
     * 头像URL
     */
    private String avatar;
}
