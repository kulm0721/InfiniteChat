package com.shanyangcode.userservice.model.dto.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


@Data
public class InviteGroupResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 成功邀请的用户ID列表
     */
    private List<Long> successIds;

    /**
     * 邀请失败的用户ID列表
     */
    private List<Long> failedIds;
}
