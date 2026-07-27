package com.shanyangcode.userservice.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户表
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User {
    /**
     * 用户ID
     */
    @TableId
    private Long userId;

    /**
     * 用户手机号
     */
    private String phone;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像url
     */
    private String avatar;

    /**
     * 性别 0 女 1 男 2 未知
     */
    private Integer gender;

    /**
     * 个性签名
     */
    private String description;

    /**
     * 状态 0 正常 1 封禁 2 注销
     */
    private Integer state;

    /**
     * 角色类型 0 普通用户 1 管理员 2 超级管理员
     */
    private Integer role;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;

    /**
     * 删除标记（0未删 1已删）
     */
    private Integer isDelete;
}