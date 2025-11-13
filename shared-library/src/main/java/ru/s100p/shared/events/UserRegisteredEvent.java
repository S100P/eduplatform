package ru.s100p.shared.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import ru.s100p.shared.constants.KafkaEventTypeNames;
import ru.s100p.shared.constants.KafkaServiceNames;

import java.util.Set;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserRegisteredEvent extends BaseEvent {
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;


    // Установка метаданных события
    public UserRegisteredEvent() {
        super();
        setEventType(KafkaEventTypeNames.USER_REGISTERED);
        setSourceService(KafkaServiceNames.USER_SERVICE);
        setCorrelationId(String.valueOf(userId));
    }
}