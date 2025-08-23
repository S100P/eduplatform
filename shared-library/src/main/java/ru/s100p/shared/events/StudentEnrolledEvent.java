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
public class StudentEnrolledEvent extends BaseEvent {
    private Long enrollmentId;
    private Long userId;
    private Long courseId;
    private String courseName;
    private BigDecimal amountPaid;
    private LocalDateTime enrolledAt;
    
    public StudentEnrolledEvent() {
        super();
        setEventType(KafkaEventTypeNames.ENROLLMENT_CREATED);
    }
}