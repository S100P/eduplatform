package ru.s100p.shared.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Value;
import ru.s100p.shared.constants.KafkaEventTypeNames;

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

    @Value("${spring.application.name}")
    private String sourceService;
    
    public UserRegisteredEvent() {
        super();
        setEventType(KafkaEventTypeNames.USER_REGISTERED);
        setSourceService(sourceService);
    } //TODO проверить насколько это обязательно, учитывая, что этот эвент публикует только один user-service и только в один топик userRegisteredTopic
}