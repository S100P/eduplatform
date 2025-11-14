# 🔄 Жизненные сценарии и взаимодействие модулей (с API Gateway)

Ниже — наглядные **диаграммы последовательностей (Mermaid)** и **поэтапные описания**, показывающие, как модули и классы взаимодействуют в типичных сценариях: **регистрация**, **логин**, **доступ к защищённому ресурсу**, **запись на курс с оплатой**. Внутри каждого сценария разобрана **внутренняя работа классов** (контроллеры → сервисы → репозитории/утилиты).

> Легенда классов (по слоям в микросервисах):  
> **Controller** — принимает HTTP, валидирует вход;  
> **Service** — бизнес-логика;  
> **Repository** — доступ к БД;  
> **Utils** — хелперы (JwtProvider, PasswordHasher и т. п.).

---

## 0) Классическая структура классов (пример: User Service)

```mermaid
classDiagram
  class UserController {
    +register(dto)
    +login(dto)
    +refresh(token)
    +getProfile()
    +updateProfile(dto)
  }
  class AuthController {
    +login(dto)
    +refresh(token)
    +logout()
  }
  class UserService {
    +createUser(dto)
    +findByEmail(email)
    +getProfile(userId)
    +updateProfile(userId,dto)
    +assignRoles(userId,roles)
  }
  class AuthService {
    +authenticate(email,password)
    +issueTokens(user)
    +refreshTokens(refreshToken)
    +revoke(refreshToken)
  }
  class JwtProvider {
    +sign(claims,exp)
    +validate(jwt)
    +getPublicJwks()
  }
  class PasswordHasher {
    +hash(password)
    +verify(plain,hash)
  }
  class UserRepository {
    +save(user)
    +findByEmail(email)
    +findById(id)
  }

  UserController --> UserService
  UserController --> AuthService
  AuthController --> AuthService
  AuthService --> UserService
  AuthService --> JwtProvider
  UserService --> PasswordHasher
  UserService --> UserRepository
```

---

## 1) Регистрация пользователя (возможен «автовход» после регистрации)

```mermaid
sequenceDiagram
    autonumber
    participant F as Frontend
    participant G as API Gateway
    participant Uc as UserController
    participant Us as UserService
    participant Ph as PasswordHasher
    participant Ur as UserRepository
    participant As as AuthService
    participant J as JwtProvider
    participant N as Notification Service (опц)

    F->>G: POST /api/users/register (email, password, name)
    G->>Uc: маршрутизация в user-service
    Uc->>Us: createUser(dto)
    Us->>Ph: hash(password)
    Ph-->>Us: passwordHash
    Us->>Ur: save(user+hash)
    Ur-->>Us: saved user(id, roles=["student"])
    alt Автовход включен
        Uc->>As: issueTokens(user)
        As->>J: sign({sub,roles,iss,aud,exp})
        J-->>As: JWT (access), refresh
        As-->>Uc: Tokens(access,refresh)
        Uc-->>G: 201 Created + tokens
    else Без автовхода
        Uc-->>G: 201 Created (без токенов)
    end
    opt Уведомление
        Uc->>N: POST /api/notifications/send (email, шаблон Welcome)
        N-->>Uc: 202 Accepted
    end
    G-->>F: Ответ клиенту
```

**Что происходит внутри:**
1. `UserController.register` валидирует DTO, вызывает `UserService.createUser`.
2. `UserService` хеширует пароль через `PasswordHasher` (Argon2/bcrypt), сохраняет через `UserRepository`, по умолчанию даёт роль `student`.
3. Если включён «автовход», `AuthService.issueTokens` создаёт `access` и `refresh` через `JwtProvider`.
4. (Опционально) отправляется welcome‑уведомление в Notification Service.

---

## 2) Логин (аутентификация) и выдача токенов

```mermaid
sequenceDiagram
    autonumber
    participant F as Frontend
    participant G as API Gateway
    participant Ac as AuthController
    participant As as AuthService
    participant Us as UserService
    participant Ph as PasswordHasher
    participant J as JwtProvider

    F->>G: POST /api/users/login (email, password)
    G->>Ac: маршрутизация в user-service
    Ac->>As: authenticate(email,password)
    As->>Us: findByEmail(email)
    Us-->>As: user + passwordHash + roles
    As->>Ph: verify(password, hash)
    Ph-->>As: ok
    As->>J: sign({sub,roles,iss,aud,exp})
    J-->>As: JWT access + refresh
    As-->>Ac: Tokens(access, refresh)
    Ac-->>G: 200 OK + tokens
    G-->>F: 200 OK + tokens
```

