package ru.s100p.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Конфигурация безопасности для API Gateway.
 * <p>
 * Поскольку API Gateway использует Spring WebFlux (он реактивный),
 * мы используем {@link EnableWebFluxSecurity} вместо {@code @EnableWebSecurity}.
 * </p>
 * <p>
 * Основная задача этой конфигурации — отключить стандартную защиту Spring Security,
 * которая включается по умолчанию и блокирует все эндпоинты. Мы делегируем всю
 * логику аутентификации нашим кастомным фильтрам ({@link ru.s100p.apigateway.filter.JwtAuthenticationFilter}),
 * которые применяются на уровне маршрутов в `application.yml`.
 * </p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Определяет цепочку фильтров безопасности для всего шлюза.
     *
     * @param http Конфигуратор ServerHttpSecurity для реактивного стека.
     * @return Сконфигурированная цепочка фильтров.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // 1. Отключаем CSRF (Cross-Site Request Forgery), так как мы используем stateless-аутентификацию (JWT).
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // 2. Отключаем стандартные механизмы входа.
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                // 3. Настраиваем правила авторизации.
                .authorizeExchange(exchange -> exchange
                        // 3.1. Разрешаем ВСЕ запросы на этом уровне.
                        // Это позволяет запросам дойти до фильтров шлюза (например, JwtAuthenticationFilter),
                        // которые уже сами примут решение об аутентификации.
                        .anyExchange().permitAll()
                )
                .build();
    }
}
