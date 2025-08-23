package ru.s100p.notification.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;
import java.util.Map;

import static ru.s100p.shared.constants.KafkaTopicNames.NOTIFICATION_REQUESTED_TOPIC;

@Configuration
public class NotificationServiceTopic {

    @Bean
    public NewTopic notificationRequestedTopic() {
        return TopicBuilder.name(NOTIFICATION_REQUESTED_TOPIC)
                .partitions(5)
                .replicas(3)
                .configs(Map.of("min.insync.replicas", "2")) // два сервера должны быть в синхроне с сервер-лидером
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
                .config(TopicConfig.SEGMENT_MS_CONFIG, String.valueOf(Duration.ofDays(1).toMillis()))
                .build();
    }
}