**Внутри:**
- `AuthService.authenticate` тянет пользователя, сверяет пароль (`PasswordHasher.verify`), при успехе выпускает токены (`JwtProvider.sign`).  
- `API Gateway` здесь **не выдаёт токены**, он лишь проксирует. Выдача — строго в `user-service`.

---

## 3) Доступ к защищённому ресурсу (пример: свой список зачислений)

```mermaid
sequenceDiagram
    autonumber
    participant F as Frontend
    participant G as API Gateway (JwtAuthFilter, RateLimit, CORS)
    participant EnC as EnrollmentController
    participant EnS as EnrollmentService
    participant EnR as EnrollmentRepository

    F->>G: GET /api/enrollments/user/{userId} (Authorization: Bearer JWT)
    G->>G: JwtAuthFilter.validate(jwt: iss,aud,exp,signature)
    G->>G: Coarse auth: доступен ли роут роли?
    G->>EnC: проксируем запрос в enrollment-service
    EnC->>EnS: getByUser(userId, claims.sub, claims.roles)
    alt Владелец или admin
        EnS->>EnR: findByUserId(userId)
        EnR-->>EnS: список зачислений
        EnS-->>EnC: ok
        EnC-->>G: 200 + data
    else Чужие данные
        EnS-->>EnC: 403 Forbidden
        EnC-->>G: 403
    end
    G-->>F: Ответ
```

**Внутри:**
- **Gateway** проверяет JWT и «грубо» фильтрует доступ по ролям.  
- **EnrollmentService** делает «тонкую» проверку: `claims.sub == {userId}` или роль `admin`, иначе 403.

---

## 4) Запись на курс c оплатой и уведомлением

```mermaid
sequenceDiagram
    autonumber
    participant F as Frontend
    participant G as API Gateway (JWT check)
    participant P as Payment Service
    participant PSP as Внешний Провайдер Платежей
    participant En as Enrollment Service
    participant N as Notification Service
    participant A as Analytics Service

    F->>G: POST /api/payments/process {courseId, price} (Bearer JWT)
    G->>P: проксирование в payment-service
    P->>PSP: Создать платеж (redirect/3DS/webhook)
    PSP-->>P: Success (txnId)
    P-->>G: 200 OK + txnId

    Note over F,G: Клиент может получить 200, а собственно зачисление делается по webhook-е

    PSP-->>P: Webhook: payment.succeeded (txnId, userId, courseId)
    P->>En: POST /api/enrollments {userId, courseId, txnId}
    En-->>P: 201 Created
    P->>N: POST /api/notifications/send (email, "Вы записаны на курс")
    N-->>P: 202 Accepted
    P->>A: POST /api/analytics/revenue {txnId, amount, courseId}
    A-->>P: 202 Accepted
```

**Внутри Payment Service:**
- Контроллер принимает запрос и инициирует платёж у PSP (создание сессии/инвойса).
- На webhook `payment.succeeded` проверяется подпись вебхука, извлекаются `userId/courseId`, создаётся запись в Enrollment Service, шлётся письмо и метрика в Analytics.

---

## 5) Как работает API Gateway — технически

- **JwtAuthFilter** (или Spring Security) вешается на защищённые маршруты:
  - Проверяет подпись/`exp`/`iss`/`aud` (`/.well-known/jwks.json` в user-service).
  - Делает **coarse-grained** проверку ролей по маршруту/методу.
  - Ставит `X-Request-Id`, чистит входящие `X-User-*`, добавляет служебные заголовки.
- **Проксирование**: исходный `Authorization: Bearer <JWT>` либо:
  - прокидывается дальше в сервис (**Вариант A**), и сервисы имеют общий middleware для тонких проверок,  
  - или заменяется на подписанные внутренние заголовки `X-Auth-*` (**Вариант B**) с HMAC-подписью и TTL.
- **Технические фильтры**: Rate limiting (`/login`/`/refresh`/платежи), CORS, логирование, WAF.

---

## 6) Мини-чеклист на каждый сценарий

- Gateway: проверка JWT + грубая авторизация по роутам.
- Сервис: тонкая авторизация по **конкретным данным** (владелец/роль).
- Все сервисы — только из внутренней сети, **mTLS** между gateway и сервисами.
- Короткие access‑токены (5–15 мин), refresh — через user‑service.
- Логи/трейсы по `X-Request-Id`, корреляция событий (в т.ч. webhooks).
