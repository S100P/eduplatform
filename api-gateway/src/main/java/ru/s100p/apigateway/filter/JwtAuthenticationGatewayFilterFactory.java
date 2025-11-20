package ru.s100p.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * Фабрика для создания экземпляров {@link AuthenticationFilter}.
 * <p>
 * Этот класс является стандартным "шаблоном" (boilerplate) для интеграции кастомного
 * {@link GatewayFilter} в Spring Cloud Gateway. Он позволяет использовать наш фильтр
 * декларативно в файле `application.yml`.
 * </p>
 * <p>
 * Spring Cloud Gateway автоматически обнаруживает бины, наследующие {@link AbstractGatewayFilterFactory}.
 * Имя фильтра в `application.yml` определяется по имени класса фабрики.
 * По соглашению, `JwtAuthenticationGatewayFilterFactory` позволяет использовать фильтр под именем `JwtAuthentication`.
 * </p>
 * <p>
 * Пример использования в `application.yml`:
 * <pre>
 * spring:
 *   cloud:
 *     gateway:
 *       routes:
 *         - id: user-service
 *           uri: http://localhost:8012
 *           predicates:
 *             - Path=/api/v1/users/**
 *           filters:
 *             - JwtAuthentication # <-- Это имя становится доступным благодаря этой фабрике
 * </pre>
 */
@Component
public class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    /**
     * Экземпляр нашего основного фильтра аутентификации.
     * Spring внедрит его сюда как зависимость.
     */
    private final AuthenticationFilter filter;

    /**
     * Конструктор для внедрения нашего кастомного фильтра.
     *
     * @param filter Экземпляр {@link AuthenticationFilter}, который будет применяться этой фабрикой.
     */
    public JwtAuthenticationGatewayFilterFactory(AuthenticationFilter filter) {
        super(Config.class);
        this.filter = filter;
    }

    /**
     * Основной метод фабрики, который возвращает экземпляр фильтра.
     * В данном случае, он просто возвращает уже существующий бин {@link AuthenticationFilter}.
     *
     * @param config Конфигурация, которая может быть передана из `application.yml` (в данном случае не используется).
     * @return Экземпляр {@link GatewayFilter} для применения к маршруту.
     */
    @Override
    public GatewayFilter apply(Config config) {
        return filter;
    }

    /**
     * Класс для хранения конфигурации, передаваемой из `application.yml`.
     * <p>
     * Например, если бы в `application.yml` было:
     * <pre>
     * - name: JwtAuthentication
     *   args:
     *     my-property: my-value
     * </pre>
     * То можно было бы добавить поле `private String myProperty;` в этот класс.
     * В нашей реализации конфигурация не требуется.
     * </p>
     */
    public static class Config {
    }
}
