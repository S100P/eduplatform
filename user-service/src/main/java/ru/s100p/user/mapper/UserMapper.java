package ru.s100p.user.mapper;

import ru.s100p.shared.dto.UserDto;
import ru.s100p.user.entity.Role;
import ru.s100p.user.entity.User;
import ru.s100p.user.entity.UserRole;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {}

    /**
     * Полное преобразование User в UserDto (для владельца аккаунта и админов)
     */
    public static UserDto toDto(User user) {
        if (user == null) return null;

        Set<String> roleNames = extractRoleNames(user);

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

    /**
     * Публичное преобразование User в UserDto (скрывает приватную информацию)
     */
    public static UserDto toPublicDto(User user) {
        if (user == null) return null;

        // Для публичного профиля скрываем некоторые данные
        return new UserDto(
                user.getId(),
                user.getUsername(),
                null, // email скрыт
                user.getFirstName(),
                user.getLastName(),
                null, // phone скрыт
                null, // dateOfBirth скрыт
                user.getBio(),
                user.getAvatarUrl(),
                user.getIsActive(),
                null, // isEmailVerified скрыт
                null, // lastLogin скрыт
                user.getCreatedAt(),
                null, // updatedAt скрыт
                Set.of() // роли скрыты
        );
    }

    /**
     * Преобразование для списков (минимальная информация)
     */
    public static UserDto toListDto(User user) {
        if (user == null) return null;

        return new UserDto(
                user.getId(),
                user.getUsername(),
                null,
                user.getFirstName(),
                user.getLastName(),
                null,
                null,
                null,
                user.getAvatarUrl(),
                user.getIsActive(),
                null,
                null,
                null,
                null,
                Set.of()
        );
    }

    /**
     * Преобразование для административных целей (полная информация)
     */
    public static UserDto toAdminDto(User user) {
        if (user == null) return null;

        Set<String> roleNames = extractRoleNames(user);

        // Для админов показываем всю информацию
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

    /**
     * Извлечение названий ролей из сущности User
     */
    private static Set<String> extractRoleNames(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return Set.of();
        }

        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Создание User из UserDto (для тестов и внутреннего использования)
     */
    public static User toEntity(UserDto dto) {
        if (dto == null) return null;

        User user = new User();
        user.setId(dto.id());
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPhone(dto.phone());
        user.setDateOfBirth(dto.dateOfBirth());
        user.setBio(dto.bio());
        user.setAvatarUrl(dto.avatarUrl());
        user.setIsActive(dto.isActive());
        user.setIsEmailVerified(dto.isEmailVerified());
        user.setLastLogin(dto.lastLogin());
        user.setCreatedAt(dto.createdAt());
        user.setUpdatedAt(dto.updatedAt());

        return user;
    }
}
