package ru.s100p.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.util.List;

/**
 * Этот класс представляет собой веб-фильтр, который перехватывает все входящие запросы к API Gateway.
 * Его основная задача - проверить наличие и валидность JWT (JSON Web Token) в заголовке Authorization.
 * Если токен валиден, фильтр извлекает из него информацию о пользователе и добавляет ее
 * в запрос в виде специальных заголовков (X-User-Id, X-Username, X-User-Roles).
 * Это позволяет нижестоящим микросервисам доверять этим заголовкам и использовать информацию о пользователе,
 * не проводя повторную валидацию токена.
 *
 * @see WebFilter интерфейс для создания кастомных фильтров в Spring WebFlux.
 * @see Component делает этот класс Spring-бином, чтобы его можно было автоматически обнаружить и внедрить.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    // Сервис для работы с JWT: генерация, валидация, извлечение данных.
    private final JwtService jwtService;

    /**
     * Основной метод фильтра, который обрабатывает каждый входящий запрос.
     *
     * @param exchange Объект, содержащий информацию о текущем HTTP-запросе и ответе.
     * @param chain    Цепочка фильтров. Вызов chain.filter(exchange) передает управление следующему фильтру в цепочке.
     * @return Mono<Void>, указывающий на завершение асинхронной обработки.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Пытаемся извлечь JWT токен из заголовка Authorization.
        String token = extractToken(request);

        // Если токена нет, просто передаем управление следующему фильтру в цепочке.
        // Запрос будет считаться анонимным.
        if (token == null) {
            return chain.filter(exchange);
        }

        // 2. Валидируем извлеченный токен. Это асинхронная операция, возвращающая Mono<Boolean>.
        return jwtService.validateToken(token)
                .flatMap(isValid -> {
                    // 3. Если токен валиден...
                    if (isValid) {
                        // Извлекаем данные пользователя (ID, имя, роли) из токена.
                        String userId = jwtService.getUserIdFromToken(token);
                        String username = jwtService.getUsernameFromToken(token);
                        List<String> roles = jwtService.getRolesFromToken(token);

                        // 4. Модифицируем исходный запрос, добавляя в него кастомные заголовки.
                        // Эти заголовки будут использоваться нижестоящими сервисами.
                        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                                .header("X-User-Id", userId)
                                .header("X-Username", username)
                                .header("X-User-Roles", String.join(",", roles))
                                .build();

                        // Создаем новый `ServerWebExchange` с нашим измененным запросом.
                        ServerWebExchange modifiedExchange = exchange.mutate()
                                .request(modifiedRequest)
                                .build();

                        log.debug("JWT валиден. User: {}, Roles: {}", username, roles);

                        // 5. Создаем объект аутентификации для Spring Security.
                        return createAuthentication(userId, username, roles)
                                .flatMap(auth ->
                                    // 6. Продолжаем цепочку фильтров с измененным запросом
                                    // и помещаем объект аутентификации в контекст безопасности.
                                    // Теперь Spring Security знает, что пользователь аутентифицирован.
                                    chain.filter(modifiedExchange)
                                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                                );
                    } else {
                        // Если токен невалиден, логируем предупреждение и продолжаем цепочку без аутентификации.
                        log.warn("Невалидный JWT токен");
                        return chain.filter(exchange);
                    }
                })
                // Обрабатываем возможные ошибки во время валидации токена (например, если токен некорректного формата).
                .onErrorResume(error -> {
                    log.error("Ошибка при валидации токена: {}", error.getMessage());
                    return chain.filter(exchange); // В случае ошибки также продолжаем без аутентификации.
                });
    }

    /**
     * Вспомогательный метод для извлечения токена из заголовка Authorization.
     * Ожидается, что заголовок будет иметь формат "Bearer <token>".
     *
     * @param request Текущий HTTP-запрос.
     * @return Строка с токеном или null, если заголовок отсутствует или имеет неверный формат.
     */
    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            // Возвращаем подстроку, которая идет после "Bearer "
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Создает объект аутентификации, который будет помещен в контекст Spring Security.
     *
     * @param userId   ID пользователя.
     * @param username Имя пользователя.
     * @param roles    Список ролей пользователя.
     * @return Mono с объектом JwtAuthentication.
     */
    private Mono<JwtAuthentication> createAuthentication(String userId, String username, List<String> roles) {
        // Mono.just() создает реактивный поток с одним элементом.
        return Mono.just(new JwtAuthentication(userId, username, roles));
    }
}
