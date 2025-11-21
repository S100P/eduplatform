package ru.s100p.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Фильтр безопасности для микросервиса, проверяющий подписанные заголовки от API Gateway.
 * <p>
 * Этот фильтр является второй линией обороны в архитектуре "Zero Trust". Он перехватывает
 * все запросы, приходящие в сервис, и ищет специальный набор заголовков (`X-Auth-*`),
 * которые должен был добавить API Gateway.
 * </p>
 * <p>
 * Его задачи:
 * <ol>
 *     <li><b>Проверка наличия заголовков:</b> Убеждается, что все необходимые `X-Auth-*` заголовки присутствуют.</li>
 *     <li><b>Проверка срока действия:</b> Проверяет заголовок `X-Auth-Exp`, чтобы убедиться, что внутренний токен не истек.</li>
 *     <li><b>Проверка подписи:</b> Пересчитывает HMAC-подпись на основе полученных данных и сравнивает ее с подписью в `X-Auth-Signature`. Это гарантирует, что заголовки не были подделаны по пути от шлюза к сервису.</li>
 *     <li><b>Заполнение SecurityContext:</b> Если все проверки пройдены, он создает объект `Authentication` и помещает его в {@link SecurityContextHolder}. Это позволяет стандартным механизмам Spring Security (например, `@PreAuthorize`, `hasRole()`) работать дальше, как будто аутентификация прошла обычным путем.</li>
 * </ol>
 * <p>
 * Этот фильтр добавляется в цепочку безопасности в классе {@link ru.s100p.user.config.SecurityConfig}.
 * </p>
 */
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKeySpec secretKeySpec;

    /**
     * Конструктор для внедрения зависимостей.
     *
     * @param internalSecret Секретный ключ для проверки HMAC-подписи.
     *                       Это тот же самый ключ, который используется в `api-gateway` для создания подписи.
     *                       Инжектируется из `application.yml` (jwt.internal-secret).
     */
    public HeaderAuthenticationFilter(@Value("${jwt.internal-secret}") String internalSecret) {
        this.secretKeySpec = new SecretKeySpec(internalSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * Основной метод фильтра, который выполняется для каждого запроса.
     *
     * <p>
     * HeaderAuthenticationFilter наследуется от OncePerRequestFilter, Spring ищет реализацию OncePerRequestFilter и запустит выполнение переопределенного doFilterInternal автоматически.
     * <p>
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Извлекаем все необходимые заголовки.
        String userId = request.getHeader("X-Auth-User");
        String userRoles = request.getHeader("X-Auth-Roles");
        String expiration = request.getHeader("X-Auth-Exp");
        String signature = request.getHeader("X-Auth-Signature");

        // Если какого-либо из заголовков нет, мы не можем доверять запросу.
        // Мы просто передаем запрос дальше по цепочке. Если эндпоинт защищен,
        // Spring Security отклонит его, так как SecurityContext будет пуст.
        // Если эндпоинт публичный (permitAll), он будет обработан.
        if (userId == null || userRoles == null || expiration == null || signature == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1. Проверяем, не истек ли срок действия внутреннего токена.
            if (isExpired(expiration)) {
                sendError(response, "Internal token expired");
                return; // Прерываем цепочку.
            }

            // 2. Проверяем целостность и подлинность заголовков, проверяя подпись.
            String dataToVerify = String.join(":", userId, userRoles, expiration);
            if (!isValidSignature(dataToVerify, signature)) {
                sendError(response, "Invalid internal signature");
                return; // Прерываем цепочку.
            }

            // 3. Если все проверки пройдены, создаем объект аутентификации.
            List<SimpleGrantedAuthority> authorities = Arrays.stream(userRoles.split(","))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // Создаем токен аутентификации. В качестве "principal" используем userId. Пароль не нужен (null).
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // Добавляем детали запроса (IP адрес)
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

            // Помещаем объект аутентификации в SecurityContext.
            // Теперь Spring Security знает, что пользователь аутентифицирован и имеет определенные роли.
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            //TODO В реальном приложении здесь должно быть логирование.
            sendError(response, "Error processing internal authentication");
            return;
        }

        // Передаем запрос дальше по цепочке фильтров.
        filterChain.doFilter(request, response);
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


    /**
     * Проверяет, что время в заголовке `X-Auth-Exp` еще не наступило.
     * @param expiration Время в миллисекундах в виде строки.
     * @return {@code true}, если время истекло, иначе {@code false}.
     */
    private boolean isExpired(String expiration) {
        return Long.parseLong(expiration) < System.currentTimeMillis();
    }

    /**
     * Проверяет HMAC-подпись.
     * @param data Данные, которые были подписаны (конкатенация userId, roles, expiration).
     * @param signature Подпись из заголовка `X-Auth-Signature`.
     * @return {@code true}, если вычисленная подпись совпадает с полученной, иначе {@code false}.
     */
    private boolean isValidSignature(String data, String signature) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        byte[] calculatedHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        String calculatedSignature = Base64.getEncoder().encodeToString(calculatedHmac);
        return signature.equals(calculatedSignature);
    }

    /**
     * Отправляет ответ с ошибкой 401 Unauthorized.
     */
    private void sendError(HttpServletResponse response, String message) throws IOException {
        // Очищаем SecurityContext на случай, если там что-то было.
        SecurityContextHolder.clearContext();
        response.sendError(HttpStatus.UNAUTHORIZED.value(), message);
    }
}
