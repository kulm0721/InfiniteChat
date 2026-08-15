package com.shanyangcode.userservice.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 新会话通知DTO（消息体body部分）
 * <p>
 * 场景：好友申请通过后，系统创建新的单聊会话，通知双方用户
 * <p>
 * 注意：sessionId、sessionType 等字段已提升至 SystemNotificationMessage 顶层
 * 此DTO仅作为 SystemNotificationMessage.body 的内容
 */
@Data
public class NewSessionNotificationDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话名称（通常是对方用户昵称）
     */
    private String sessionName;

    /**
     * 头像URL
     */
    private String avatar;
}
