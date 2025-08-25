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
     */
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
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Устанавливаем аутентификацию в SecurityContext
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
            // Не прерываем цепочку фильтров, пусть Spring Security обработает отсутствие аутентификации
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