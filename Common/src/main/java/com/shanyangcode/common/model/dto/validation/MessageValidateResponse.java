package com.shanyangcode.common.model.dto.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 消息发送权限校验响应
 *
 * 用于返回消息/红包发送权限校验结果
 * 供 UserService、RealTimeService、RedPacketService 共用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageValidateResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否允许发送
     */
    private boolean allowed;

    /**
     * 拒绝原因（可选）
     * 如：NOT_FRIEND, BLOCKED_BY_RECEIVER, FRIEND_DELETED 等
     * 使用 ValidationError.name() 获取
     * @see ValidationError
     */
    private String rejectReason;

    /**
     * 错误码（可选）
     * 91001-91008 范围
     * 使用 ValidationError.getCode() 获取
     * @see ValidationError
     */
    private Integer errorCode;

    /**
     * 创建允许响应
     */
    public static MessageValidateResponse allow() {
        return MessageValidateResponse.builder()
                .allowed(true)
                .build();
    }

    /**
     * 创建拒绝响应
     *
     * @param errorCode    错误码（使用 ValidationError.getCode()）
     * @param rejectReason 拒绝原因（使用 ValidationError.name()）
     * @return 拒绝响应
     */
    public static MessageValidateResponse reject(Integer errorCode, String rejectReason) {
        return MessageValidateResponse.builder()
                .allowed(false)
                .errorCode(errorCode)
                .rejectReason(rejectReason)
                .build();
    }
}
