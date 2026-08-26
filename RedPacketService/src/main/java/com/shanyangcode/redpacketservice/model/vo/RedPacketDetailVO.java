package com.shanyangcode.redpacketservice.model.vo;


import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 红包详情 VO
 */
@Data
public class RedPacketDetailVO implements Serializable {

    /**
     * 红包 ID
     */
    private Long redPacketId;

    /**
     * 发送者用户 ID
     */
    private Long senderId;

    /**
     * 发送者昵称
     */
    private String senderNickname;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 会话 ID
     */
    private Long sessionId;


    /**
     * 会话类型：0 单聊，1 群聊
     */
    private Integer sessionType;

    /**
     * 红包封面文案
     */
    private String redPacketWrapperText;

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

    /**
     * 领取记录列表
     */
    private List<RedPacketReceiveVO> receiveRecords;
}
