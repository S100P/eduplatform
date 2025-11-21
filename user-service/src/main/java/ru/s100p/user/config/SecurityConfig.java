package ru.s100p.user.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.s100p.user.security.CustomAccessDeniedHandler;
import ru.s100p.user.security.HeaderAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

import static ru.s100p.shared.constants.ApiConstants.*;
import static ru.s100p.shared.constants.SecurityConstants.ROLE_ADMIN;
import static ru.s100p.shared.constants.SecurityConstants.ROLE_INSTRUCTOR;

/**
 * Конфигурация безопасности для микросервиса `user-service`.
 * <p>
 * В текущей архитектуре этот сервис не выполняет первичную аутентификацию по JWT.
 * Эту задачу выполняет `api-gateway`. Данный сервис доверяет шлюзу, но проверяет
 * предоставленные им данные.
 * </p>
 * <p>
 * Основные принципы этой конфигурации:
 * <ul>
 *     <li><b>Stateless:</b> Сессии не используются, что типично для REST API и микросервисов.</li>
 *     <li><b>Делегирование аутентификации:</b> Сервис не работает напрямую с JWT. Вместо этого он ожидает
 *     специальные, подписанные шлюзом заголовки (`X-Auth-*`).</li>
 *     <li><b>Авторизация на основе заголовков:</b> Кастомный {@link HeaderAuthenticationFilter} проверяет
 *     эти заголовки и заполняет {@code SecurityContext}, что позволяет использовать стандартные
 *     механизмы авторизации Spring Security (`hasRole`, `@PreAuthorize`).</li>
 *     <li><b>Публичные эндпоинты:</b> Эндпоинты для регистрации, логина и восстановления пароля (`/api/v1/auth/**`)
 *     остаются публичными, так как именно они генерируют JWT, с которым клиент пойдет на шлюз.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Включает @PreAuthorize и другие аннотации на методах
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Обработчик ошибок доступа (403 Forbidden).
     */
    private final CustomAccessDeniedHandler accessDeniedHandler;

    /**
     * Кастомный фильтр, который проверяет подписанные заголовки от шлюза.
     * Это ключевой элемент для интеграции с `api-gateway`.
     */
    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    /**
     * Список URL, доступных без какой-либо аутентификации.
     * Сюда входят эндпоинты для управления аутентификацией, документация API и служебные эндпоинты.
     */
    private static final String[] PUBLIC_URLS = {
            "/.well-known/jwks.json", // Эндпоинт для получения публичных ключей //TODO еще раз почитать почему они могут быть публичными
            "/api/v1/auth/**",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error",
            "/favicon.ico"
    };

    /**
     * Определяет основную цепочку фильтров безопасности Spring Security.
     *
     * @param http Конфигуратор HttpSecurity.
     * @return Сконфигурированный {@link SecurityFilterChain}.
     * @throws Exception в случае ошибок конфигурации.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable) // Отключаем CSRF, так как используем stateless-подход
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Устанавливаем политику без сессий
                )
                // Настройка правил авторизации для запросов
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll() // Разрешаем доступ к публичным URL
                        .requestMatchers(INTERNAL_API_PREFIX + API_V1_ADMIN + "/**").hasRole(ROLE_ADMIN) // Требуем роль ADMIN
                        .requestMatchers(INTERNAL_API_PREFIX + API_V1_INSTRUCTOR + "/**").hasAnyRole(ROLE_INSTRUCTOR, ROLE_ADMIN) // Требуем роль INSTRUCTOR или ADMIN
                        .anyRequest().authenticated() // Все остальные запросы требуют аутентификации
                )
                // Настройка обработки исключений
                .exceptionHandling(ex -> ex
                                // Используем кастомный обработчик для ошибок доступа (403)
                                .accessDeniedHandler(accessDeniedHandler)
                        // AuthenticationEntryPoint здесь больше не нужен, так как первичная ошибка 401
                        // обрабатывается на уровне шлюза или нашим HeaderAuthenticationFilter.
                )
                // Добавляем наш кастомный фильтр ПЕРЕД стандартным фильтром Spring.
                // Он должен выполниться раньше, чтобы успеть заполнить SecurityContext.
                // Под капотом запускается headerAuthenticationFilter.doFilterInternal() (в том числе .shouldNotFilter, если есть), который проверяет заголовки. Spring делает это автоматические благодаря тому, что HeaderAuthenticationFilter наследуется от OncePerRequestFilter
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Бин для кодирования паролей. Используется при регистрации и логине.
     *
     * @return Реализация {@link PasswordEncoder} (BCrypt).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Бин {@link AuthenticationManager}, необходимый для процесса аутентификации (например, в AuthController).
     *
     * @param config Конфигурация аутентификации Spring.
     * @return Стандартный AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Бин для конфигурации CORS (Cross-Origin Resource Sharing).
     * Позволяет фронтенду, запущенному на другом домене/порту, обращаться к этому API.
     *
     * @return Источник конфигурации CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
