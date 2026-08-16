package com.shanyangcode.userservice.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class InviteGroupRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    /**
     * 邀请者用户ID
     */
    @NotNull(message = "邀请者ID不能为空")
    private Long inviterId;

    /**
     * 被邀请者用户ID
     */
    @NotEmpty(message = "被邀请成员ID列表不能为空")
    private List<Long> inviteeIds;
}
