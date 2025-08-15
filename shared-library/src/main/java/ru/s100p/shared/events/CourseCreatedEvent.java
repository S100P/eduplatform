package ru.s100p.shared.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CourseCreatedEvent extends BaseEvent {
    private Long courseId;
    private String title;
    private String description;
    private Long instructorId;
    private BigDecimal price;
    private String category;
    
    public CourseCreatedEvent() {
        super();
        setEventType("COURSE_CREATED");
    }
}