package ru.s100p.user.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для создания JSON Web Tokens (JWT).
 * <p>
 * В текущей архитектуре этот сервис отвечает <b>только за генерацию</b> токенов.
 * Вся логика по валидации, парсингу и проверке токенов вынесена в `api-gateway`.
 * </p>
 * <p>
 * Этот сервис используется в {@link ru.s100p.user.service.AuthService} для создания
 * access и refresh токенов при успешном логине пользователя.
 * </p>
 */
@Slf4j
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}") // 1 час в миллисекундах
    private int jwtExpirationMs;

    /**
     * Генерация access токена для аутентифицированного пользователя.
     * <p>
     * Извлекает ID пользователя и его роли из объекта {@link CustomUserPrincipal}
     * и создает на их основе JWT с коротким сроком жизни.
     * </p>
     *
     * @param authentication объект аутентификации Spring Security, содержащий {@link CustomUserPrincipal}.
     * @return строковое представление JWT access токена.
     */
    public String generateAccessToken(Authentication authentication) {
        // Предполагается, что Principal является экземпляром CustomUserPrincipal
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();

        Map<String, Object> claims = new HashMap<>();
        // Важно: в claims для шлюза мы кладем 'id' и 'roles', как он ожидает.
        claims.put("id", userPrincipal.getId());
        claims.put("roles", userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return createToken(claims, userPrincipal.getUsername(), jwtExpirationMs);
    }

    /**
     * Вспомогательный метод для создания и подписи JWT.
     *
     * @param claims Полезная нагрузка токена (ID, роли).
     * @param subject "Тема" токена (username).
     * @param expirationMs Время жизни токена в миллисекундах.
     * @return Компактная строка JWT (JWS).
     */
    private String createToken(Map<String, Object> claims, String subject, int expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Создает криптографический ключ для подписи токена из секрета.
     *
     * @return Объект {@link Key}.
     */
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
