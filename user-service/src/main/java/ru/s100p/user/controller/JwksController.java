package ru.s100p.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.s100p.user.service.JwksService;

import java.util.Map;

/**
 * Контроллер для предоставления публичных ключей в формате JWKS (JSON Web Key Set).
 * <p>
 * Этот эндпоинт используется внешними системами (например, API Gateway) для получения
 * публичного ключа, необходимого для валидации JWT, подписанных этим сервисом.
 * </p>
 * <p>
 * Соответствует стандарту OpenID Connect для обнаружения публичных ключей.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/.well-known") // Стандартный путь для JWKS
@RequiredArgsConstructor
public class JwksController {

    private final JwksService jwksService;

    /**
     * Возвращает набор публичных ключей в формате JWKS.
     * @return JSON-объект, содержащий массив публичных ключей.
     */
    @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getJwkSet() {
        log.debug("Запрос JWKS");
        return ResponseEntity.ok(jwksService.getJwkSet());
    }
}
