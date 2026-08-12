package com.shanyangcode.userservice.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 好友详情VO
 *
 * 用于返回好友的详细信息
 */
@Data
public class FriendDetailVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 个性签名
     */
    private String signature;

    /**
     * 性别 (0:女 1:男 2:未知)
     */
    private Integer gender;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 好友状态 (0:好友 1:拉黑 2:删除 -1:非好友)
     */
    private Integer status;
}
