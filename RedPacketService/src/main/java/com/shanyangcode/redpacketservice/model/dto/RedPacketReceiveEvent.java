// RedPacketReceiveEvent.java
package com.shanyangcode.redpacketservice.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 红包领取事件 DTO（用于 Kafka）
 */
@Data
public class RedPacketReceiveEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long userId;

    /** 红包 ID */
    private Long redPacketId;

    /** 领取金额（单位：分） */
    private Long receivedAmount;

    /** 领取时间（毫秒时间戳） */
    private Long receiveTime;
}
