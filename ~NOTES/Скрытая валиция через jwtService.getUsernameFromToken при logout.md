# Скрытая (неявная) валидация JWT через `getUsernameFromToken`

При анализе кода может показаться, что в методах `logout` и `logoutFromAllDevices` отсутствует предварительная валидация JWT перед его обработкой. Это не совсем так. Валидация происходит, но она "спрятана" внутри другого метода, что делает ее неявной.

### Как это работает?

В коде присутствует вызов:
```java
String username = jwtService.getUsernameFromToken(token);
```

На первый взгляд, этот метод просто извлекает имя пользователя. Но давайте посмотрим на его типичную реализацию в `JwtService`:

```java
// 1. Публичный метод, который мы вызываем
public String getUsernameFromToken(String token) {
    // Он просто вызывает более общий метод, передавая ему функцию
    // для извлечения нужного поля (subject/username).
    return extractClaim(token, Claims::getSubject);
}

// 2. Приватный метод для извлечения любого поля (claim)
private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    // Сначала извлекаются ВСЕ claims
    final Claims claims = extractAllClaims(token);
    // И только потом из них извлекается нужное значение
    return claimsResolver.apply(claims);
}

// 3. "Сердце" валидации
private Claims extractAllClaims(String token) {
    // Эта цепочка вызовов и есть полная валидация токена
    return Jwts.parserBuilder()
            .setSigningKey(getSignKey()) // Используем ключ для проверки подписи
            .build()
            .parseClaimsJws(token)       // <<<<<<< ЗДЕСЬ ПРОИСХОДИТ ПАРСИНГ И ВАЛИДАЦИЯ
            .getBody();
}
```

### Что происходит при вызове `parseClaimsJws(token)`?

Этот один метод из библиотеки `jjwt` выполняет сразу три критически важные проверки:

1.  **Проверка подписи (Signature Validation):** Сравнивает подпись токена с подписью, сгенерированной с использованием секретного ключа. Если они не совпадают, выбрасывается `SecurityException` или `MalformedJwtException`. Это защищает от подделки токенов.
2.  **Проверка срока жизни (Expiration Validation):** Сравнивает поле `exp` (expiration) в токене с текущим временем. Если токен просрочен, выбрасывается `ExpiredJwtException`.
3.  **Проверка формата (Format Validation):** Убеждается, что строка является корректным JWT. В противном случае выбрасывается `MalformedJwtException`.

### Вывод

Любой вызов, который внутри себя использует `parseClaimsJws(token)` (например, `getUsernameFromToken` или `getExpirationDateFromToken`), по сути, является **валидирующим вызовом**.

*   **Плюс:** Код работает и безопасен, так как невалидный токен вызовет исключение и прервет выполнение метода.
*   **Минус:** Это **неочевидно** для того, кто читает код. Логика "сначала проверь, потом используй" не выражена явно. Лучшей практикой является явный вызов `jwtService.validateToken(token)` в начале метода, чтобы сделать намерение программиста ясным и улучшить читаемость кода.
