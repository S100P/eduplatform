--liquibase formatted


-- Создание таблиц user_activity_logs, course_statistics, user_learning_stats

--changeset s100p:1 (create user_activity_logs table)
CREATE TABLE user_activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(100),

    -- Активность
    action VARCHAR(100) NOT NULL, -- LOGIN, LOGOUT, COURSE_VIEW, LESSON_START, etc.
    resource_type VARCHAR(50), -- COURSE, LESSON, PAYMENT
    resource_id BIGINT,

    -- Контекст
    ip_address INET,
    user_agent TEXT,
    referer VARCHAR(500),

    -- Дополнительные данные
    metadata JSON, -- дополнительная информация о событии
    duration INTEGER, -- продолжительность сессии/действия в секундах

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_activity_user (user_id),
    INDEX idx_activity_action (action),
    INDEX idx_activity_resource (resource_type, resource_id),
    INDEX idx_activity_session (session_id),
    INDEX idx_activity_created (created_at)
);


--changeset s100p:2 (create course_statistics table)
CREATE TABLE course_statistics (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    date DATE NOT NULL,

    -- Статистика просмотров и записей
    views_count INTEGER DEFAULT 0,
    enrollments_count INTEGER DEFAULT 0,
    completions_count INTEGER DEFAULT 0,

    -- Статистика доходов
    revenue DECIMAL(10,2) DEFAULT 0.00,
    refunds DECIMAL(10,2) DEFAULT 0.00,

    -- Средние показатели
    avg_completion_time INTEGER, -- в часах
    avg_rating DECIMAL(3,2),

    -- Обновление статистики
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(course_id, date),
    INDEX idx_course_stats_course (course_id),
    INDEX idx_course_stats_date (date)
);


--changeset s100p:3 (create user_learning_stats table)
CREATE TABLE user_learning_stats (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,

    -- Активность обучения
    total_study_time INTEGER DEFAULT 0, -- в минутах
    lessons_completed INTEGER DEFAULT 0,
    courses_enrolled INTEGER DEFAULT 0,
    courses_completed INTEGER DEFAULT 0,

    -- Достижения
    badges_earned INTEGER DEFAULT 0,
    certificates_earned INTEGER DEFAULT 0,

    -- Финансовая статистика
    total_spent DECIMAL(10,2) DEFAULT 0.00,

    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(user_id, date),
    INDEX idx_learning_stats_user (user_id),
    INDEX idx_learning_stats_date (date)
);






