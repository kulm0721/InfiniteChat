package com.shanyangcode.redpacketservice.constant;

public final class KafkaConstant {
    private KafkaConstant() {
        // 私有构造函数，防止实例化
    }

    // ==================== Kafka Topic 配置 ====================

    /**
     * Topic 默认分区数
     */
    public static final int DEFAULT_PARTITION_COUNT = 3;

    /**
     * Topic 默认副本数
     */
    public static final int DEFAULT_REPLICA_COUNT = 1;

    // ==================== Kafka 消费者配置 ====================

    /**
     * 消费者默认并发数
     */
    public static final String DEFAULT_CONCURRENCY = "3";

    // ==================== 消费者组 ID ====================

    /**
     * 红包领取事件消费者组
     */
    public static final String GROUP_RECEIVE_HANDLER = "redpacket-receive-handler-group";

    /**
     * 红包领完事件消费者组
     */
    public static final String GROUP_COMPLETED_HANDLER = "redpacket-completed-handler-group";

    /**
     * 延迟任务注册消费者组
     */
    public static final String GROUP_DELAY_TASK_ENQUEUER = "delay-task-enqueuer-group";

    /**
     * 过期任务执行消费者组
     */
    public static final String GROUP_EXPIRATION_EXECUTOR = "expiration-task-executor-group";
}
