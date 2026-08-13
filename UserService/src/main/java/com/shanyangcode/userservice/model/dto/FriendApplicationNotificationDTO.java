package com.shanyangcode.userservice.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 好友申请通知DTO
 *
 * 场景：用户A向用户B发送好友申请，通知用户B
 */
@Data
public class FriendApplicationNotificationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 申请者用户昵称
     */
    private String applyUserName;

    /**
     * 申请者用户ID
     */
    private Long applyUserId;

    /**
     * 申请附言（用于推送通知）
     */
    private String message;

    /**
     * 申请者头像URL
     */
    private String applyFriendAvatar;
}
