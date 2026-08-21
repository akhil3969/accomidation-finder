-- =====================================================================
--  Accommodation Finder - MySQL 8 schema
--  Author: Kona Sai Akhil (EPITA Paris)
--
--  The Spring Boot app can create these tables itself (ddl-auto=update in
--  the dev profile). This script is the authoritative reference and is what
--  you should run in production, where ddl-auto is set to `validate`.
--
--  Usage:  mysql -u root -p < database/schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS accommodation_finder
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE accommodation_finder;

-- Dropped in reverse dependency order so the script is re-runnable.
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS favorites;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS room_amenities;
DROP TABLE IF EXISTS room_images;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS users;

-- ---------------------------------------------------------------- users
CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(255) NOT NULL,          -- BCrypt hash
    phone       VARCHAR(30)      NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'TENANT',
    avatar_url  VARCHAR(500)     NULL,
    bio         VARCHAR(500)     NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY idx_users_email (email),
    CONSTRAINT chk_users_role CHECK (role IN ('TENANT', 'LANDLORD', 'ADMIN'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------- rooms
CREATE TABLE rooms (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200)  NOT NULL,
    description     TEXT              NULL,
    city            VARCHAR(100)  NOT NULL,
    address         VARCHAR(255)      NULL,
    postal_code     VARCHAR(20)       NULL,
    country         VARCHAR(100)      NULL DEFAULT 'France',
    price           DECIMAL(10, 2) NOT NULL,
    deposit         DECIMAL(10, 2)    NULL,
    size_sqm        INT               NULL,
    bedrooms        INT               NULL DEFAULT 1,
    bathrooms       INT               NULL DEFAULT 1,
    max_guests      INT               NULL DEFAULT 1,
    available       BOOLEAN       NOT NULL DEFAULT TRUE,
    furnished       BOOLEAN           NULL DEFAULT TRUE,
    bills_included  BOOLEAN           NULL DEFAULT FALSE,
    available_from  DATE              NULL,
    min_stay_months INT               NULL DEFAULT 1,
    room_type       VARCHAR(20)   NOT NULL DEFAULT 'STUDIO',
    landlord_id     BIGINT        NOT NULL,
    latitude        DECIMAL(9, 6)     NULL,
    longitude       DECIMAL(9, 6)     NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME          NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_rooms_city (city),
    KEY idx_rooms_price (price),
    KEY idx_rooms_available (available),
    CONSTRAINT fk_rooms_landlord FOREIGN KEY (landlord_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_rooms_type CHECK (room_type IN ('STUDIO', 'SHARED', 'APARTMENT', 'HOUSE')),
    CONSTRAINT chk_rooms_price CHECK (price > 0)
) ENGINE = InnoDB;

-- Photo URLs, ordered. Matches the @ElementCollection on Room.images.
CREATE TABLE room_images (
    room_id  BIGINT       NOT NULL,
    url      VARCHAR(500)     NULL,
    position INT          NOT NULL,
    PRIMARY KEY (room_id, position),
    CONSTRAINT fk_room_images_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE room_amenities (
    room_id BIGINT      NOT NULL,
    amenity VARCHAR(60) NOT NULL,
    PRIMARY KEY (room_id, amenity),
    CONSTRAINT fk_room_amenities_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ------------------------------------------------------------- bookings
CREATE TABLE bookings (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    room_id           BIGINT      NOT NULL,
    tenant_id         BIGINT      NOT NULL,
    check_in          DATE        NOT NULL,
    check_out         DATE        NOT NULL,
    guests            INT         NOT NULL DEFAULT 1,
    message           TEXT            NULL,
    landlord_response TEXT            NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_price       DECIMAL(12, 2)  NULL,
    requested_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_bookings_room (room_id),
    KEY idx_bookings_tenant (tenant_id),
    KEY idx_bookings_status (status),
    CONSTRAINT fk_bookings_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_tenant FOREIGN KEY (tenant_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_bookings_dates CHECK (check_out > check_in)
) ENGINE = InnoDB;

-- ------------------------------------------------------------- messages
CREATE TABLE messages (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    sender_id    BIGINT   NOT NULL,
    recipient_id BIGINT   NOT NULL,
    room_id      BIGINT       NULL,
    content      TEXT     NOT NULL,
    read_flag    BOOLEAN  NOT NULL DEFAULT FALSE,
    sent_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_messages_conversation (room_id, sender_id, recipient_id),
    KEY idx_messages_recipient (recipient_id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE SET NULL
) ENGINE = InnoDB;

-- ------------------------------------------------------------ favorites
CREATE TABLE favorites (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    room_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite_user_room (user_id, room_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- -------------------------------------------------------------- reviews
CREATE TABLE reviews (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    room_id    BIGINT   NOT NULL,
    author_id  BIGINT   NOT NULL,
    rating     INT      NOT NULL,
    comment    TEXT         NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_author_room (author_id, room_id),
    CONSTRAINT fk_reviews_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB;
