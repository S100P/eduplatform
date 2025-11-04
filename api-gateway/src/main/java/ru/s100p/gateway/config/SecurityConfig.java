package ru.s100p.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import ru.s100p.gateway.security.JwtAuthenticationFilter;
import ru.s100p.gateway.security.JwtAuthenticationManager;

import java.util.Arrays;
import java.util.List;

/**
 * Этот класс является центральной точкой конфигурации безопасности для всего API Gateway.
 * Он использует Spring Security для защиты эндпоинтов.
 * Все проверки JWT (JSON Web Token) происходят здесь, что избавляет от необходимости
 * дублировать логику аутентификации и авторизации в каждом отдельном микросервисе.
 *
 * @see EnableWebFluxSecurity используется для включения поддержки безопасности в неблокирующем стеке WebFlux.
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Менеджер аутентификации, который проверяет валидность JWT токена.
    private final JwtAuthenticationManager jwtAuthenticationManager;
    // Фильтр, который извлекает JWT токен из заголовка Authorization.
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * PUBLIC_PATHS - это массив строк, который определяет публичные эндпоинты.
     * Эти эндпоинты доступны всем пользователям без аутентификации.
     * Сюда обычно входят страницы регистрации, входа, восстановления пароля и т.д.
     */
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/api/v1/courses/public/**",
            "/actuator/health", // Эндпоинт для проверки состояния работоспособности сервиса
            "/swagger-ui/**",   // Эндпоинты для Swagger UI, инструмента для документирования и тестирования API
            "/v3/api-docs/**"   // Эндпоинты для OpenAPI спецификации
    };

    /**
     * Этот метод конфигурирует цепочку фильтров безопасности.
     * Именно здесь определяются правила доступа к различным эндпоинтам.
     *
     * @param http объект ServerHttpSecurity для конфигурации безопасности.
     * @return сконфигурированная цепочка фильтров безопасности.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // Конфигурация CORS (Cross-Origin Resource Sharing)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Отключаем CSRF (Cross-Site Request Forgery) защиту.
                // В REST API, использующих токены, CSRF-атаки менее вероятны.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Начинаем описание правил авторизации для эндпоинтов.
                .authorizeExchange(exchanges -> exchanges
                        // Разрешаем доступ ко всем публичным эндпоинтам, определенным в PUBLIC_PATHS.
                        .pathMatchers(PUBLIC_PATHS).permitAll()

                        // Эндпоинты, доступные только администраторам.
                        .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Эндпоинты, доступные инструкторам и администраторам.
                        .pathMatchers("/api/v1/instructor/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .pathMatchers("/api/v1/courses/create").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .pathMatchers("/api/v1/courses/*/edit").hasAnyRole("INSTRUCTOR", "ADMIN")

                        // Эндпоинты, доступные только студентам.
                        .pathMatchers("/api/v1/enrollments/**").hasRole("STUDENT")
                        .pathMatchers("/api/v1/courses/*/enroll").hasRole("STUDENT")

                        // Все остальные запросы требуют аутентификации.
                        .anyExchange().authenticated()
                )
                // Устанавливаем наш кастомный менеджер аутентификации.
                .authenticationManager(jwtAuthenticationManager)
                // Добавляем наш кастомный JWT фильтр в цепочку фильтров Spring Security.
                // Он будет срабатывать на этапе аутентификации.
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                // Собираем и возвращаем сконфигурированный объект SecurityWebFilterChain.
                .build();
    }

    /**
     * Этот метод создает конфигурацию CORS (Cross-Origin Resource Sharing).
     * CORS - это механизм, который позволяет веб-странице запрашивать ресурсы с другого домена.
     *
     * @return источник конфигурации CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Указываем, с каких доменов разрешены запросы (например, ваш фронтенд-сервер).
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:4200"));
        // Указываем разрешенные HTTP-методы.
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // Разрешаем все заголовки в запросах.
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // Разрешаем отправку cookie и других учетных данных.
        configuration.setAllowCredentials(true);
        // Устанавливаем максимальное время, на которое может быть закеширован результат pre-flight запроса.
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Применяем данную конфигурацию ко всем путям.
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
