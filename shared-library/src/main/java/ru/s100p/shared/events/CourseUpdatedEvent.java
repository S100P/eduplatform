package ru.s100p.shared.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import ru.s100p.shared.constants.KafkaTopicsConstants;

import java.math.BigDecimal;
import java.util.Map;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CourseUpdatedEvent extends BaseEvent {
    private Long courseId;
    private String title;
    private String description;
    private BigDecimal price;
    private Map<String, Object> changes;
    
    public CourseUpdatedEvent() {
        super();
        setEventType(KafkaTopicsConstants.COURSE_UPDATED.name());
    }
}