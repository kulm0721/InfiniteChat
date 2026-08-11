package com.shanyangcode.userservice.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 会话表
 * @TableName session
 */
@TableName(value="session")
@Data
public class Session {
    /**
     * 会话 ID
     */
    @TableId
    private Long sessionId;

    /**
     * 名称
     */
    private String name;

    /**
     * 类别：0 单聊，1 群聊，2 AI
     */
    private Integer type;

    /**
     * 状态：0 正常，1 删除
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
