package com.shanyangcode.userservice.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


/**
 * 修改好友申请请求DTO
 */
@Data
public class ModifyFriendApplicationRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 接收者用户Id列表
     * (通过和拒绝状态时只能包含一个；已读状态时可以包含多个)
     */
    @NotEmpty(message = "接收者ID列表不能为空")
    private List<String> receiveuserIds;
}
