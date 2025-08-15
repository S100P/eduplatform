--liquibase formatted


-- Создание таблиц categories, courses, course_categories, lessons

--changeset s100p:1 (create categories  table)
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    slug VARCHAR(100) UNIQUE NOT NULL,
    parent_id BIGINT, -- для иерархии категорий
    is_active BOOLEAN DEFAULT true,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL,
    INDEX idx_categories_slug (slug),
    INDEX idx_categories_parent (parent_id),
    INDEX idx_categories_active (is_active)
);

--changeset s100p:2 (create courses table)
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200) UNIQUE NOT NULL,
    description TEXT,
    short_description VARCHAR(500),
    instructor_id BIGINT NOT NULL, -- ссылка на user из User Service
    instructor_name VARCHAR(100) NOT NULL, -- денормализация для производительности

    -- Ценообразование
    price DECIMAL(10,2) DEFAULT 0.00,
    discount_price DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'USD',

    -- Метаданные курса
    duration_hours INTEGER, -- продолжительность в часах
    difficulty_level VARCHAR(20), -- BEGINNER, INTERMEDIATE, ADVANCED
    language VARCHAR(10) DEFAULT 'en',

    -- Медиа
    thumbnail_url VARCHAR(500),
    preview_video_url VARCHAR(500),

    -- Статус и публикация
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, ARCHIVED
    is_featured BOOLEAN DEFAULT false,
    published_at TIMESTAMP,

    -- SEO
    meta_title VARCHAR(200),
    meta_description VARCHAR(300),
    meta_keywords VARCHAR(500),

    -- Статистика
    enrollment_count INTEGER DEFAULT 0,
    rating_average DECIMAL(3,2) DEFAULT 0.00,
    rating_count INTEGER DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Индексы
    INDEX idx_courses_instructor (instructor_id),
    INDEX idx_courses_status (status),
    INDEX idx_courses_slug (slug),
    INDEX idx_courses_featured (is_featured),
    INDEX idx_courses_published (published_at),
    INDEX idx_courses_price (price),
    FULLTEXT INDEX idx_courses_search (title, description, short_description)
);


--changeset s100p:3 (create course_categories table)
CREATE TABLE course_categories (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,

    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,

    UNIQUE(course_id, category_id),
    INDEX idx_course_categories_course (course_id),
    INDEX idx_course_categories_category (category_id)
);


--changeset s100p:4 (create lessons table)
CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    description TEXT,
    content TEXT, -- HTML контент урока

    -- Порядок и структура
    sort_order INTEGER NOT NULL,
    section_name VARCHAR(100), -- группировка уроков по секциям

    -- Медиа контент
    video_url VARCHAR(500),
    video_duration INTEGER, -- в секундах

    -- Настройки доступа
    is_free BOOLEAN DEFAULT false, -- доступен ли без покупки курса
    is_published BOOLEAN DEFAULT true,

    -- Требования для завершения
    min_watch_time INTEGER, -- минимальное время просмотра для засчитывания

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,

    UNIQUE(course_id, slug),
    INDEX idx_lessons_course (course_id),
    INDEX idx_lessons_order (course_id, sort_order),
    INDEX idx_lessons_published (is_published)
);


--changeset s100p:5 (create lesson_resources table)
CREATE TABLE lesson_resources (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    resource_type VARCHAR(50) NOT NULL, -- PDF, VIDEO, LINK, DOWNLOAD
    resource_url VARCHAR(500) NOT NULL,
    file_size BIGINT, -- размер файла в байтах
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
    INDEX idx_lesson_resources_lesson (lesson_id)
);


