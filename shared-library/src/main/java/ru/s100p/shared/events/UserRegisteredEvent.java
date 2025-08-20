package ru.s100p.shared.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import ru.s100p.shared.constants.KafkaTopicsConstants;

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
        setEventType(KafkaTopicsConstants.USER_REGISTERED.name());
        setSourceService(sourceService);
    }
}