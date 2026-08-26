package com.shanyangcode.redpacketservice.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;


/**
 * 红包基本信息 VO（不含领取记录）
 */
@Data
public class RedPacketBasicVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 红包 ID
     */
    private Long redPacketId;

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
     * 已领取个数
     */
    private Integer receivedCount;

    /**
     * 已领取金额（单位：元）
     */
    private BigDecimal receivedAmount;

    /**
     * 状态：0 未领取完，1 已领取完，2 已过期
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createdTime;
}
