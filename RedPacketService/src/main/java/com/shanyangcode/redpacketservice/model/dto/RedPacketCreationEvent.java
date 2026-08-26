package com.shanyangcode.redpacketservice.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 红包创建事件 DTO（用于 Kafka）
 */
@Data
public class RedPacketCreationEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 红包 ID
     */
    private Long redPacketId;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long createTime;
}
