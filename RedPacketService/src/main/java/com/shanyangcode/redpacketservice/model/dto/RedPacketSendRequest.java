package com.shanyangcode.redpacketservice.model.dto;


import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 红包发送请求 DTO
 */
@Data
public class RedPacketSendRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 接收者 ID（单聊时使用）
     */
    private Long receiverId;

    /**
     * 发送者 ID
     */
    private Long senderId;

    /**
     * 消息类型（3 代表红包）
     */
    private Integer type;

    /**
     * 会话类型：0 单聊，1 群聊
     */
    private Integer sessionType;

    /**
     * 红包体信息
     */
    private RedPacketBody body;

    /**
     * 客户端消息 ID
     */
    private String clientMessageId;
}
