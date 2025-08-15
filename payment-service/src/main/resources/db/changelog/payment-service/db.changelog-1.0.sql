--liquibase formatted


-- Создание таблиц payments, payment_refunds, discount_coupons, coupon_usage

--changeset s100p:1 (create payments table)
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrollment_id BIGINT, -- связь с записью на курс

    -- Сумма и валюта
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    discount_amount DECIMAL(10,2) DEFAULT 0.00,
    tax_amount DECIMAL(10,2) DEFAULT 0.00,
    total_amount DECIMAL(10,2) NOT NULL,

    -- Статус платежа
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED, CANCELLED, REFUNDED

    -- Информация о платеже
    payment_method VARCHAR(50), -- CARD, PAYPAL, BANK_TRANSFER, CRYPTO
    payment_provider VARCHAR(50), -- STRIPE, PAYPAL, RAZORPAY
    external_payment_id VARCHAR(200), -- ID от внешнего провайдера
    external_transaction_id VARCHAR(200),

    -- Детали карты (только последние 4 цифры)
    card_last_four VARCHAR(4),
    card_brand VARCHAR(20), -- VISA, MASTERCARD, AMEX

    -- Даты
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    failed_at TIMESTAMP,
    refunded_at TIMESTAMP,

    -- Дополнительная информация
    failure_reason VARCHAR(500),
    payment_description VARCHAR(200),
    notes TEXT,

    -- Денормализованные данные
    course_title VARCHAR(200),
    user_email VARCHAR(100),

    INDEX idx_payments_user (user_id),
    INDEX idx_payments_course (course_id),
    INDEX idx_payments_status (status),
    INDEX idx_payments_provider (payment_provider),
    INDEX idx_payments_created (created_at),
    INDEX idx_payments_external (external_payment_id)
);


--changeset s100p:2 (create payment_refunds table)
CREATE TABLE payment_refunds (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, COMPLETED, FAILED
    external_refund_id VARCHAR(200),
    processed_by BIGINT, -- admin user ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,

    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    INDEX idx_payment_refunds_payment (payment_id),
    INDEX idx_payment_refunds_status (status)
);


--changeset s100p:3 (create discount_coupons table)
CREATE TABLE discount_coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(200),

    -- Тип скидки
    discount_type VARCHAR(20) NOT NULL, -- PERCENTAGE, FIXED_AMOUNT
    discount_value DECIMAL(10,2) NOT NULL,

    -- Ограничения
    minimum_amount DECIMAL(10,2), -- минимальная сумма заказа
    maximum_discount DECIMAL(10,2), -- максимальная сумма скидки
    usage_limit INTEGER, -- общий лимит использований
    usage_limit_per_user INTEGER, -- лимит на пользователя
    usage_count INTEGER DEFAULT 0,

    -- Применимость
    applicable_courses JSON, -- массив ID курсов или null для всех
    applicable_categories JSON, -- массив ID категорий

    -- Даты действия
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,

    is_active BOOLEAN DEFAULT true,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_coupons_code (code),
    INDEX idx_coupons_active (is_active),
    INDEX idx_coupons_dates (valid_from, valid_until)
);


--changeset s100p:4 (create coupon_usage table)
CREATE TABLE coupon_usage (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (coupon_id) REFERENCES discount_coupons(id) ON DELETE CASCADE,
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,

    INDEX idx_coupon_usage_coupon (coupon_id),
    INDEX idx_coupon_usage_user (user_id),
    INDEX idx_coupon_usage_payment (payment_id)
);





