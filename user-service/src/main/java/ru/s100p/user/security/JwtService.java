package ru.s100p.user.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}") // 1 час в миллисекундах
    private int jwtExpirationMs;

    @Value("${jwt.refresh.expiration}") // 7 дней в миллисекундах
    private int refreshExpirationMs;

    /**
     * Генерация access токена для аутентифицированного пользователя.
     *
     * <p>Процесс генерации:</p>
     * <ol>
     *     <li>Извлекает объект {@code UserDetails} из {@code Authentication}, который содержит информацию о пользователе.</li>
     *     <li>Формирует claims (полезную нагрузку) токена:
     *         <ul>
     *             <li><b>username</b>: имя пользователя для идентификации владельца токена.</li>
     *             <li><b>authorities</b>: список ролей и прав доступа пользователя, преобразованный из {@code GrantedAuthority} в строки.</li>
     *         </ul>
     *     </li>
     *     <li>Вызывает {@code createToken} для создания подписанного JWT токена с коротким временем жизни ({@code jwtExpirationMs}).</li>
     * </ol>
     *
     * <p>Назначение access токена:</p>
     * <ul>
     *     <li>Используется для аутентификации пользователя при каждом запросе к защищенным ресурсам.</li>
     *     <li>Содержит информацию о правах доступа (authorities), что позволяет авторизовать действия без обращения к базе данных.</li>
     *     <li>Имеет короткий срок жизни (обычно 1 час) для минимизации рисков при компрометации токена.</li>
     * </ul>
     *
     * @param authentication объект аутентификации Spring Security, содержащий информацию о пользователе.
     * @return строковое представление JWT access токена.
     */
    public String generateAccessToken(Authentication authentication) {
        // Извлечение информации о пользователе из объекта аутентификации
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        // Формирование полезной нагрузки токена с данными пользователя
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", userPrincipal.getUsername());
        // Преобразование списка прав доступа в список строк для включения в токен
        claims.put("authorities", userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        // Создание и подпись токена с коротким временем жизни
        return createToken(claims, userPrincipal.getUsername(), jwtExpirationMs);
    }

    /**
     * Генерация refresh токена для обновления access токена.
     *
     * <p>Процесс генерации:</p>
     * <ol>
     *     <li>Создает минимальный набор claims, содержащий только тип токена ("refresh") для идентификации его назначения.</li>
     *     <li>Вызывает {@code createToken} для создания подписанного JWT токена с длинным временем жизни ({@code refreshExpirationMs}).</li>
     * </ol>
     *
     * <p>Назначение refresh токена:</p>
     * <ul>
     *     <li>Используется для получения нового access токена без повторной аутентификации пользователя (ввода логина и пароля).</li>
     *     <li>Имеет длительный срок жизни (обычно 7 дней), что позволяет пользователю оставаться авторизованным длительное время.</li>
     *     <li>Содержит минимальную информацию (только тип и username), так как не используется для авторизации запросов напрямую.</li>
     *     <li>При компрометации refresh токена злоумышленник может получить новый access токен, поэтому важно хранить его безопасно.</li>
     * </ul>
     *
     * @param username имя пользователя, для которого генерируется refresh токен.
     * @return строковое представление JWT refresh токена.
     */
    public String generateRefreshToken(String username) {
        // Формирование минимальной полезной нагрузки с указанием типа токена
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        // Создание и подпись токена с длинным временем жизни
        return createToken(claims, username, refreshExpirationMs);
    }

    /**
     * Создание и подпись JWT токена.
     *
     * <p>Процесс подписи:</p>
     * <p>Подпись токена происходит с помощью вызова {@code .signWith(getSignKey(), SignatureAlgorithm.HS512)}.</p>
     * <ol>
     *     <li><b>signWith(...)</b>: Метод из библиотеки JJWT, который отвечает за подпись JWT токена.</li>
     *     <li><b>getSignKey()</b>: Внутренний метод, который предоставляет ключ для подписи. Он декодирует секретную строку (jwtSecret) из Base64 и создает на ее основе ключ для HMAC-SHA алгоритма.</li>
     *     <li><b>SignatureAlgorithm.HS512</b>: Алгоритм шифрования (HMAC с использованием SHA-512), который используется для создания подписи.</li>
     * </ol>
     *
     * <p>Как это обеспечивает аутентификацию?</p>
     * <p>Когда сервер получает токен, он заново вычисляет подпись, используя тот же секретный ключ. Если вычисленная подпись совпадает с подписью в токене, это доказывает, что:</p>
     * <ul>
     *     <li>Токен был выдан именно этим сервером (т.к. только он знает секрет).</li>
     *     <li>Данные в токене не были изменены после его выдачи.</li>
     * </ul>
     * <p>Проверка подписи реализована в методе {@code validateToken}.</p>
     *
     * @param claims Дополнительные данные (полезная нагрузка), которые будут добавлены в токен.
     * @param subject "Тема" токена, обычно это имя пользователя.
     * @param expirationMs Время жизни токена в миллисекундах.
     * @return Строковое представление JWT токена.
     */
    private String createToken(Map<String, Object> claims, String subject, int expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                // Установка полезной нагрузки (claims)
                .setClaims(claims)
                // Установка "темы" токена (обычно username)
                .setSubject(subject)
                // Установка времени создания токена
                .setIssuedAt(now)
                // Установка времени истечения срока действия токена
                .setExpiration(expiryDate)
                // Подпись токена с использованием секретного ключа и алгоритма HS512
                .signWith(getSignKey(), SignatureAlgorithm.HS512)
                // Сборка и сериализация токена в компактную строку (JWS)
                .compact();
    }

    /**
     * Получение username из токена
     */
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Получение даты истечения токена
     */
    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Извлечение claim из токена
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Извлечение всех claims из токена
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Проверка истечения токена
     */
    private Boolean isTokenExpired(String token) {
        return getExpirationDateFromToken(token).before(new Date());
    }

    /**
     * Валидация токена
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Валидация токена без UserDetails
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("JWT token is expired");
        } catch (UnsupportedJwtException ex) {
            log.error("JWT token is unsupported");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    /**
     * Получение ключа для подписи
     */
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Получение authorities из токена
     */
    @SuppressWarnings("unchecked")
    public List<String> getAuthoritiesFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return (List<String>) claims.get("authorities");
    }
}