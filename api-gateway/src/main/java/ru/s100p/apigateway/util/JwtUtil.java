package ru.s100p.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

/**
 * Утилитарный класс для работы с JSON Web Tokens (JWT).
 * <p>
 * Этот компонент отвечает за централизованную обработку JWT, полученных от клиентов.
 * Его основная задача — парсинг токена и проверка его валидности (в данном случае, срока действия).
 * Он инкапсулирует логику взаимодействия с библиотекой JJWT.
 * </p>
 * <p>
 * Используется {@link ru.s100p.apigateway.filter.AuthenticationFilter} для проверки токена перед тем,
 * как шлюз начнет обработку запроса и создание внутренних заголовков.
 * </p>
 */
@Component
public class JwtUtil {

    /**
     * Секретный ключ для валидации подписи JWT.
     * Этот ключ должен быть идентичен тому, который используется в `user-service` для генерации токенов.
     * Значение инжектируется из `application.yml` (jwt.secret).
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Объект ключа, используемый для проверки подписи токена.
     * Генерируется один раз при инициализации компонента.
     */
    private Key key;

    /**
     * Метод инициализации, который вызывается после создания бина.
     * Он преобразует строковый секрет в криптографический ключ ({@link Key}),
     * который затем используется для всех операций с JWT.
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Извлекает все "claims" (полезную нагрузку) из JWT.
     * Claims — это набор утверждений о пользователе (например, ID, роли, срок действия).
     *
     * @param token JWT, полученный от клиента.
     * @return Объект {@link Claims}, содержащий все данные из токена.
     * @throws io.jsonwebtoken.JwtException если токен имеет неверную подпись или структуру.
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    /**
     * Проверяет, истек ли срок действия токена.
     *
     * @param token JWT для проверки.
     * @return {@code true}, если срок действия токена (exp) наступил, иначе {@code false}.
     */
    private boolean isTokenExpired(String token) {
        return this.getAllClaimsFromToken(token).getExpiration().before(new Date());
    }

    /**
     * Основной метод для проверки валидности токена.
     * На данный момент, "невалидность" означает только истекший срок действия.
     * В будущем сюда можно добавить другие проверки (например, проверку "черного списка" токенов).
     *
     * @param token JWT для проверки.
     * @return {@code true}, если токен невалиден (истек), иначе {@code false}.
     */
    public boolean isInvalid(String token) {
        // В данном контексте, единственная проверка - это срок действия.
        return this.isTokenExpired(token);
    }
}
