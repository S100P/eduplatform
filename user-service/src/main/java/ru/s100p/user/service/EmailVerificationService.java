// EmailVerificationService.java (заглушка)
package ru.s100p.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.s100p.user.entity.User;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;



@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    // Временное хранилище токенов (в продакшене использовать Redis)
    private final ConcurrentHashMap<String, Long> emailTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> resetTokens = new ConcurrentHashMap<>();

    public void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        emailTokens.put(token, user.getId());

        // TODO: Интеграция с email сервисом
        log.info("Отправка email верификации на {}: http://localhost:8080/api/v1/auth/verify-email?token={}",
                user.getEmail(), token);
    }

    public void sendPasswordResetEmail(User user, String resetToken) {
        resetTokens.put(resetToken, user.getId());

        // TODO: Интеграция с email сервисом
        log.info("Отправка email для сброса пароля на {}: http://localhost:8080/api/v1/auth/reset-password?token={}",
                user.getEmail(), resetToken);
    }

    public Long validateEmailToken(String token) {
        Long userId = emailTokens.remove(token);
        if (userId == null) {
            throw new IllegalArgumentException("Неверный или истекший токен");
        }
        return userId;
    }

    public Long validatePasswordResetToken(String token) {
        Long userId = resetTokens.remove(token);
        if (userId == null) {
            throw new IllegalArgumentException("Неверный или истекший токен");
        }
        return userId;
    }
}