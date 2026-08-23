package com.shanyangcode.userservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class KickGroupMembersRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    @NotNull(message = "操作者ID不能为空")
    private Long operatorId;

    @NotEmpty(message = "被踢出成员ID列表不能为空")
    private List<Long> memberIds;
}
