package com.shanyangcode.userservice.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class GroupExitRequestDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
