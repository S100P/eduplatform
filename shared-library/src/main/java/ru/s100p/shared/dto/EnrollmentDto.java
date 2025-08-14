package ru.s100p.shared.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

//DTO для записи на курс

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnrollmentDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    Long id;
    Long userId;
    Long courseId;
    LocalDateTime enrolledAt;
    BigDecimal progress; // 0-100%
    String status; // ACTIVE, COMPLETED, CANCELLED
    LocalDateTime completedAt;
}
