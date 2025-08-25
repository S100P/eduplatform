package ru.s100p.user.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.s100p.user.entity.User;

import ru.s100p.user.repository.UserRepository;


/**
 * Сервис для загрузки пользователей Spring Security
 * Используется для аутентификации и авторизации
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Загрузка пользователя по username для Spring Security
     * Также поддерживает вход по email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Загрузка пользователя для аутентификации: {}", usernameOrEmail);

        // Пытаемся найти пользователя по username или email
        User user = userRepository.findWithRolesByUsername(usernameOrEmail).or(() -> userRepository.findWithRolesByEmail(usernameOrEmail)).orElseThrow(() -> {
            log.error("Пользователь не найден: {}", usernameOrEmail);
            return new UsernameNotFoundException(String.format("Пользователь с username или email '%s' не найден", usernameOrEmail));
        });

        log.debug("Пользователь найден: {} с {} ролями", user.getUsername(), user.getRoles().size());

        // Создаем CustomUserPrincipal
        return CustomUserPrincipal.create(user);
    }

    /**
     * Загрузка пользователя по ID
     * Используется для обновления контекста безопасности
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        log.debug("Загрузка пользователя по ID: {}", id);

        User user = userRepository.findWithRolesById(id).orElseThrow(() -> {
            log.error("Пользователь не найден с ID: {}", id);
            return new UsernameNotFoundException(String.format("Пользователь с ID '%d' не найден", id));
        });

        return CustomUserPrincipal.create(user);
    }
}
