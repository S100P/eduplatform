package ru.s100p.shared.events;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
public abstract class BaseEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String sourceService;
    private String correlationId;
    
    protected BaseEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }
}