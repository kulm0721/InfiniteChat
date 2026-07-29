package com.shanyangcode.common.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.util.Date;

@TableName(value="'message'")
@Data
public class Message {
    /**
     * 消息 id
     */
    @TableId
    private Long messageId;

    /**
     * 发送者 id
     */
    private Long senderId;

    /**
     * 会话 id
     */
    private Long sessionId;

    /**
     * 消息类型: 0 文本消息，1 图片消息，2 表情包，3 红包
     */
    private Integer type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息引用 id
     */
    private Long replyId;

    /**
     * 会话类型: 0 单聊，1 群聊
     */
    private Integer sessionType;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}
