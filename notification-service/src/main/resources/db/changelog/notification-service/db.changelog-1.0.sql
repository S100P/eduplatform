--liquibase formatted


-- Создание таблиц notification_templates, notifications, notification_preferences

--changeset s100p:1 (create notification_templates table)
CREATE TABLE notification_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    type VARCHAR(20) NOT NULL, -- EMAIL, PUSH, SMS
    subject VARCHAR(200), -- для email
    content TEXT NOT NULL,
    variables JSON, -- описание доступных переменных
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_templates_type ON notification_templates (type);
CREATE INDEX idx_templates_active ON notification_templates (is_active);


--changeset s100p:2 (create notifications table)
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL, -- EMAIL, PUSH, SMS
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SENT, DELIVERED, FAILED, READ

    -- Содержание
    subject VARCHAR(200),
    content TEXT NOT NULL,
    recipient VARCHAR(200) NOT NULL, -- email, phone, push token

    -- Метаданные
    template_id BIGINT,
    template_variables JSON,
    priority VARCHAR(10) DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT

    -- Время отправки
    scheduled_at TIMESTAMP, -- запланированное время отправки
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    failed_at TIMESTAMP,

    -- Ошибки
    failure_reason VARCHAR(500),
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,

    -- Группировка
    notification_group VARCHAR(100), -- для группировки связанных уведомлений
    reference_id VARCHAR(100), -- ссылка на связанную сущность
    reference_type VARCHAR(50), -- ENROLLMENT, PAYMENT, COURSE, etc.

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (template_id) REFERENCES notification_templates(id) ON DELETE SET NULL
);

CREATE INDEX idx_notifications_user ON notifications (user_id);
CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_notifications_type ON notifications (type);
CREATE INDEX idx_notifications_scheduled ON notifications (scheduled_at);
CREATE INDEX idx_notifications_reference ON notifications (reference_type, reference_id);
CREATE INDEX idx_notifications_group ON notifications (notification_group);


--changeset s100p:3 (create notification_preferences table)
CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,

    -- Настройки по типам уведомлений
    email_enabled BOOLEAN DEFAULT true,
    push_enabled BOOLEAN DEFAULT true,
    sms_enabled BOOLEAN DEFAULT false,

    -- Настройки по категориям
    course_updates BOOLEAN DEFAULT true,
    payment_notifications BOOLEAN DEFAULT true,
    marketing_emails BOOLEAN DEFAULT false,
    system_alerts BOOLEAN DEFAULT true,

    -- Время тишины
    quiet_hours_enabled BOOLEAN DEFAULT false,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    timezone VARCHAR(50),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(user_id)
);