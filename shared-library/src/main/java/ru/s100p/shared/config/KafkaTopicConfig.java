package ru.s100p.shared.config;


import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaTopicConfig {

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name("user-events")
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 дней
                .config(TopicConfig.SEGMENT_MS_CONFIG, "86400000")    // 1 день
                .build();
    }

    @Bean
    public NewTopic courseEventsTopic() {
        return TopicBuilder.name("course-events")
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")
                .build();
    }

    @Bean
    public NewTopic enrollmentEventsTopic() {
        return TopicBuilder.name("enrollment-events")
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")
                .build();
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name("payment-events")
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000") // 30 дней
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, "compact")
                .build();
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name("notification-events")
                .partitions(2)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "259200000") // 3 дня
                .build();
    }

    @Bean
    public NewTopic analyticsEventsTopic() {
        return TopicBuilder.name("analytics-events")
                .partitions(4)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000") // 30 дней
                .build();
    }

    // Dead Letter Topics
    @Bean
    public NewTopic userEventsDltTopic() {
        return TopicBuilder.name("user-events-dlt")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
