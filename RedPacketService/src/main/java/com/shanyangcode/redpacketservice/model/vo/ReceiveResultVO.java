package com.shanyangcode.redpacketservice.model.vo;

import com.shanyangcode.redpacketservice.constant.RedPacketConstant;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 红包领取结果 VO
 * <p>
 * 专用于领取红包接口 /chat/redPacket/receive 的返回值
 */
@Data
public class ReceiveResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 红包状态
     * <p>
     * 参见 {@link RedPacketConstant}:
     * <ul>
     *   <li>STATUS_NOT_COMPLETED (0): 领取成功</li>
     *   <li>STATUS_COMPLETED (1): 红包已领完</li>
     *   <li>STATUS_EXPIRED (2): 红包已过期</li>
     *   <li>STATUS_ALREADY_RECEIVED (3): 已经领取过</li>
     * </ul>
     */
    private Integer status;

    /**
     * 结果消息
     */
    private String message;

    /**
     * 领取金额（单位：元）
     * <p>
     * 仅当 status=0（领取成功）或 status=3（已领取过）时有值
     */
    private BigDecimal amount;
}
