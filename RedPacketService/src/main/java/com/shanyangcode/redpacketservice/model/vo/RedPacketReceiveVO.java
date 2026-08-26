package com.shanyangcode.redpacketservice.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 红包领取记录 VO
 * <p>
 * 用于红包详情中的领取记录展示
 */
@Data
public class RedPacketReceiveVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 领取者用户 ID
     */
    private Long receiverId;

    /**
     * 领取者昵称
     */
    private String receiverNickname;

    /**
     * 领取者头像
     */
    private String receiverAvatar;

    /**
     * 领取金额（单位：元）
     */
    private BigDecimal amount;

    /**
     * 领取时间
     */
    private Date receivedAt;
}
