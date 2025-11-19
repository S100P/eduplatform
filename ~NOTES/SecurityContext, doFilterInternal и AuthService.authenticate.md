# Принцип работы SecurityContext, doFilterInternal и AuthService.authenticate

Это заметка, объясняющая ключевые аспекты работы аутентификации в Spring Security при использовании JWT.

### 1. Для чего нужен `SecurityContextHolder.getContext().setAuthentication(authentication)`?

Это ключевой шаг, который **сообщает Spring Security, что текущий пользователь успешно аутентифицирован для данного конкретного запроса.**

Представьте себе `SecurityContextHolder` как временное хранилище информации о безопасности для текущего потока выполнения (т.е. для текущего HTTP-запроса).

Когда вы кладете в него объект `Authentication`, вы делаете следующее:
*   **Даете доступ к защищенным ресурсам:** После этого шага, когда Spring Security будет решать, можно ли пустить пользователя на какой-то эндпоинт (например, помеченный `@PreAuthorize("hasRole('USER')")`), он заглянет в `SecurityContextHolder`, увидит там ваш объект `Authentication` и проверит, есть ли у пользователя нужные права (`getAuthorities()`).
*   **Позволяете получить данные пользователя в контроллере:** Вы сможете легко получить информацию о текущем пользователе в методах контроллера, используя аннотацию `@AuthenticationPrincipal`.

Если **не выполнить** `setAuthentication`, то для Spring Security пользователь останется анонимным, и он не сможет получить доступ к защищенным эндпоинтам, даже если его токен был абсолютно валидным.

### 2. Почему это не происходит автоматически?

Потому что Spring Security по своей природе **не знает о вашем JWT-токене** и о том, как его обрабатывать.

Стандартные механизмы Spring Security — это аутентификация через сессии (stateful), Basic Auth, форма входа и т.д. Использование JWT — это **безсессионный (stateless)** подход, который вы реализуете самостоятельно.

Ваш класс `JwtAuthenticationFilter` — это и есть та самая **инструкция для Spring Security**:
1.  Вы говорите: "Для каждого запроса, пожалуйста, используй *мой* фильтр".
2.  Внутри фильтра вы сами пишете логику: "Я достану токен из заголовка `Authorization`".
3.  "Я проверю его подпись и срок действия с помощью *моего* `JwtService`".
4.  "Если все хорошо, я сам создам стандартный для Spring Security объект `UsernamePasswordAuthenticationToken` и **вручную положу его в `SecurityContextHolder`**, чтобы остальная часть Spring Security могла с ним работать".

Таким образом, ваш фильтр выступает "переводчиком" между вашим кастомным механизмом JWT и стандартным механизмом работы Spring Security.

### 3. Как SecurityContext получает аутентификацию в случае с `AuthService.authenticate`?

В методе `AuthService.authenticate` **основная цель — не установить аутентификацию в `SecurityContext`, а проверить учетные данные и сгенерировать токен.**

Взгляните на этот блок в методе `authenticate`:

```java
// Аутентификация через Spring Security
Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                request.getPassword()
        )
);
```

Здесь происходит следующее:
1.  Вы создаете `UsernamePasswordAuthenticationToken`, который содержит логин и пароль, введенные пользователем.
2.  Вы передаете этот токен в `authenticationManager`.
3.  `authenticationManager` находит подходящий `AuthenticationProvider` (например, `DaoAuthenticationProvider`).
4.  Этот провайдер использует ваш `UserDetailsService` для загрузки пользователя из базы данных.
5.  Затем он сравнивает хэш пароля из базы с хэшем пароля, который ввел пользователь.
6.  Если все совпадает, `authenticationManager.authenticate()` возвращает **полностью заполненный объект `Authentication`**, у которого флаг `isAuthenticated()` установлен в `true`.

**Но и здесь `SecurityContext` не затрагивается!**

Результат работы `authenticationManager.authenticate()` используется **только для того, чтобы сгенерировать JWT-токен**:

```java
// Генерация токенов
String accessToken = jwtService.generateAccessToken(authentication);
```

Вы просто берете из этого объекта `Authentication` имя пользователя и его роли и "зашиваете" их в JWT.

### Итог:

*   **При логине (в `AuthController` -> `AuthService.authenticate`)**: `SecurityContext` **не используется**. Происходит проверка логина/пароля, и если она успешна, клиенту просто возвращается JWT-токен в теле ответа. Аутентификация для этого конкретного запроса на логин не нужна, так как эндпоинт `/api/v1/auth/login` и так публичный.

*   **При последующих запросах (например, на `/api/v1/users/me`)**: `JwtAuthenticationFilter` перехватывает запрос, извлекает токен, валидирует его и **вручную устанавливает аутентификацию в `SecurityContext`**. Только после этого Spring Security разрешает доступ к защищенному эндпоинту.
