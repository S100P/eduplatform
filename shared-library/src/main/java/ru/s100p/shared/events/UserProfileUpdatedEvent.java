package ru.s100p.shared.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import ru.s100p.shared.constants.KafkaEventTypeNames;

import java.util.Map;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserProfileUpdatedEvent extends BaseEvent {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private Map<String, Object> changes; // что именно изменилось
    
    public UserProfileUpdatedEvent() {
        super();
        setEventType(KafkaEventTypeNames.USER_PROFILE_UPDATED);
    }
}