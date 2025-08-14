package ru.s100p.shared.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    Long id;

    @NotBlank(message = "Title is required")
    String title;

    String description;
    BigDecimal price;
    String instructorName;
    Long instructorId;
    String category;
    Integer duration; // в часах
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    boolean published;
}
