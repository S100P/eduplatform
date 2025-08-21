package ru.s100p.user.dto;

import java.time.LocalDateTime;

public record RoleDto(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt
) {}
