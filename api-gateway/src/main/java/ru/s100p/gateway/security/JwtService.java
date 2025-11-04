package ru.s100p.gateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Сервис для работы с JWT токенами в API Gateway
 * Отвечает за:
 * - Генерацию токенов после успешной аутентификации
 * - Валидацию токенов при каждом запросе
 * - Извлечение информации из токенов
 * - Обновление токенов
 */
@Slf4j
@Service
public class JwtService {

    // ========== НАСТРОЙКИ ==========

    /**
     * Секретный ключ для подписи токенов
     * ВАЖНО: В продакшене должен быть:
     * - Минимум 256 бит (32 символа) для HS256
     * - Минимум 512 бит (64 символа) для HS512
     * - Храниться в безопасном месте (HashiCorp Vault, AWS Secrets Manager)
     * - Периодически ротироваться
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Время жизни access токена (короткоживущий)
     * Обычно 15-60 минут
     */
    @Value("${jwt.expiration:900000}") // 15 минут по умолчанию
    private long accessTokenExpiration;

    /**
     * Время жизни refresh токена (долгоживущий)
     * Обычно 7-30 дней
     */
    @Value("${jwt.refresh.expiration:604800000}") // 7 дней по умолчанию
    private long refreshTokenExpiration;

    /**
     * Issuer - кто выпустил токен
     * Используется для дополнительной проверки
     */
    @Value("${jwt.issuer:edu-platform}")
    private String tokenIssuer;

    // ========== ГЕНЕРАЦИЯ ТОКЕНОВ ==========

    /**
     * Генерация Access Token после успешной аутентификации
     *
     * @param userId - ID пользователя
     * @param username - имя пользователя
     * @param email - email пользователя
     * @param roles - роли пользователя
     * @return JWT access token
     */
    public String generateAccessToken(Long userId, String username, String email, Set<String> roles) {
        log.debug("Генерация access токена для пользователя: {}", username);

        // Подготавливаем claims (данные, которые будут в токене)
        Map<String, Object> claims = new HashMap<>();

        // Стандартные claims
        claims.put(Claims.SUBJECT, username);           // sub - subject (кому принадлежит токен)
        claims.put(Claims.ISSUER, tokenIssuer);        // iss - issuer (кто выпустил)
        claims.put(Claims.ISSUED_AT, new Date());      // iat - issued at (когда выпущен)

        // Кастомные claims (наши данные)
        claims.put("userId", userId.toString());        // ID пользователя
        claims.put("email", email);                    // Email
        claims.put("roles", roles);                    // Роли
        claims.put("type", "access");                  // Тип токена

        // Вычисляем время истечения
        Date expirationDate = Date.from(
                Instant.now().plus(accessTokenExpiration, ChronoUnit.MILLIS)
        );
        claims.put(Claims.EXPIRATION, expirationDate);

        // Создаем и подписываем токен
        String token = Jwts.builder()
                .setClaims(claims)                     // Устанавливаем claims
                .signWith(getSigningKey())             // Подписываем секретным ключом
                .compact();                             // Собираем токен

        log.debug("Access токен сгенерирован, истекает: {}", expirationDate);

        return token;
    }

    /**
     * Генерация Refresh Token для обновления access токена
     * Содержит минимум информации для безопасности
     */
    public String generateRefreshToken(Long userId, String username) {
        log.debug("Генерация refresh токена для пользователя: {}", username);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("type", "refresh");
        claims.put("jti", UUID.randomUUID().toString()); // JWT ID - уникальный идентификатор токена

        Date expirationDate = Date.from(
                Instant.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS)
        );

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .signWith(getSigningKey())
                .compact();

        log.debug("Refresh токен сгенерирован, истекает: {}", expirationDate);

