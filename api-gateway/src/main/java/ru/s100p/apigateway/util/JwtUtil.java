package ru.s100p.apigateway.util;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Date;
import java.util.Map;

/**
 * Утилитарный класс для валидации JWT с использованием публичных ключей из JWKS-эндпоинта.
 * <p>
 * Этот класс отвечает за проверку подлинности и срока действия JWT, полученных от клиентов.
 * Он не хранит никаких секретов, а вместо этого динамически загружает публичные ключи
 * от сервиса-издателя (user-service) по указанному `jwks-uri`.
 * </p>
 */
@Slf4j
@Component
public class JwtUtil {

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    public JwtUtil(@Value("${jwt.jwks-uri}") String jwksUri) {
        this.jwtProcessor = new DefaultJWTProcessor<>();

        try {
            // Создаем и настраиваем retriever с таймаутами
            DefaultResourceRetriever resourceRetriever = new DefaultResourceRetriever();
            resourceRetriever.setConnectTimeout(1000);
            resourceRetriever.setReadTimeout(1000);

            // Создаем источник ключей, который будет обращаться к нашему user-service
            // Используем современный JWKSourceBuilder для настройки кэширования и таймаутов
            JWKSource<SecurityContext> keySource = JWKSourceBuilder
                    .create(URI.create(jwksUri).toURL(), resourceRetriever)
                    .cache(true)
                    .build();

            // Настраиваем процессор для использования этих ключей
            // (библиотека сама найдет нужный ключ для проверки подписи)
            com.nimbusds.jose.proc.JWSKeySelector<SecurityContext> keySelector =
                    new com.nimbusds.jose.proc.JWSVerificationKeySelector<>(com.nimbusds.jose.JWSAlgorithm.RS256, keySource);
            jwtProcessor.setJWSKeySelector(keySelector);
        } catch (Exception e) {
            log.error("Ошибка инициализации JWKS источника: {}", jwksUri, e);
            throw new RuntimeException("Не удалось инициализировать JWKS-источник", e);
        }
    }

    /**
     * Извлекает все "claims" из JWT после его валидации.
     * Claims — это набор утверждений о пользователе (например, ID, роли, срок действия).
     * <p>
     * Этот метод использует библиотеку Nimbus для валидации токена.
     * Он проверяет подпись с использованием публичного ключа из JWKS и базовые claims (например, срок действия).
     * </p>
     *
     * @param token JWT, полученный от клиента.
     * @return Объект {@link Claims}, совместимый с библиотекой JJWT.
     * @throws Exception если токен невалиден.
     */
    public Claims getAllClaimsFromToken(String token) throws Exception {
        // Валидируем токен и получаем его claims с помощью Nimbus
        JWTClaimsSet claimsSet = jwtProcessor.process(token, null);

        // Получаем все claims в виде стандартной Java Map
        Map<String, Object> claimsMap = claimsSet.getClaims();

        // Создаем объект Claims из библиотеки JJWT напрямую из этой карты.
        // Это чистый и правильный способ, без ручного маппинга.
        return Jwts.claims(claimsMap);
    }

    /**
     * Проверяет, является ли токен невалидным.
     *
     * @param token JWT для проверки.
     * @return {@code true}, если токен невалиден, иначе {@code false}.
     */
    public boolean isInvalid(String token) {
        try {
            JWTClaimsSet claimsSet = jwtProcessor.process(token, null);
            // Дополнительная проверка, что срок действия не истек
            if (claimsSet.getExpirationTime().before(new Date())) {
                log.warn("JWT token is expired");
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("Ошибка валидации JWT: {}", e.getMessage());
            return true;
        }
    }
}
