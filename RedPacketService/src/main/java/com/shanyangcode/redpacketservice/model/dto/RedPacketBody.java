package com.shanyangcode.redpacketservice.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 红包体 DTO（用于消息请求中的红包信息）
 */
@Data
public class RedPacketBody implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 红包类型：0 普通红包，1 拼手气红包
     */
    private Integer redPacketType;

    /**
     * 红包总金额（单位：元）
     */
    private BigDecimal totalAmount;

    /**
     * 红包总个数
     */
    private Integer totalCount;

    /**
     * 红包封面文案
     */
    private String redPacketWrapperText;
}
