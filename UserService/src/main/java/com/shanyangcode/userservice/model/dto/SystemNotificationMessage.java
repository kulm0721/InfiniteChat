package com.shanyangcode.userservice.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 系统通知消息DTO
 *
 * 功能说明：
 * - 统一的系统通知消息格式，符合IM项目通知消息设计方案
 * - 通过type字段区分不同类型的系统通知
 * - 通过Kafka传输，由RealTimeService消费并推送给用户
 *
 * 消息类型（type字段）：
 * - 101：收到好友申请通知
 * - 102：新会话创建通知
 * - 103：新群聊会话创建通知（群组邀请）
 */
@Data
public class SystemNotificationMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一ID
     * 格式：msg_{timestamp}_{snowflakeId}
     * 用于消息追踪和去重
     */
    private String messageId;

    /**
     * 会话ID（可为null）
     * - 好友申请通知：null
     * - 新会话通知：会话ID
     * - 群组邀请通知：群组会话ID
     */
    private Long sessionId;

    /**
     * 发送者ID
     * - 系统消息：0
     * - 好友申请：申请人ID
     * - 新建会话：新建会话人ID
     * - 其他：根据业务场景设置
     */
    private Long senderId;

    /**
     * 接收者用户ID（需要接收通知的用户）
     */
    private Long receiverId;

    /**
     * 消息类型
     * 101: 收到好友申请
     * 102: 新会话创建
     * 103: 新群聊会话创建
     */
    private Integer type;

    /**
     * 会话类型（可为null）
     * 0: 单聊
     * 1: 群聊
     * 2: 机器人
     * null: 不适用于会话（如好友申请）
     */
    private Integer sessionType;

    /**
     * 消息创建时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 消息体（业务数据）
     * 不同类型的通知，body内容不同：
     * - type=101: {nickname: "张三", avatar: "http://...", msg: "你好我是xxx"}
     * - type=102: {sessionName: "张三", avatar: "http://..."}
     * - type=103: {sessionName: "技术交流群", avatar: "http://...", creatorId: 123, membersCount: 5}
     */
    private Map<String, Object> body;
}
