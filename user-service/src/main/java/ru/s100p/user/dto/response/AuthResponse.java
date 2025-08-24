package ru.s100p.user.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.s100p.shared.dto.UserDto;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // в секундах
    private LocalDateTime issuedAt;
    private UserDto user;

    // Дополнительная информация
    private boolean firstLogin;
    private boolean emailVerified;
    private String message;
}
