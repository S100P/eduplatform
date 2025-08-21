package ru.s100p.user.kafka;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.s100p.shared.dto.UserDto;
import ru.s100p.shared.events.UserRegisteredEvent;
import ru.s100p.user.entity.Role;
import ru.s100p.user.entity.User;
import ru.s100p.user.entity.UserRole;

import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Slf4j
@Component
public class UserServiceProducer {

    @Value("${spring.kafka.producer.topic.name}")
    private String topicName;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserRegistered(User user) {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setUserId(user.getId());
        event.setEmail(user.getEmail());
        event.setFirstName(user.getFirstName());
        event.setLastName(user.getLastName());
        event.setRoles(user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getName)
                .collect(Collectors.toUnmodifiableSet()));

        kafkaTemplate.send(topicName, user.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("User registered event sent successfully: {}", event.getEventId());
                    } else {
                        log.error("Failed to send user registered event: {}", event.getEventId(), ex);
                    }
                });
    }

    //TODO public void publishUserUpdated

}
