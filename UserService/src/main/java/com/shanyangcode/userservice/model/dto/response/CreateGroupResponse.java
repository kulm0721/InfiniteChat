package com.shanyangcode.userservice.model.dto.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 创建群聊响应 DTO
 */
@Data
public class CreateGroupResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建者ID */
    private String creatorId;

    /** 会话 ID */
    private String sessionId;

    /** 会话名称（群名） */
    private String sessionName;

    /** 会话类型 (1:群聊) */
    private Integer sessionType;

    /** 群头像 URL */
    private String avatar;

    /** 邀请失败的成员 ID 列表 */
    private List<String> failedMemberIds;
}
