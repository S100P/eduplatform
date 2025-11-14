# EduPlatform - Архитектура микросервисов

## Сервисы

### 1. User Service (Сервис пользователей)
**Порт:** 8081
**База данных:** PostgreSQL (users_db)
**Ответственность:**
- Регистрация и аутентификация пользователей
- Управление профилями
- Роли и права доступа (RBAC)
- JWT токены

**Endpoints:**
- POST /api/users/register
- POST /api/users/login
- GET /api/users/profile
- PUT /api/users/profile
- GET /api/users/{id}

### 2. Course Service (Сервис курсов)
**Порт:** 8082
**База данных:** PostgreSQL (courses_db)
**Ответственность:**
- CRUD операции с курсами
- Управление уроками и материалами
- Категории курсов
- Поиск и фильтрация

**Endpoints:**
- POST /api/courses
- GET /api/courses
- GET /api/courses/{id}
- PUT /api/courses/{id}
- DELETE /api/courses/{id}
- GET /api/courses/{id}/lessons

### 3. Enrollment Service (Сервис записи)
**Порт:** 8083
**База данных:** PostgreSQL (enrollments_db)
**Ответственность:**
- Запись студентов на курсы
- Отслеживание прогресса обучения
- Управление доступом к урокам
- Статистика обучения

**Endpoints:**
- POST /api/enrollments
- GET /api/enrollments/user/{userId}
- GET /api/enrollments/course/{courseId}
- PUT /api/enrollments/{id}/progress

### 4. Payment Service (Сервис платежей)
**Порт:** 8084
**База данных:** PostgreSQL (payments_db)
**Ответственность:**
- Обработка платежей
- История транзакций
- Интеграция с внешними платежными системами
- Управление подписками

**Endpoints:**
- POST /api/payments/process
- GET /api/payments/user/{userId}
- GET /api/payments/{id}
- POST /api/payments/refund

### 5. Notification Service (Сервис уведомлений)
**Порт:** 8085
**База данных:** PostgreSQL (notifications_db)
**Ответственность:**
- Отправка email уведомлений
- Push уведомления
- SMS уведомления (опционально)
- Шаблоны уведомлений

**Endpoints:**
- POST /api/notifications/send
- GET /api/notifications/user/{userId}
- PUT /api/notifications/{id}/read

### 6. Analytics Service (Сервис аналитики)
**Порт:** 8086
**База данных:** PostgreSQL (analytics_db)
**Ответственность:**
- Сбор и анализ данных о пользователях
- Статистика курсов
- Отчеты для администраторов
- Метрики производительности

**Endpoints:**
- GET /api/analytics/users/stats
- GET /api/analytics/courses/stats
- GET /api/analytics/revenue
- GET /api/analytics/dashboard

### 7. API Gateway
**Порт:** 8080
**Технология:** Spring Cloud Gateway
**Ответственность:**
- Маршрутизация запросов
- Аутентификация и авторизация
- Rate limiting
- Логирование
- CORS

## Kafka Topics

### Event-Driven Communication
- **user-registered** - пользователь зарегистрирован
- **user-profile-updated** - профиль пользователя обновлен
- **course-created** - курс создан
- **course-updated** - курс обновлен
- **enrollment-created** - студент записан на курс
- **lesson-completed** - урок завершен
- **payment-processed** - платеж обработан
- **payment-failed** - платеж неуспешен
- **notification-requested** - запрос на отправку уведомления

## Базы данных

### PostgreSQL схемы
- **users_db:** таблицы users, roles, user_roles
- **courses_db:** таблицы courses, lessons, categories, course_categories
- **enrollments_db:** таблицы enrollments, progress, lesson_completions
- **payments_db:** таблицы payments, transactions, subscriptions
- **notifications_db:** таблицы notifications, templates, delivery_status
- **analytics_db:** таблицы user_stats, course_stats, revenue_stats

## Docker Setup

### docker-compose.yml структура
```yaml
services:
  # Databases
  postgres-users:
    image: postgres:15
    environment:
      POSTGRES_DB: users_db
  
  postgres-courses:
    image: postgres:15
    environment:
      POSTGRES_DB: courses_db
  
  # ... остальные БД
  
  # Kafka Infrastructure
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
  
  kafka:
    image: confluentinc/cp-kafka:latest
  
  # Services
  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
  
  user-service:
    build: ./user-service
    ports:
      - "8081:8081"
  
  # ... остальные сервисы
```

## Security Architecture

### Spring Security Configuration
- **JWT Authentication:** для stateless авторизации
- **OAuth2 Resource Server:** для защиты endpoints
- **Method Level Security:** @PreAuthorize аннотации
- **CORS Configuration:** для frontend интеграции

### Роли и права
- **ADMIN:** полный доступ ко всем ресурсам
- **INSTRUCTOR:** создание и управление курсами
- **STUDENT:** доступ к приобретенным курсам
- **GUEST:** только просмотр публичной информации

## Мониторинг и Логирование

### Технологии
- **Spring Boot Actuator:** health checks и метрики
- **Micrometer + Prometheus:** сбор метрик
- **ELK Stack:** централизованное логирование
- **Distributed Tracing:** Spring Cloud Sleuth

## Паттерны проектирования

### Используемые паттерны
- **API Gateway Pattern:** единая точка входа
- **Database Per Service:** изоляция данных
- **Event Sourcing:** для аудита изменений
- **CQRS:** разделение команд и запросов
- **Circuit Breaker:** устойчивость к сбоям
- **Saga Pattern:** распределенные транзакции