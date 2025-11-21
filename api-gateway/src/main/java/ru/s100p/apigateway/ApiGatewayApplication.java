package ru.s100p.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс для запуска приложения API Gateway.
 * <p>
 * Аннотация {@link SpringBootApplication} включает в себя:
 * <ul>
 *     <li>{@code @Configuration}: Помечает класс как источник определений бинов.</li>
 *     <li>{@code @EnableAutoConfiguration}: Включает механизм автоконфигурации Spring Boot.</li>
 *     <li>{@code @ComponentScan}: Включает сканирование компонентов в текущем пакете и его подпакетах.</li>
 * </ul>
 * <p>
 * Для Spring Cloud Gateway дополнительных аннотаций (например, {@code @EnableDiscoveryClient})
 * не требуется, если используется автоконфигурация.
 * </p>
 */
@SpringBootApplication
public class ApiGatewayApplication {

    /**
     * Точка входа в приложение.
     *
     * @param args Аргументы командной строки.
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
