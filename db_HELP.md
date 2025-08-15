Связи между базами данных (Cross-Service References)
Основные связи:

User Service → Course Service: courses.instructor_id → users.id
Course Service → Enrollment Service: enrollments.course_id → courses.id
User Service → Enrollment Service: enrollments.user_id → users.id
Payment Service: связи с users.id и courses.id
Analytics Service: агрегирует данные из всех сервисов

Принципы проектирования:

Database Per Service: каждый сервис имеет свою БД
Eventual Consistency: данные синхронизируются через события
Денормализация: дублируем критичные данные для производительности
Soft References: ссылки между сервисами через ID, без FK constraints