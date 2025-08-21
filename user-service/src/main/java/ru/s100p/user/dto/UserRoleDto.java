package ru.s100p.user.dto;


import java.time.LocalDateTime;

public record UserRoleDto(
        Long id,
        Long userId,
        String roleName,
        LocalDateTime assignedAt,
        Long assignedById
) {}

