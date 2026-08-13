package com.shanyangcode.common.constant;

/**
 * Kafka Topic 与消费者组常量
 * <p>
 * 统一维护所有 Kafka topic 名称与消费者组 ID，避免魔法字符串散落各处。
 * 跨服务（如 UserService 生产、RealTimeService 消费）的 topic 必须放这里，
 * 供双方共同依赖的 Common 模块引用。
 */
public class KafkaTopicConstant {

    /**
     * 消息存储 topic（消息落库，OfflineDataService 消费）
     */
    public static final String TOPIC_MESSAGE_STORE = "store-topic";

    /**
     * 消息推送 topic（实时推送，RealTimeService 消费）
     */
    public static final String TOPIC_MESSAGE_PUSH = "message-topic";

    /**
     * 系统通知消息 topic
     * 用途：发送系统通知消息（新会话、好友申请、群聊邀请等）
     * Producer：UserService
     * Consumer：RealTimeService
     */
    public static final String TOPIC_SYSTEM_NOTIFICATION = "system-notification-topic";

    /**
     * 系统通知持久化 topic
     * 用途：用户离线时通知消息落库，待用户上线后拉取
     */
    public static final String TOPIC_NOTIFICATION_STORE = "store-notification-topic";

    /**
     * 好友申请创建事件 topic
     * 用途：好友申请创建时发送事件，用于注册延迟过期任务
     * Producer：UserService (ApplyFriendService)
     * Consumer：UserService (FriendRequestExpirationEnqueuer)
     */
    public static final String TOPIC_FRIEND_REQUEST_CREATION = "friend-request-creation-topic";

    /**
     * 好友申请过期事件 topic
     * 用途：定时任务扫描到过期的好友申请时发送事件
     * Producer：UserService (FriendRequestExpirationDispatcher)
     * Consumer：UserService (FriendRequestExpirationExecutor)
     */
    public static final String TOPIC_FRIEND_REQUEST_EXPIRATION = "friend-request-expiration-topic";

    /**
     * 系统通知消费者组 ID
     */
    public static final String GROUP_SYSTEM_NOTIFICATION_CONSUMER = "system-notification-consumer-group";

    /**
     * 好友申请创建事件消费者组 ID
     */
    public static final String GROUP_FRIEND_REQUEST_ENQUEUER = "friend-request-enqueuer-group";

    /**
     * 好友申请过期事件消费者组 ID
     */
    public static final String GROUP_FRIEND_REQUEST_EXECUTOR = "friend-request-executor-group";
}
