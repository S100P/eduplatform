package ru.s100p.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Фильтр для проверки JWT токенов в каждом запросе
 * Выполняется один раз на запрос (OncePerRequestFilter)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Основной метод фильтрации
     * Проверяет наличие и валидность JWT токена в заголовке Authorization

     @AuthService.authenticate — это "дверь" для входа. Вы используете его один раз, чтобы получить "ключ" (JWT-токен).

     @JwtAuthenticationFilter.doFilterInternal — это "охранник" на каждом последующем запросе. Он проверяет ваш "ключ" (JWT-токен), чтобы убедиться, что у вас есть доступ к запрашиваемым ресурсам.
     */
    // Задача этого фильтра (JwtAuthenticationFilter): Его единственная задача — попытаться найти JWT-токен и, если он валиден, установить информацию об аутентификации в SecurityContext. Он отвечает на вопрос: "Кто этот пользователь?". Если он не может ответить на этот вопрос (из-за ошибки, отсутствия токена и т.д.), он просто "умывает руки".
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // Извлекаем JWT токен из заголовка
            String jwt = getJwtFromRequest(request);

            // Логируем информацию о запросе для отладки
            log.debug("Processing request to: {} {}", request.getMethod(), request.getRequestURI());

            // Проверяем наличие токена и его валидность
            if (StringUtils.hasText(jwt) && jwtService.validateToken(jwt)) {

                // Проверяем, не находится ли токен в черном списке
                if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
                    log.warn("Попытка использования токена из черного списка");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked");
                    return;
                }

                // Извлекаем username из токена
                String username = jwtService.getUsernameFromToken(jwt);
                log.debug("JWT токен валиден для пользователя: {}", username);

                // Загружаем данные пользователя
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Дополнительная проверка токена с UserDetails
                if (jwtService.validateToken(jwt, userDetails)) {
                    // Создаем объект аутентификации
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null, // credentials не нужны после аутентификации
                                    userDetails.getAuthorities()
                            );

                    // Добавляем детали запроса
                    /*
                    Этот объект извлекает из request (вашего HTTP-запроса) и сохраняет в себе:
◦
                    IP-адрес клиента, с которого пришел запрос.
◦
                    ID сессии, если она используется.
                    Для чего это нужно?
                    Эта информация не является критичной для самой аутентификации, но она очень полезна для:
                    1.
                    Аудита и логирования: Вы можете записывать в логи, с какого IP-адреса пользователь совершал те или иные действия. Это помогает при расследовании инцидентов безопасности.
                    2.
                    Дополнительных проверок безопасности: Теоретически, вы можете реализовать логику, которая, например, запрещает одновременное использование одного и того же аккаунта с разных IP-адресов или отслеживает подозрительную активность.

                    Проще говоря, это обогащение данных об аутентифицированном пользователе контекстом самого запроса.
                    */
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Устанавливаем аутентификацию в SecurityContext
                    /*
                    Представьте себе SecurityContextHolder как временное хранилище информации о безопасности для текущего потока выполнения (т.е. для текущего HTTP-запроса).

                     Когда вы кладете в него объект Authentication, вы делаете следующее:
•
                    - Даете доступ к защищенным ресурсам: После этого шага, когда Spring Security будет решать, можно ли пустить пользователя на какой-то эндпоинт (например, помеченный @PreAuthorize("hasRole('USER')")), он заглянет в SecurityContextHolder, увидит там ваш объект Authentication и проверит, есть ли у пользователя нужные права (getAuthorities()).
•
                    - Позволяете получить данные пользователя в контроллере: Вы сможете легко получить информацию о текущем пользователе в методах контроллера, используя аннотацию @AuthenticationPrincipal.

                    Если не выполнить setAuthentication, то для Spring Security пользователь останется анонимным, и он не сможет получить доступ к защищенным эндпоинтам, даже если его токен был абсолютно валидным.
                    */
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Аутентификация установлена для пользователя: {}", username);
                } else {
                    log.warn("JWT токен не прошел дополнительную валидацию для пользователя: {}", username);
                }
            } else if (StringUtils.hasText(jwt)) {
                log.debug("JWT токен невалиден или истек");
            }

        } catch (Exception ex) {
            log.error("Не удалось установить аутентификацию пользователя: {}", ex.getMessage());
            // Не прерываем цепочку фильтров, пусть Spring Security обработает отсутствие аутентификации. Наш фильтр только проверяет jwt токен, дальше Spring Security передаст ответственность на AuthorizationFilter, он поймет, что не аутентифицированный пользователь (аноним) не имеет прав доступа к эндпоинтам и отправит Unauthorized или 403 Forbidden на фронт
        }

        // Продолжаем цепочку фильтров
        filterChain.doFilter(request, response);
    }

    /**
     * Извлечение JWT токена из заголовка Authorization
     * Ожидаемый формат: "Bearer <token>"
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // Проверяем наличие заголовка и правильный формат
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7); // Убираем "Bearer "
            log.debug("JWT токен извлечен из заголовка Authorization");
            return token;
        }

        // Альтернативный способ - проверка в query параметрах (для WebSocket соединений)
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            log.debug("JWT токен извлечен из query параметра");
            return tokenParam;
        }

        log.debug("JWT токен не найден в запросе");
        return null;
    }

    /**
     * Определяем, нужно ли пропустить фильтр для данного запроса
     * Можно пропускать для публичных endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Список путей, которые не требуют JWT проверки
        return path.startsWith("/api/v1/auth/register") ||
                path.startsWith("/api/v1/auth/login") ||
                path.startsWith("/api/v1/auth/refresh") ||
                path.startsWith("/api/v1/auth/forgot-password") ||
                path.startsWith("/api/v1/auth/reset-password") ||
                path.startsWith("/api/v1/auth/verify-email") ||
                path.startsWith("/actuator/health") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");
    }
}