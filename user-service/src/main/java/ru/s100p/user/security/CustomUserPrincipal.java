package ru.s100p.user.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.s100p.user.entity.User;
import ru.s100p.user.entity.UserRole;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Кастомная реализация UserDetails
 * Содержит дополнительную информацию о пользователе
 */
class CustomUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final boolean emailVerified;

    private CustomUserPrincipal(Long id,
                                String username,
                                String email,
                                String password,
                                Collection<? extends GrantedAuthority> authorities,
                                boolean enabled,
                                boolean emailVerified) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
        this.emailVerified = emailVerified;
    }

    /**
     * Создание CustomUserPrincipal из сущности User
     */
    public static CustomUserPrincipal create(User user) {
        // Преобразуем роли в GrantedAuthority
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(UserRole::getRole)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        // Добавляем дополнительные permissions если нужно
        authorities.addAll(getPermissionsForRoles(user.getRoles()));

        return new CustomUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                authorities,
                user.getIsActive(),
                user.getIsEmailVerified()
        );
    }

    /**
     * Получение дополнительных permissions на основе ролей
     */
    private static Set<GrantedAuthority> getPermissionsForRoles(Set<UserRole> userRoles) {
        Set<GrantedAuthority> permissions = userRoles.stream()
                .map(UserRole::getRole)
                .flatMap(role -> {
                    // Маппинг ролей на permissions //TODO переписать на константы
                    return switch (role.getName()) {
                        case "ADMIN" -> Set.of(
                                new SimpleGrantedAuthority("READ_ALL"),
                                new SimpleGrantedAuthority("WRITE_ALL"),
                                new SimpleGrantedAuthority("DELETE_ALL"),
                                new SimpleGrantedAuthority("MANAGE_USERS"),
                                new SimpleGrantedAuthority("MANAGE_COURSES")
                        ).stream();
                        case "INSTRUCTOR" -> Set.of(
                                new SimpleGrantedAuthority("CREATE_COURSE"),
                                new SimpleGrantedAuthority("EDIT_OWN_COURSE"),
                                new SimpleGrantedAuthority("DELETE_OWN_COURSE"),
                                new SimpleGrantedAuthority("VIEW_STUDENTS")
                        ).stream();
                        case "STUDENT" -> Set.of(
                                new SimpleGrantedAuthority("ENROLL_COURSE"),
                                new SimpleGrantedAuthority("VIEW_COURSES"),
                                new SimpleGrantedAuthority("SUBMIT_ASSIGNMENT")
                        ).stream();
                        default -> Set.<GrantedAuthority>of().stream();
                    };
                })
                .collect(Collectors.toSet());

        return permissions;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    // UserDetails методы
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Можно добавить логику проверки срока действия аккаунта
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled; // Если аккаунт деактивирован, считаем его заблокированным
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Можно добавить логику проверки срока действия пароля
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
