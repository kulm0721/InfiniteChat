package com.shanyangcode.common.model.dto;

import lombok.Data;

@Data
public class MessageBody {
    private String content;

    private Long replyId;

    /**
     * 红包 ID，仅红包消息使用
     */
    private String redPacketId;

    /**
     * 红包封面文案，仅红包消息使用
     */
    private String redPacketWrapperText;
}
