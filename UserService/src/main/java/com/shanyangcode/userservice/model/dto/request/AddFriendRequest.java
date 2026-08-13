package com.shanyangcode.userservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 添加好友请求Request
 */
@Data
public class AddFriendRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 申请消息
     */
    @NotBlank(message = "申请消息不能为空")
    private String msg;
}
