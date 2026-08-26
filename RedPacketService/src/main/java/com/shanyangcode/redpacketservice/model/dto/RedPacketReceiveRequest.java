package com.shanyangcode.redpacketservice.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 红包领取请求 DTO
 */
@Data
public class RedPacketReceiveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 红包 ID
     */
    private Long redPacketId;
}
