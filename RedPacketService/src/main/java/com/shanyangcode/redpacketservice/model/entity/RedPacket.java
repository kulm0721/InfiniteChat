package com.shanyangcode.redpacketservice.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 红包实体类
 */
@Data
@TableName("red_packet")
public class RedPacket implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 红包 ID
     */
    @TableId
    private Long redPacketId;

    /**
     * 发送者用户 ID
     */
    private Long senderId;

    /**
     * 会话 ID（单聊或群聊）
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
     * 红包总金额（单位：分）
     */
    private Long totalAmount;

    /**
     * 红包总个数
     */
    private Integer totalCount;

    /**
     * 状态：0 未领取完，1 已领取完，2 已过期
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;
}
