package ru.s100p.user.service;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

/**
 * Сервис для предоставления публичных ключей в формате JWKS (JSON Web Key Set).
 */
@Slf4j
@Service
public class JwksService {

    @Value("${jwt.rsa.public-key}")
    private String publicKeyString;

    private JWK jwk;

    /**
     * Инициализация сервиса: загрузка и преобразование публичного ключа.
     */
    @PostConstruct
    public void init() {
        try {
            log.info("Загрузка публичного RSA ключа для JWKS...");
            RSAPublicKey publicKey = loadPublicKey();
            // Создаем JWK (JSON Web Key) из публичного RSA ключа
            jwk = new RSAKey.Builder(publicKey)
                    .keyID("user-service-key-1") // Уникальный идентификатор ключа
                    .build();
            log.info("Публичный ключ успешно преобразован в JWK.");
        } catch (Exception e) {
            log.error("Ошибка при создании JWK из публичного ключа!", e);
            throw new RuntimeException("Не удалось создать JWK", e);
        }
    }

    /**
     * Возвращает набор ключей в формате, совместимом с JWKS.
     * @return Map, представляющая JSON объект JWKS.
     */
    public Map<String, Object> getJwkSet() {
        return Collections.singletonMap("keys", Collections.singletonList(jwk.toJSONObject()));
    }

    /**
     * Загружает и преобразует публичный ключ из строкового представления в application.yml.
     */
    private RSAPublicKey loadPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyString);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }
}
