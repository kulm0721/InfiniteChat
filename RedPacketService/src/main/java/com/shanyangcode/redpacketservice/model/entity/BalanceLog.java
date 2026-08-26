package com.shanyangcode.redpacketservice.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 余额变动记录实体类
 */
@Data
@TableName("balance_log")
public class BalanceLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录 ID
     */
    @TableId
    private Long balanceLogId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 变动金额（单位：分），正数为增加，负数为减少
     */
    private Long amount;

    /**
     * 变动类型：0 发送红包，1 领取红包，2 红包退回
     */
    private Integer type;

    /**
     * 关联 ID，如红包 ID
     */
    private Long relatedId;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;
}
