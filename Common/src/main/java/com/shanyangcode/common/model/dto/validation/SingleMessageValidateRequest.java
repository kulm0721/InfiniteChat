package com.shanyangcode.common.model.dto.validation;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 单聊消息发送权限校验请求
 *
 * 用于校验单聊消息/红包发送前的好友关系验证
 * 供 UserService、RealTimeService、RedPacketService 共用
 */
@Data
public class SingleMessageValidateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 发送者 ID
     */
    private Long senderId;

    /**
     * 接收者 ID
     */
    private Long receiverId;

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 创建校验请求
     *
     * @param senderId   发送者 ID
     * @param receiverId 接收者 ID
     * @param sessionId  会话 ID
     * @return 校验请求对象
     */
    public static SingleMessageValidateRequest of(Long senderId, Long receiverId, Long sessionId) {
        SingleMessageValidateRequest request = new SingleMessageValidateRequest();
        request.setSenderId(senderId);
        request.setReceiverId(receiverId);
        request.setSessionId(sessionId);
        return request;
    }
}
