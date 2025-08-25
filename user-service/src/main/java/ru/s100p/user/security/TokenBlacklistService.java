// TokenBlacklistService.java (заглушка)
package ru.s100p.user.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TokenBlacklistService {

    // Временное хранилище (в продакшене использовать Redis)
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Long> passwordResetTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
        log.debug("Токен добавлен в черный список");
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    public void savePasswordResetToken(Long userId, String token) {
        passwordResetTokens.put(token, userId);
        log.debug("Токен сброса пароля сохранен для пользователя: {}", userId);
    }

    public Long validatePasswordResetToken(String token) {
        Long userId = passwordResetTokens.get(token);
        if (userId == null) {
            throw new IllegalArgumentException("Неверный или истекший токен сброса пароля");
        }
        return userId;
    }

    public void deletePasswordResetToken(String token) {
        passwordResetTokens.remove(token);
    }
}
