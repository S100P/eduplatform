package ru.s100p.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import ru.s100p.shared.dto.ApiResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Обработчик для неаутентифицированных запросов
 * Возвращает JSON ответ с ошибкой 401 вместо редиректа на страницу логина
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Метод вызывается когда неаутентифицированный пользователь
     * пытается получить доступ к защищенному ресурсу
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.error("Неаутентифицированный доступ к: {} {}",
                request.getMethod(), request.getRequestURI());

        // Определяем причину ошибки
        String errorMessage = determineErrorMessage(request, authException);
        String errorCode = determineErrorCode(request);

        // Логируем детали ошибки
        log.debug("Ошибка аутентификации: {}", errorMessage);
        if (authException != null) {
            log.debug("Исключение: {}", authException.getMessage());
        }

        // Создаем ответ с ошибкой
        ApiResponse<Object> errorResponse = ApiResponse.builder()
                .success(false)
                .message(errorMessage)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();

        // Добавляем дополнительные детали для отладки (только в dev режиме)
        if (isDebugMode()) {
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("path", request.getRequestURI());
            debugInfo.put("method", request.getMethod());
            if (authException != null) {
                debugInfo.put("exception", authException.getClass().getSimpleName());
                debugInfo.put("exceptionMessage", authException.getMessage());
            }
            errorResponse.setData(debugInfo);
        }

        // Устанавливаем статус и заголовки ответа
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Добавляем заголовок WWW-Authenticate согласно стандарту
        response.addHeader("WWW-Authenticate", "Bearer realm=\"User Service\"");

        // Записываем JSON ответ
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    /**
     * Определение сообщения об ошибке на основе контекста
     */
    private String determineErrorMessage(HttpServletRequest request, AuthenticationException authException) {
        // Проверяем атрибуты запроса на наличие специфичных ошибок
        Object jwtError = request.getAttribute("jwt_error");

        if (jwtError != null) {
            return jwtError.toString();
        }

        // Проверяем тип исключения
        if (authException != null) {
            String exceptionMessage = authException.getMessage();

            if (exceptionMessage != null && exceptionMessage.contains("expired")) {
                return "Токен аутентификации истек. Пожалуйста, войдите в систему заново";
            }

            if (exceptionMessage != null && exceptionMessage.contains("JWT")) {
                return "Недействительный токен аутентификации";
            }

            if (exceptionMessage != null && exceptionMessage.contains("Bad credentials")) {
                return "Неверные учетные данные";
            }
        }

        // Проверяем наличие заголовка Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            return "Отсутствует токен аутентификации. Пожалуйста, войдите в систему";
        }

        if (!authHeader.startsWith("Bearer ")) {
            return "Неверный формат токена. Используйте: Bearer <token>";
        }

        // Сообщение по умолчанию
        return "Для доступа к данному ресурсу требуется аутентификация";
    }

    /**
     * Определение кода ошибки
     */
    private String determineErrorCode(HttpServletRequest request) {
        Object jwtErrorCode = request.getAttribute("jwt_error_code");

        if (jwtErrorCode != null) {
            return jwtErrorCode.toString();
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            return "MISSING_TOKEN";
        }

        if (!authHeader.startsWith("Bearer ")) {
            return "INVALID_TOKEN_FORMAT";
        }

        return "AUTHENTICATION_REQUIRED";
    }

    /**
     * Проверка режима отладки
     * В продакшене следует использовать профили Spring
     */
    private boolean isDebugMode() {
        // TODO: Использовать @Value("${app.debug:false}") или профили Spring
        return true; // Для разработки
    }
}
