package com.shanyangcode.userservice.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 好友申请DTO
 * <p>
 * 用于返回好友申请列表数据
 */
@Data
public class ApplyFriendDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 对方用户ID（发送者或接收者）
     */
    private String userId;

    /**
     * 对方用户昵称
     */
    private String nickname;

    /**
     * 对方用户头像
     */
    private String avatar;

    /**
     * 申请消息
     */
    private String msg;

    /**
     * 申请状态 (0:未读 1:通过 2:拒绝 3:已读 4:过期)
     */
    private Integer status;

    /**
     * 更新时间
     */
    private LocalDateTime time;

    /**
     * 是否为接收者 (0:否-我是发送者 1:是-我是接收者)
     */
    private Integer isReceiver;
}
