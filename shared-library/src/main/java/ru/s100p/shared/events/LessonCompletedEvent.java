package ru.s100p.shared.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import ru.s100p.shared.constants.KafkaEventTypeNames;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class LessonCompletedEvent extends BaseEvent {
    private Long userId;
    private Long courseId;
    private Long lessonId;
    private String lessonTitle;
    private BigDecimal progressPercentage;
    private LocalDateTime completedAt;
    
    public LessonCompletedEvent() {
        super();
        setEventType(KafkaEventTypeNames.LESSON_COMPLETED);
    }
}