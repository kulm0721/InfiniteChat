package com.shanyangcode.common.model.dto.validation;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 群成员数量响应
 *
 * 供内部校验接口返回群成员数量信息
 * 供 UserService、RedPacketService 共用
 */
@Data
public class GroupMemberCountResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话 ID（群聊 ID）
     */
    private Long sessionId;

    /**
     * 群成员数量
     */
    private Integer memberCount;

    /**
     * 创建响应对象
     *
     * @param sessionId   会话 ID
     * @param memberCount 成员数量
     * @return 响应对象
     */
    public static GroupMemberCountResponse of(Long sessionId, Integer memberCount) {
        GroupMemberCountResponse response = new GroupMemberCountResponse();
        response.setSessionId(sessionId);
        response.setMemberCount(memberCount);
        return response;
    }
}
