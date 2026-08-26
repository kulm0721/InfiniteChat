package com.shanyangcode.common.model.dto.validation;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 群成员资格响应
 *
 * 供内部校验接口返回群成员资格信息
 * 供 UserService、RedPacketService 共用
 */
@Data
public class GroupMembershipResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 会话 ID（群聊 ID）
     */
    private Long sessionId;

    /**
     * 是否为群成员
     */
    private Boolean isMember;

    /**
     * 成员角色
     * 0: 群主
     * 1: 管理员
     * 2: 普通成员
     * null: 非成员
     */
    private Integer role;

    /**
     * 群成员角色常量：群主
     */
    public static final int ROLE_OWNER = 0;

    /**
     * 群成员角色常量：管理员
     */
    public static final int ROLE_ADMIN = 1;

    /**
     * 群成员角色常量：普通成员
     */
    public static final int ROLE_MEMBER = 2;

    /**
     * 创建成员响应
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param isMember  是否为成员
     * @param role      角色
     * @return 响应对象
     */
    public static GroupMembershipResponse of(Long userId, Long sessionId, Boolean isMember, Integer role) {
        GroupMembershipResponse response = new GroupMembershipResponse();
        response.setUserId(userId);
        response.setSessionId(sessionId);
        response.setIsMember(isMember);
        response.setRole(role);
        return response;
    }
}
