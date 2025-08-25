package ru.s100p.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import ru.s100p.shared.dto.ApiResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Обработчик для случаев, когда аутентифицированный пользователь
 * не имеет прав доступа к запрашиваемому ресурсу (403 Forbidden)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Обработка отказа в доступе
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // Получаем информацию о текущем пользователе
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymous";

        log.warn("Отказ в доступе для пользователя '{}' к ресурсу: {} {}",
                username, request.getMethod(), request.getRequestURI());

        // Определяем сообщение об ошибке
        String errorMessage = determineErrorMessage(request, authentication);

        // Создаем ответ с ошибкой
        ApiResponse<Object> errorResponse = ApiResponse.builder()
                .success(false)
                .message(errorMessage)
                .errorCode("ACCESS_DENIED")
                .timestamp(LocalDateTime.now())
                .build();

        // Добавляем дополнительную информацию для отладки
        if (isDebugMode()) {
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("path", request.getRequestURI());
            debugInfo.put("method", request.getMethod());
            debugInfo.put("user", username);
            if (authentication != null) {
                debugInfo.put("authorities", authentication.getAuthorities().toString());
            }
            debugInfo.put("requiredRole", extractRequiredRole(request));
            errorResponse.setData(debugInfo);
        }

        // Устанавливаем статус и заголовки ответа
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Записываем JSON ответ
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    /**
     * Определение сообщения об ошибке на основе контекста
     */
    private String determineErrorMessage(HttpServletRequest request, Authentication authentication) {
        String path = request.getRequestURI();

        // Специфичные сообщения для разных endpoints
        if (path.contains("/admin")) {
            return "Доступ разрешен только администраторам";
        }

        if (path.contains("/instructor")) {
            return "Доступ разрешен только инструкторам";
        }

        if (path.contains("/courses") && request.getMethod().equals("POST")) {
            return "Только инструкторы могут создавать курсы";
        }

        if (path.contains("/users") && request.getMethod().equals("DELETE")) {
            return "Недостаточно прав для удаления пользователя";
        }

        // Проверяем, верифицирован ли email
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal) {
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            if (!principal.isEmailVerified()) {
                return "Для доступа к данному ресурсу необходимо подтвердить email";
            }
        }

        // Сообщение по умолчанию
        return "У вас недостаточно прав для выполнения данной операции";
    }

    /**
     * Извлечение требуемой роли из атрибутов запроса
     */
    private String extractRequiredRole(HttpServletRequest request) {
        // Spring Security может сохранять требуемые authorities в атрибутах
        Object requiredAuthorities = request.getAttribute("required_authorities");
        if (requiredAuthorities != null) {
            return requiredAuthorities.toString();
        }

        // Определяем по пути
        String path = request.getRequestURI();
        if (path.contains("/admin")) {
            return "ROLE_ADMIN";
        }
        if (path.contains("/instructor")) {
            return "ROLE_INSTRUCTOR";
        }

        return "UNKNOWN";
    }

    /**
     * Проверка режима отладки
     */
    private boolean isDebugMode() {
        // TODO: Использовать профили Spring
        return true; // Для разработки
    }
}
