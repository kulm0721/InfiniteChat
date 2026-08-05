package com.shanyangcode.userservice.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value = "user_session")
@Data
public class UserSession {
    /**
     * 用户 id
     */

    private Long userId;

    /**
     * 会话 id
     */

    private Long sessionId;

    /**
     * 角色：0 群主，1 管理员，2 普通用户
     */
    private Integer role;

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
