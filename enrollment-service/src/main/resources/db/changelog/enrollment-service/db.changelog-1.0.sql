--liquibase formatted


-- Создание таблиц enrollments, lesson_progress, quizzes, quiz_attempts

--changeset s100p:1 (create enrollments table)
CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,

    -- Статус записи
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, CANCELLED, EXPIRED

    -- Даты
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP, -- для платных курсов с ограниченным доступом
    completed_at TIMESTAMP,
    last_accessed_at TIMESTAMP,

    -- Прогресс
    progress_percentage DECIMAL(5,2) DEFAULT 0.00, -- 0.00 - 100.00
    lessons_completed INTEGER DEFAULT 0,
    total_lessons INTEGER NOT NULL,
    total_watch_time INTEGER DEFAULT 0, -- в секундах

    -- Сертификация
    certificate_issued BOOLEAN DEFAULT false,
    certificate_url VARCHAR(500),
    certificate_issued_at TIMESTAMP,

    -- Денормализованные данные для быстрого доступа
    course_title VARCHAR(200),
    instructor_name VARCHAR(100),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(user_id, course_id),
    INDEX idx_enrollments_user (user_id),
    INDEX idx_enrollments_course (course_id),
    INDEX idx_enrollments_status (status),
    INDEX idx_enrollments_completed (completed_at),
    INDEX idx_enrollments_progress (progress_percentage)
);

--changeset s100p:2 (create lesson_progress table)
CREATE TABLE lesson_progress (
    id BIGSERIAL PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL, -- денормализация для быстрых запросов

    -- Прогресс урока
    status VARCHAR(20) DEFAULT 'NOT_STARTED', -- NOT_STARTED, IN_PROGRESS, COMPLETED
    watch_time INTEGER DEFAULT 0, -- время просмотра в секундах
    completion_percentage DECIMAL(5,2) DEFAULT 0.00,

    -- Даты
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Дополнительные данные
    notes TEXT, -- заметки студента
    bookmarks JSON, -- закладки в видео (временные метки)

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE CASCADE,

    UNIQUE(enrollment_id, lesson_id),
    INDEX idx_lesson_progress_enrollment (enrollment_id),
    INDEX idx_lesson_progress_user (user_id),
    INDEX idx_lesson_progress_status (status),
    INDEX idx_lesson_progress_completed (completed_at)
);


--changeset s100p:3 (create quizzes table)
CREATE TABLE quizzes (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT,
    course_id BIGINT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    questions JSON NOT NULL, -- структура вопросов в JSON
    passing_score INTEGER DEFAULT 70, -- минимальный балл для прохождения
    max_attempts INTEGER DEFAULT 3,
    time_limit INTEGER, -- лимит времени в минутах
    is_required BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);


--changeset s100p:4 (create quiz_attempts table)
CREATE TABLE quiz_attempts (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    enrollment_id BIGINT NOT NULL,

    -- Результаты
    answers JSON NOT NULL, -- ответы пользователя
    score INTEGER NOT NULL, -- набранный балл
    max_score INTEGER NOT NULL, -- максимальный возможный балл
    is_passed BOOLEAN NOT NULL,

    -- Время
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NOT NULL,
    time_spent INTEGER, -- время в секундах

    FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE CASCADE,

    INDEX idx_quiz_attempts_quiz (quiz_id),
    INDEX idx_quiz_attempts_user (user_id),
    INDEX idx_quiz_attempts_enrollment (enrollment_id)
);





