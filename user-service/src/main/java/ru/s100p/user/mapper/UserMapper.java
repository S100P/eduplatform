package ru.s100p.user.mapper;


import ru.s100p.shared.dto.UserDto;
import ru.s100p.user.entity.User;
import ru.s100p.user.entity.UserRole;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {}

    public static UserDto toDto(User user) {
        if (user == null) return null;

        Set<String> roleNames = user.getRoles() == null ? Set.of() :
                user.getRoles().stream()
                        .filter(Objects::nonNull)
                        .map(UserRole::getRole)
                        .filter(Objects::nonNull)
                        .map(r -> r.getName())
                        .collect(Collectors.toUnmodifiableSet());

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getDateOfBirth(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getIsActive(),
                user.getIsEmailVerified(),
                user.getLastLogin(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                roleNames
        );
    }
}
