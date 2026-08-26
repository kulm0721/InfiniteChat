package com.shanyangcode.common.model.dto.validation;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户状态响应
 *
 * 供内部校验接口返回用户状态信息
 * 供 UserService、RedPacketService 共用
 */
@Data
public class UserStatusResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户状态
     * @see UserStateConstant
     * 0: 正常
     * 1: 封禁
     * 2: 注销
     * null: 用户不存在
     */
    private Integer state;

    /**
     * 状态描述
     */
    private String stateName;

}


