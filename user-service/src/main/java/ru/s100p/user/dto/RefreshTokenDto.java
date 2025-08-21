package ru.s100p.user.dto;


import java.time.LocalDateTime;

public record RefreshTokenDto(
        Long id,
        Long userId,
        String token,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        Boolean isRevoked
) {}