        return token;
    }

    // ========== ВАЛИДАЦИЯ ТОКЕНОВ ==========

    /**
     * Реактивная валидация токена для WebFlux
     * Используется в фильтрах API Gateway
     *
     * @param token - JWT токен
     * @return Mono<Boolean> - true если токен валиден
     */
    public Mono<Boolean> validateToken(String token) {
        return Mono.fromCallable(() -> {
                    try {
                        // Парсим и валидируем токен
                        Jws<Claims> claimsJws = Jwts.parserBuilder()
                                .setSigningKey(getSigningKey())        // Проверяем подпись
                                .requireIssuer(tokenIssuer)            // Проверяем issuer
                                .build()
                                .parseClaimsJws(token);

                        Claims claims = claimsJws.getBody();

                        // Дополнительные проверки

                        // 1. Проверяем тип токена
                        String tokenType = (String) claims.get("type");
                        if (!"access".equals(tokenType)) {
                            log.warn("Неверный тип токена: {}", tokenType);
                            return false;
                        }

                        // 2. Проверяем, не истек ли токен (это уже проверяется автоматически, но для ясности)
                        Date expiration = claims.getExpiration();
                        if (expiration.before(new Date())) {
                            log.warn("Токен истек: {}", expiration);
                            return false;
                        }

                        // 3. Можно добавить проверку на blacklist
                        if (isTokenBlacklisted(token)) {
                            log.warn("Токен в черном списке");
                            return false;
                        }

                        log.debug("Токен валиден для пользователя: {}", claims.getSubject());
                        return true;

                    } catch (ExpiredJwtException e) {
                        log.warn("JWT токен истек: {}", e.getMessage());
                        return false;
                    } catch (UnsupportedJwtException e) {
                        log.error("Неподдерживаемый JWT токен: {}", e.getMessage());
                        return false;
                    } catch (MalformedJwtException e) {
                        log.error("Некорректный JWT токен: {}", e.getMessage());
                        return false;
                    } catch (SignatureException e) {
                        log.error("Неверная подпись JWT токена: {}", e.getMessage());
                        return false;
                    } catch (IllegalArgumentException e) {
                        log.error("JWT claims пусты: {}", e.getMessage());
                        return false;
                    }
                })
                .doOnError(error -> log.error("Ошибка при валидации токена", error))
                .onErrorReturn(false);
    }

    // ========== ИЗВЛЕЧЕНИЕ ДАННЫХ ИЗ ТОКЕНА ==========

    /**
     * Извлечение всех claims из токена
     * Claims - это данные, хранящиеся в токене
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Извлечение User ID из токена
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return (String) claims.get("userId");
        } catch (Exception e) {
            log.error("Не удалось извлечь userId из токена", e);
            return null;
        }
    }

    /**
     * Извлечение username из токена
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Не удалось извлечь username из токена", e);
            return null;
        }
    }

    /**
     * Извлечение email из токена
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return (String) claims.get("email");
        } catch (Exception e) {
            log.error("Не удалось извлечь email из токена", e);
            return null;
        }
    }

    /**
     * Извлечение ролей из токена
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return (List<String>) claims.get("roles");
        } catch (Exception e) {
            log.error("Не удалось извлечь роли из токена", e);
            return Collections.emptyList();
        }
    }

    /**
     * Проверка, истек ли токен
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Получение времени истечения токена
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getExpiration();
    }

    // ========== ОБНОВЛЕНИЕ ТОКЕНОВ ==========

    /**
     * Обновление access токена с помощью refresh токена
     */
    public Mono<TokenPair> refreshAccessToken(String refreshToken) {
        return Mono.fromCallable(() -> {
            try {
                // Валидируем refresh токен
                Claims claims = extractAllClaims(refreshToken);

                // Проверяем тип токена
                String tokenType = (String) claims.get("type");
                if (!"refresh".equals(tokenType)) {
                    throw new IllegalArgumentException("Неверный тип токена для обновления");
                }

                // Извлекаем данные
                String userId = (String) claims.get("userId");
                String username = claims.getSubject();

                // Здесь нужно получить актуальные данные пользователя из User Service
                // Для примера используем заглушку
                String email = "user@example.com";
                Set<String> roles = Set.of("ROLE_STUDENT");

                // Генерируем новую пару токенов
                String newAccessToken = generateAccessToken(
                        Long.parseLong(userId), username, email, roles
                );
                String newRefreshToken = generateRefreshToken(
                        Long.parseLong(userId), username
                );

                log.info("Токены обновлены для пользователя: {}", username);

                return new TokenPair(newAccessToken, newRefreshToken);

            } catch (Exception e) {
                log.error("Ошибка при обновлении токена", e);
                throw new RuntimeException("Не удалось обновить токен", e);
            }
        });
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Получение ключа для подписи/проверки токенов
     * Конвертирует строковый секрет в криптографический ключ
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Проверка токена в черном списке (заглушка)
     * В реальном приложении нужно проверять в Redis
     */
    private boolean isTokenBlacklisted(String token) {
        // TODO: Реализовать проверку в Redis
        // return redisTemplate.hasKey("blacklist:" + token);
        return false;
    }

    /**
     * Добавление токена в черный список при logout
     */
    public Mono<Void> blacklistToken(String token) {
        return Mono.fromRunnable(() -> {
            try {
                Claims claims = extractAllClaims(token);
                Date expiration = claims.getExpiration();

                // Сохраняем в Redis с TTL = времени до истечения токена
                long ttl = expiration.getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    // TODO: сохранить в Redis
                    log.info("Токен добавлен в черный список с TTL: {} мс", ttl);
                }
            } catch (Exception e) {
                log.error("Ошибка при добавлении токена в черный список", e);
            }
        });
    }

    /**
     * DTO для пары токенов
     */
    public record TokenPair(String accessToken, String refreshToken) {}
}
