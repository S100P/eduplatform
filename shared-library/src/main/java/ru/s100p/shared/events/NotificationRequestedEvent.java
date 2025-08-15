package ru.s100p.shared.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class NotificationRequestedEvent extends BaseEvent {
    private Long userId;
    private String notificationType; // EMAIL, PUSH, SMS
    private String templateId;
    private Map<String, Object> templateVariables;
    private String subject;
    private String content;
    private String recipient;
    
    public NotificationRequestedEvent() {
        super();
        setEventType("NOTIFICATION_REQUESTED");
    }
}