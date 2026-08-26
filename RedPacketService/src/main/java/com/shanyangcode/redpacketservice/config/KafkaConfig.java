package com.shanyangcode.redpacketservice.config;

import com.shanyangcode.redpacketservice.constant.KafkaConstant;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    /**
     * 红包创建事件主题
     */
    public static final String TOPIC_REDPACKET_CREATION = "redpacket-creation-topic";

    /**
     * 红包领取事件主题
     */
    public static final String TOPIC_REDPACKET_RECEIVE = "topic-redpacket-receive";

    /**
     * 红包领完事件主题
     */
    public static final String TOPIC_REDPACKET_COMPLETED = "topic-redpacket-completed";

    /**
     * 红包过期事件主题
     */
    public static final String TOPIC_REDPACKET_EXPIRATION = "redpacket-expiration-topic";

    /**
     * 创建红包创建事件主题
     */
    @Bean
    public NewTopic redpacketCreationTopic() {
        return TopicBuilder.name(TOPIC_REDPACKET_CREATION)
                .partitions(KafkaConstant.DEFAULT_PARTITION_COUNT)
                .replicas(KafkaConstant.DEFAULT_REPLICA_COUNT)
                .build();
    }

    /**
     * 创建红包领取事件主题
     */
    @Bean
    public NewTopic redpacketReceiveTopic() {
        return TopicBuilder.name(TOPIC_REDPACKET_RECEIVE)
                .partitions(KafkaConstant.DEFAULT_PARTITION_COUNT)
                .replicas(KafkaConstant.DEFAULT_REPLICA_COUNT)
                .build();
    }

    /**
     * 创建红包领完事件主题
     */
    @Bean
    public NewTopic redpacketCompletedTopic() {
        return TopicBuilder.name(TOPIC_REDPACKET_COMPLETED)
                .partitions(KafkaConstant.DEFAULT_PARTITION_COUNT)
                .replicas(KafkaConstant.DEFAULT_REPLICA_COUNT)
                .build();
    }

    /**
     * 创建红包过期事件主题
     */
    @Bean
    public NewTopic redpacketExpirationTopic() {
        return TopicBuilder.name(TOPIC_REDPACKET_EXPIRATION)
                .partitions(KafkaConstant.DEFAULT_PARTITION_COUNT)
                .replicas(KafkaConstant.DEFAULT_REPLICA_COUNT)
                .build();
    }
}
