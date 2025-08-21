package ru.s100p.shared.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record UserDto(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        LocalDate dateOfBirth,
        String bio,
        String avatarUrl,
        Boolean isActive,
        Boolean isEmailVerified,
        LocalDateTime lastLogin,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Set<String> roles
) {}
