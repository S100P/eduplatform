package ru.s100p.user.mapper;


import ru.s100p.user.dto.UserRoleDto;
import ru.s100p.user.entity.UserRole;

public final class UserRoleMapper {

    private UserRoleMapper() {}

    public static UserRoleDto toDto(UserRole ur) {
        if (ur == null) return null;
        return new UserRoleDto(
                ur.getId(),
                ur.getUser() != null ? ur.getUser().getId() : null,
                ur.getRole() != null ? ur.getRole().getName() : null,
                ur.getAssignedAt(),
                ur.getAssignedBy() != null ? ur.getAssignedBy().getId() : null
        );
    }
}

