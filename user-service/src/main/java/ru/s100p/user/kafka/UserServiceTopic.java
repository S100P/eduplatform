package ru.s100p.user.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;
import java.util.Map;

import static ru.s100p.shared.constants.KafkaTopicNames.USER_PROFILE_UPDATED_TOPIC;
import static ru.s100p.shared.constants.KafkaTopicNames.USER_REGISTERED_TOPIC;

@Configuration
public class UserServiceTopic {


    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(USER_REGISTERED_TOPIC)
                .partitions(5)
                .replicas(3)
                .configs(Map.of("min.insync.replicas", "2")) // два сервера должны быть в синхроне с сервер-лидером
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
                .config(TopicConfig.SEGMENT_MS_CONFIG, String.valueOf(Duration.ofDays(1).toMillis()))
                .build();
    }

    @Bean
    public NewTopic userUpdatedTopic() {
        return TopicBuilder.name(USER_PROFILE_UPDATED_TOPIC)
                .partitions(5)
                .replicas(3)
                .configs(Map.of("min.insync.replicas", "2")) // два сервера должны быть в синхроне с сервер-лидером
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
                .config(TopicConfig.SEGMENT_MS_CONFIG, String.valueOf(Duration.ofDays(1).toMillis()))
                .build();
    }


}
