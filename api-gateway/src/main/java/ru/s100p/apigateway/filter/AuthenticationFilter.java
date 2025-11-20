package ru.s100p.apigateway.filter;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.s100p.apigateway.util.JwtUtil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Основной фильтр аутентификации для Spring Cloud Gateway.
 * <p>
 * Этот фильтр является центральным элементом безопасности на уровне шлюза. Он выполняет следующие задачи:
 * <ol>
 *     <li><b>Проверка JWT:</b> Валидирует JWT, полученный от клиента, используя {@link JwtUtil}.</li>
 *     <li><b>Экранирование заголовков:</b> Удаляет любые входящие заголовки, которые могут конфликтовать с внутренней логикой авторизации (например, `X-Auth-*`, `X-User-*`). Это предотвращает подделку данных клиентом.</li>
 *     <li><b>Создание внутреннего токена:</b> Генерирует набор подписанных заголовков (`X-Auth-*`), которые содержат информацию о пользователе (ID, роли), срок действия и HMAC-подпись.</li>
 *     <li><b>Обогащение запроса:</b> Добавляет эти доверенные заголовки в запрос перед его отправкой во внутренние микросервисы.</li>
 * </ol>
 * <p>
 * Этот подход реализует принцип "Zero Trust" (нулевого доверия) между шлюзом и внутренними сервисами.
 * Микросервисы не доверяют слепо информации, а проверяют подпись, гарантируя, что данные были созданы именно шлюзом.
 * </p>
 * <p>
 * Этот фильтр используется фабрикой {@link JwtAuthenticationGatewayFilterFactory}, что позволяет применять его декларативно в `application.yml`.
 * </p>
 */
@RefreshScope // Позволяет обновлять бин (например, при изменении jwt.internal-secret в Spring Cloud Config) без перезапуска приложения.
@Component
public class AuthenticationFilter implements GatewayFilter {

    private final JwtUtil jwtUtil;
    private final SecretKeySpec secretKeySpec;

    /**
     * Конструктор для внедрения зависимостей.
     *
     * @param jwtUtil Утилита для работы с JWT, полученными от клиента.
     * @param internalSecret Секретный ключ для создания HMAC-подписи внутренних заголовков.
     *                       Этот ключ должен быть известен только шлюзу и внутренним сервисам.
     *                       Инжектируется из `application.yml` (jwt.internal-secret).
     */
    public AuthenticationFilter(JwtUtil jwtUtil, @Value("${jwt.internal-secret}") String internalSecret) {
        this.jwtUtil = jwtUtil;
        // Создаем объект ключа для HMAC-SHA256 один раз для производительности.
        this.secretKeySpec = new SecretKeySpec(internalSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * Главный метод фильтра, который обрабатывает каждый запрос, проходящий через шлюз.
     *
     * @param exchange Объект, содержащий запрос, ответ и другие данные контекста.
     * @param chain Цепочка фильтров, через которую должен пройти запрос.
     * @return {@link Mono<Void>}, сигнализирующий о завершении обработки.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Проверяем наличие заголовка Authorization.
        if (isAuthMissing(request)) {
            return this.onError(exchange, "Authorization header is missing in request", HttpStatus.UNAUTHORIZED);
        }

        final String token = getAuthHeader(request);

        // 2. Валидируем JWT с помощью JwtUtil.
        if (jwtUtil.isInvalid(token)) {
            return this.onError(exchange, "Authorization header is invalid", HttpStatus.UNAUTHORIZED);
        }

        ServerWebExchange modifiedExchange;
        try {
            // 3. Извлекаем данные из JWT.
            Claims claims = jwtUtil.getAllClaimsFromToken(token);
            String userId = String.valueOf(claims.get("id"));
            String userRoles = String.valueOf(claims.get("roles"));
            String expiration = String.valueOf(claims.getExpiration().getTime());

            // 4. Готовим данные для подписи.
            String dataToSign = String.join(":", userId, userRoles, expiration);
            String signature = createSignature(dataToSign);

            // 5. Экранируем и обогащаем заголовки.
            // Создаем новый запрос, мутируя исходный.
            ServerHttpRequest newRequest = exchange.getRequest().mutate()
                    .headers(httpHeaders -> {
                        // 5.1. Удаляем потенциально поддельные заголовки от клиента.
                        httpHeaders.remove("X-Auth-User");
                        httpHeaders.remove("X-Auth-Roles");
                        httpHeaders.remove("X-Auth-Exp");
                        httpHeaders.remove("X-Auth-Signature");
                        httpHeaders.remove("X-User-Id"); // Также удаляем заголовки от старой реализации
                        httpHeaders.remove("X-User-Roles");

                        // 5.2. Добавляем наши собственные, доверенные заголовки.
                        // Используем set() вместо add(), чтобы гарантировать единственное значение.
                        httpHeaders.set("X-Auth-User", userId);
                        httpHeaders.set("X-Auth-Roles", userRoles);
                        httpHeaders.set("X-Auth-Exp", expiration);
                        httpHeaders.set("X-Auth-Signature", signature);
                    })
                    .build();

            // Создаем новый ServerWebExchange с измененным запросом.
            modifiedExchange = exchange.mutate().request(newRequest).build();

        } catch (Exception e) {
            // В реальном приложении здесь должно быть логирование ошибки.
            return this.onError(exchange, "Failed to create internal signature", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 6. Передаем управление следующему фильтру в цепочке с уже измененным exchange.
        return chain.filter(modifiedExchange);
    }

    /**
     * Вспомогательный метод для формирования ответа с ошибкой.
     */
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    /**
     * Извлекает токен из заголовка "Authorization: Bearer ...".
     */
    private String getAuthHeader(ServerHttpRequest request) {
        return request.getHeaders().getOrEmpty("Authorization").get(0).substring(7);
    }

    /**
     * Проверяет наличие и корректность формата заголовка Authorization.
     */
    private boolean isAuthMissing(ServerHttpRequest request) {
        if (!request.getHeaders().containsKey("Authorization")) {
            return true;
        }
        return !request.getHeaders().getOrEmpty("Authorization").get(0).startsWith("Bearer ");
    }

    /**
     * Создает HMAC-SHA256 подпись для предоставленных данных.
     *
     * @param data Строка данных для подписи.
     * @return Строка с подписью в формате Base64.
     */
    private String createSignature(String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}
