package ru.s100p.user.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.s100p.user.security.CustomAccessDeniedHandler;
import ru.s100p.user.security.JwtAuthenticationEntryPoint;
import ru.s100p.user.security.JwtAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

import static ru.s100p.shared.constants.ApiConstants.API_V1_ADMIN;
import static ru.s100p.shared.constants.ApiConstants.API_V1_INSTRUCTOR;
import static ru.s100p.shared.constants.SecurityConstants.ROLE_ADMIN;
import static ru.s100p.shared.constants.SecurityConstants.ROLE_INSTRUCTOR;

@Configuration
@EnableWebSecurity // Включает поддержку веб-безопасности Spring
@EnableMethodSecurity(prePostEnabled = true) // Включает поддержку безопасности на уровне методов (например, @PreAuthorize)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint; // Точка входа для обработки ошибок аутентификации JWT
    private final JwtAuthenticationFilter jwtAuthenticationFilter; // Фильтр для аутентификации с использованием JWT
    private final UserDetailsService userDetailsService; // Сервис для загрузки пользовательских данных
    private final CustomAccessDeniedHandler accessDeniedHandler;

    // Массив публичных URL-адресов, доступных без аутентификации
    private static final String[] PUBLIC_URLS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    // Определяет цепочку фильтров безопасности
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Настройка CORS (Cross-Origin Resource Sharing). Эта настройка критически важна для того, чтобы ваше фронтенд-приложение (например, на React или Angular) могло успешно общаться с вашим Java-бэкендом, когда они запущены на разных портах или доменах. Без нее браузер будет блокировать все запросы от фронтенда к бэкенду.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Отключение защиты от CSRF (Cross-Site Request Forgery), так как используется JWT
                .csrf(AbstractHttpConfigurer::disable)
                // Настройка правил авторизации для HTTP-запросов
                .authorizeHttpRequests(auth -> auth
                        // Разрешает доступ к публичным URL-адресам для всех
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        // Требует роль "ADMIN" для доступа к URL-адресам, начинающимся с "/api/v1/admin/**"
                        .requestMatchers(API_V1_ADMIN + "/**").hasRole(ROLE_ADMIN)
                        // Требует роль "INSTRUCTOR" или "ADMIN" для доступа к URL-адресам, начинающимся с "/api/v1/instructor/**"
                        .requestMatchers(API_V1_INSTRUCTOR + "/**").hasAnyRole(ROLE_INSTRUCTOR, ROLE_ADMIN)
                        // Требует аутентификацию для всех остальных запросов
                        .anyRequest().authenticated()
                )
                // Настройка обработки исключений
                .exceptionHandling(ex -> ex
                        // Устанавливает точку входа для обработки ошибок аутентификации
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // Настройка управления сессиями
                .sessionManagement(session -> session
                        // Устанавливает политику создания сессий на STATELESS (без сессий на сервере)
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) //TODO можно ли оставить стейтфул и использовать вместе с JWT?
                )
                // Устанавливает провайдер аутентификации. В современных версиях эта строка уже не нужна в конфиге, так как это делается автоматически. Эту строку можно удалить.
                .authenticationProvider(authenticationProvider())
                // Добавляет JWT-фильтр перед стандартным фильтром аутентификации по имени пользователя и паролю
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Собирает и возвращает конфигурацию HttpSecurity
        return http.build();
    }

    // Определяет бин для кодирования паролей
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Использует BCrypt с силой хеширования 12
        return new BCryptPasswordEncoder(12);
    }

    /* Этот метод настраивает основной механизм для проверки подлинности (аутентификации) пользователей. Кратко: Этот бин связывает логику аутентификации Spring Security с вашими данными о пользователях и стратегией хеширования паролей.
    *
    * В современных версиях Spring Security вам больше не нужно вручную создавать DaoAuthenticationProvider и передавать его в HttpSecurity. Spring Boot сделает это за вас автоматически, если найдет в контексте приложения бины UserDetailsService и PasswordEncoder.
    *
    * Поэтому можно удалить метод authenticationProvider(). Он больше не нужен.
    * Удалить строку .authenticationProvider(authenticationProvider()) из конфигурации securityFilterChain.
    *
    * Но если будет несколько реализаций UserDetailService, то нужно
    * */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        /* Создает DAO (Data Access Object) провайдер аутентификации.  Это стандартная реализация AuthenticationProvider в Spring Security. Его задача — аутентифицировать пользователя на основе логина и пароля.*/
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        /* Устанавливает сервис для загрузки пользовательских данных. Это самая важная часть. Она "подключает" вашу кастомную логику для поиска пользователя. userDetailsService — это интерфейс, Spring автоматически находит его нашу реализацию CustomUserDeteilservice, который уже в свою очередь знает, как получить данные пользователя (логин, хеш пароля, роли/права) из базы данных или любого другого источника благодаря переопределенному методу loadUserByUsername.*/
        authProvider.setUserDetailsService(userDetailsService);
        /* Устанавливает кодировщик паролей. authProvider.setPassordEncoder(passwordEncoder()): Хранить пароли в открытом виде — плохая практика. Эта строка устанавливает алгоритм хеширования паролей (например, BCrypt). Когда пользователь пытается войти, этот провайдер берет введенный им пароль, хеширует его с помощью passwordEncoder и сравнивает результат с хешем, который userDetailsService получил из базы данных.*/
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Определяет бин менеджера аутентификации. Этот бин делает главный "движок" аутентификации доступным для всего вашего приложения.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        /* Получает менеджер аутентификации из конфигурации. В современных версиях Spring Security это рекомендуемый способ сделать AuthenticationManager доступным в виде бина. Spring Boot автоматически создает и настраивает AuthenticationManager, который знает обо всех бинах AuthenticationProvider (включая тот, что мы определили выше). Этот метод просто получает уже настроенный менеджер и делает его доступным для внедрения в других частях вашего приложения.*/
        return config.getAuthenticationManager();
    }

    /* Этот метод настраивает CORS (Cross-Origin Resource Sharing). Это механизм безопасности браузера, который по умолчанию запрещает веб-странице делать запросы к другому домену (отличному от того, с которого была загружена сама страница). Эта конфигурация необходима, если ваш фронтенд (например, приложение на React/Angular/Vue) работает на другом сервере или порту, отличном от вашего бэкенд API.*/
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Создает объект конфигурации CORS
        CorsConfiguration configuration = new CorsConfiguration();
        /* Устанавливает "белый список" URL-адресов, которым разрешено делать запросы к вашему бэкенду. Здесь разрешены запросы от приложений, запущенных на localhost:3000 (часто для React) и localhost:4200 (часто для Angular).*/
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:4200"));
        // Устанавливает какие HTTP-методы (GET, POST и т.д.) разрешены с этих адресов.
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // Разрешает все заголовки
        configuration.setAllowedHeaders(Arrays.asList("*"));
        /* Разрешает отправку учетных данных (например, cookie). Критически важная настройка. Она позволяет браузеру включать в междоменные запросы куки (cookies) и токены аутентификации(например, JWT в заголовке Authorization).*/
        configuration.setAllowCredentials(true);
        /* Устанавливает максимальное время кеширования предварительных (pre-flight) запросов. Для некоторых запросов (например, PUT или DELETE) браузеры сначала отправляют "предварительный" запрос OPTIONS, чтобы проверить, разрешен ли основной запрос. Эта настройка говорит браузеру кешировать это разрешение на 3600 секунд (1 час), чтобы не отправлять OPTIONS каждый раз.*/
        configuration.setMaxAge(3600L);

        // Создает источник конфигурации CORS на основе URL
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Применяет только что определенные правила CORS ко всем эндпоинтам (/**) вашего API.
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
