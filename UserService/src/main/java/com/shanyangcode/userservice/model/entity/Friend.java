package com.shanyangcode.userservice.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@TableName("friend")
@Accessors(chain = true)
public class Friend {

    /**
     * 用户ID（复合主键之一）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 好友ID（复合主键之二）
     */
    @TableField("friend_id")
    private Long friendId;

    /**
     * 好友状态
     * 0: 好友（NORMAL）- 正常好友关系
     * 1: 拉黑（BLOCKED）- 已拉黑
     * 2: 删除（DELETED）- 已删除
     *
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("created_time")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField("updated_time")
    private LocalDateTime updatedTime;
}
