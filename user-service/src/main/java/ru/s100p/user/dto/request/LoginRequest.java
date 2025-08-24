package ru.s100p.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username или Email не может быть пустым")
    private String usernameOrEmail;

    @NotBlank(message = "Пароль не может быть пустым")
    private String password;

    // Запомнить меня - для долгоживущего refresh token
    private boolean rememberMe;
}
