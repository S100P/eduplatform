package ru.s100p.user.mapper;


import ru.s100p.user.dto.RoleDto;
import ru.s100p.user.entity.Role;

public final class RoleMapper {

    private RoleMapper() {}

    public static RoleDto toDto(Role role) {
        if (role == null) return null;
        return new RoleDto(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getCreatedAt()
        );
    }
}

