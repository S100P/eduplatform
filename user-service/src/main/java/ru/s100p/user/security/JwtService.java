package ru.s100p.user.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для создания JSON Web Tokens (JWT) с использованием асимметричного шифрования (RSA).
 * <p>
 * Этот сервис является "издателем" токенов (Token Issuer). Он использует <b>приватный ключ</b>
 * для подписи токенов по алгоритму RS256.
 * </p>
 * <p>
 * Валидация токенов происходит в `api-gateway` с использованием соответствующего <b>публичного ключа</b>,
 * что исключает необходимость хранения приватного ключа где-либо, кроме этого сервиса.
 * </p>
 */
@Slf4j
@Component
public class JwtService {

    @Value("${jwt.rsa.private-key}")
    private String privateKeyString;

    @Value("${jwt.expiration}") // 1 час в миллисекундах
    private int jwtExpirationMs;

    private PrivateKey privateKey;

    /**
     * Метод инициализации, который преобразует строковое представление приватного ключа
     * из application.yml в объект {@link PrivateKey}.
     */
    @PostConstruct
    protected void init() {
        try {
            log.info("Загрузка приватного RSA ключа для подписи JWT...");
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(keySpec);
            log.info("Приватный RSA ключ успешно загружен.");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("Ошибка при загрузке приватного RSA ключа!", e);
            throw new RuntimeException("Не удалось загрузить приватный ключ для подписи JWT", e);
        }
    }

    /**
     * Генерация access токена для аутентифицированного пользователя.
     *
     * @param authentication объект аутентификации Spring Security.
     * @return строковое представление JWT access токена, подписанного по алгоритму RS256.
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

        return createToken(claims, userPrincipal.getUsername());
    }

    /**
     * Вспомогательный метод для создания и подписи JWT c использованием приватного ключа.
     *
     * @param claims Полезная нагрузка токена (ID, роли).
     * @param subject "Тема" токена (username).
     * @return Компактная строка JWT (JWS).
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                // Используем алгоритм RS256 и приватный ключ для подписи
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}
