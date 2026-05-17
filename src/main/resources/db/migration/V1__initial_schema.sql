-- V1: Initial schema for concert booking system

-- Users table
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    full_name       VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- Concerts table
CREATE TABLE concerts (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    venue           VARCHAR(500),
    starts_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    cover_image_url VARCHAR(1000),
    created_by      BIGINT       NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_concert_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED'))
);

CREATE INDEX idx_concerts_status_starts ON concerts(status, starts_at);

-- Ticket categories table
CREATE TABLE ticket_categories (
    id                  BIGSERIAL PRIMARY KEY,
    concert_id          BIGINT       NOT NULL REFERENCES concerts(id) ON DELETE CASCADE,
    name                VARCHAR(100) NOT NULL,
    price_amount        DECIMAL(15,2) NOT NULL,
    price_currency      VARCHAR(3)   NOT NULL DEFAULT 'VND',
    total_quantity       INT          NOT NULL,
    available_quantity   INT          NOT NULL,
    sort_order           INT          NOT NULL DEFAULT 0,

    CONSTRAINT chk_available_qty CHECK (available_quantity >= 0)
);

CREATE INDEX idx_ticket_cat_concert ON ticket_categories(concert_id);

-- Vouchers table
CREATE TABLE vouchers (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(50)  NOT NULL UNIQUE,
    type                VARCHAR(20)  NOT NULL,
    value               DECIMAL(15,2) NOT NULL,
    min_order_amount    DECIMAL(15,2) DEFAULT 0,
    max_discount_amount DECIMAL(15,2),
    max_uses            INT          NOT NULL DEFAULT 0,
    used_count          INT          NOT NULL DEFAULT 0,
    max_uses_per_user   INT          NOT NULL DEFAULT 1,
    valid_from          TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until         TIMESTAMP WITH TIME ZONE NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT chk_voucher_type CHECK (type IN ('PERCENT', 'FIXED')),
    CONSTRAINT chk_voucher_status CHECK (status IN ('ACTIVE', 'PAUSED', 'EXPIRED')),
    CONSTRAINT chk_used_count CHECK (used_count <= max_uses)
);

-- Bookings table
CREATE TABLE bookings (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users(id),
    concert_id          BIGINT       NOT NULL REFERENCES concerts(id),
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING_PAYMENT',
    subtotal_amount     DECIMAL(15,2) NOT NULL,
    discount_amount     DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(15,2) NOT NULL,
    voucher_id          BIGINT       REFERENCES vouchers(id),
    idempotency_key     VARCHAR(255) NOT NULL,
    hold_expires_at     TIMESTAMP WITH TIME ZONE,
    paid_at             TIMESTAMP WITH TIME ZONE,
    cancelled_at        TIMESTAMP WITH TIME ZONE,
    cancelled_reason    VARCHAR(500),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_booking_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'FAILED', 'CANCELLED', 'EXPIRED', 'REFUNDED')),
    CONSTRAINT uq_user_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_bookings_status_hold ON bookings(status, hold_expires_at);
CREATE INDEX idx_bookings_user_created ON bookings(user_id, created_at DESC);

-- Booking items table
CREATE TABLE booking_items (
    id                  BIGSERIAL PRIMARY KEY,
    booking_id          BIGINT       NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    ticket_category_id  BIGINT       NOT NULL REFERENCES ticket_categories(id),
    quantity            INT          NOT NULL,
    unit_price          DECIMAL(15,2) NOT NULL,
    subtotal            DECIMAL(15,2) NOT NULL
);

CREATE INDEX idx_booking_items_booking ON booking_items(booking_id);

-- Voucher usages table
CREATE TABLE voucher_usages (
    id          BIGSERIAL PRIMARY KEY,
    voucher_id  BIGINT NOT NULL REFERENCES vouchers(id),
    user_id     BIGINT NOT NULL REFERENCES users(id),
    booking_id  BIGINT NOT NULL REFERENCES bookings(id),
    used_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_voucher_booking UNIQUE (voucher_id, booking_id)
);

CREATE INDEX idx_voucher_usage_user ON voucher_usages(voucher_id, user_id);

-- Payments table
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT       NOT NULL REFERENCES bookings(id),
    provider        VARCHAR(50)  NOT NULL,
    provider_txn_id VARCHAR(255) NOT NULL UNIQUE,
    amount          DECIMAL(15,2) NOT NULL,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'VND',
    status          VARCHAR(20)  NOT NULL,
    raw_payload     JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Audit logs table
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    actor_id    BIGINT,
    actor_role  VARCHAR(20),
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(100),
    target_id   VARCHAR(100),
    before_data JSONB,
    after_data  JSONB,
    ip          VARCHAR(50),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_target ON audit_logs(target_type, target_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
