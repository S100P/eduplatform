package ru.s100p.shared.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 2, max = 50, message = "Имя не должно превышать 50 символов или быть меньше 2 символов")
    String username;

    @Email(message = "Email should be valid")
    String email;

    String firstName;
    String lastName;
    LocalDateTime createdAt;
    Set<String> roles;
    boolean active;
}
