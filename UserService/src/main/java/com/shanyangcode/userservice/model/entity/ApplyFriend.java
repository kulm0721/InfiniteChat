package com.shanyangcode.userservice.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@TableName("apply_friend")
@Accessors(chain = true)
public class ApplyFriend {

    /** 好友申请ID（主键，使用分布式ID生成） */
    @TableId(value = "apply_friend_id", type = IdType.ASSIGN_ID)
    private Long applyFriendId;

    /** 发送者用户ID（申请发起人） */
    @TableField("sender_id")
    private Long senderId;

    /** 接收者用户ID（申请接收人） */
    @TableField("receiver_id")
    private Long receiverId;

    /** 申请信息（好友申请附加消息），默认值："请求添加好友" */
    @TableField("message")
    private String message;


    /**
     * 申请状态
     * 0: 未读（UNREAD）
     * 1: 通过（ACCEPTED）
     * 2: 拒绝（REJECTED）
     * 3: 已读（READ）
     * 4: 过期（EXPIRED）
     */
    @TableField("status")
    private Integer status;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}
