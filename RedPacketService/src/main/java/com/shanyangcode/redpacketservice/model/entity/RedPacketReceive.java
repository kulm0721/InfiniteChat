package com.shanyangcode.redpacketservice.model.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 红包领取记录实体类
 */
@Data
@TableName("red_packet_receive")
public class RedPacketReceive implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录 ID
     */
    @TableId
    private Long redPacketReceiveId;

    /**
     * 红包 ID
     */
    private Long redPacketId;

    /**
     * 领取者用户 ID
     */
    private Long receiverId;

    /**
     * 领取金额（单位：分）
     */
    private Long amount;

    /**
     * 领取时间
     */
    private Date receivedAt;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;
}
