-- =====================================================================
--  Accommodation Finder - MySQL 8 schema
--  Author: Kona Sai Akhil (EPITA Paris)
--
--  Generated from the JPA entities, which are the source of truth, and
--  verified by starting the app against it with `ddl-auto: validate`.
--
--  The previous hand-written version had drifted badly: it was missing
--  seven tables entirely (aid_parameters, checklist_items,
--  checklist_progress, guide_articles, moderation_actions, reports,
--  room_risk_flags), missing columns on tables it did define
--  (users.verification_status, rooms.housing_zone), and declared the
--  enum columns as VARCHAR where Hibernate 6.4 emits native ENUM. Any
--  of those made the prod profile refuse to start.
--
--  Regenerate after changing an entity:
--    mvn spring-boot:run -Dspring-boot.run.profiles=dev   # ddl-auto: update
--    mysqldump -u root -p --no-data --compact accommodation_finder
--
--  Usage:  mysql -u root -p < database/schema.sql
--          mysql -u root -p < database/seed.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS accommodation_finder
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE accommodation_finder;

-- Tables are emitted in alphabetical order, so `bookings` is created before the
-- `users` it points at. Deferring the check is what mysqldump itself does.
SET FOREIGN_KEY_CHECKS = 0;

-- NOTE: the CURRENT_TIMESTAMP defaults below are added on top of what
-- Hibernate emits. It never needs them (it sets the values itself), but
-- seed.sql inserts rows without those columns.

CREATE TABLE `aid_parameters` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `effective_from` date DEFAULT NULL,
  `param_key` varchar(80) NOT NULL,
  `label` varchar(200) NOT NULL,
  `last_verified` datetime(6) DEFAULT NULL,
  `source` varchar(500) DEFAULT NULL,
  `param_value` decimal(12,2) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aid_parameters_param_key` (`param_key`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `bookings` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `check_in` date NOT NULL,
  `check_out` date NOT NULL,
  `guests` int(11) NOT NULL,
  `landlord_response` text DEFAULT NULL,
  `message` text DEFAULT NULL,
  `requested_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `status` enum('PENDING','CONFIRMED','REJECTED','CANCELLED','COMPLETED') NOT NULL,
  `total_price` decimal(12,2) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint(20) NOT NULL DEFAULT 0,
  `room_id` bigint(20) NOT NULL,
  `tenant_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_bookings_room` (`room_id`),
  KEY `idx_bookings_tenant` (`tenant_id`),
  KEY `idx_bookings_status` (`status`),
  CONSTRAINT `FKeg03kra5t9odfw0e171p83jf8` FOREIGN KEY (`tenant_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKrgoycol97o21kpjodw1qox4nc` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `checklist_items` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL DEFAULT b'1',
  `category` varchar(60) DEFAULT NULL,
  `essential` bit(1) NOT NULL DEFAULT b'0',
  `explanation` text DEFAULT NULL,
  `if_you_cannot` text DEFAULT NULL,
  `item_key` varchar(60) NOT NULL,
  `non_eu_only` bit(1) NOT NULL DEFAULT b'0',
  `official_link` varchar(500) DEFAULT NULL,
  `sort_order` int(11) NOT NULL,
  `students_only` bit(1) NOT NULL DEFAULT b'0',
  `title` varchar(200) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_checklist_items_item_key` (`item_key`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `checklist_progress` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `done` bit(1) NOT NULL DEFAULT b'0',
  `note` text DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `item_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_progress_user_item` (`user_id`,`item_id`),
  KEY `FKbsbxhdrxgrsvv9el3wthvy4c3` (`item_id`),
  CONSTRAINT `FKbsbxhdrxgrsvv9el3wthvy4c3` FOREIGN KEY (`item_id`) REFERENCES `checklist_items` (`id`),
  CONSTRAINT `FKt9g777n9nq57g6kvn5uhbv2pp` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `favorites` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `room_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorite_user_room` (`user_id`,`room_id`),
  KEY `FKberjnu7ygmw3btmkhf0chgh1f` (`room_id`),
  CONSTRAINT `FKberjnu7ygmw3btmkhf0chgh1f` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `FKk7du8b8ewipawnnpg76d55fus` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `guide_articles` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `body` text DEFAULT NULL,
  `category` varchar(60) DEFAULT NULL,
  `icon` varchar(20) DEFAULT NULL,
  `last_verified` datetime(6) DEFAULT NULL,
  `official_link` varchar(500) DEFAULT NULL,
  `published` bit(1) NOT NULL DEFAULT b'0',
  `slug` varchar(80) NOT NULL,
  `sort_order` int(11) NOT NULL,
  `summary` varchar(300) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_guide_articles_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `messages` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `read_flag` bit(1) NOT NULL DEFAULT b'0',
  `sent_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `recipient_id` bigint(20) NOT NULL,
  `room_id` bigint(20) DEFAULT NULL,
  `sender_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_messages_conversation` (`room_id`,`sender_id`,`recipient_id`),
  KEY `idx_messages_recipient` (`recipient_id`),
  KEY `FK4ui4nnwntodh6wjvck53dbk9m` (`sender_id`),
  CONSTRAINT `FK4ui4nnwntodh6wjvck53dbk9m` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK92hs6y8g4al98ihp4ms6nbxeq` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `FKhdkwfnspwb3s60j27vpg0rpg6` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `moderation_actions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `action` varchar(40) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `reason` text DEFAULT NULL,
  `target_id` bigint(20) DEFAULT NULL,
  `target_type` varchar(20) DEFAULT NULL,
  `moderator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_moderation_created` (`created_at`),
  KEY `FK7poio9ncyp3c58t8h0viqpmuo` (`moderator_id`),
  CONSTRAINT `FK7poio9ncyp3c58t8h0viqpmuo` FOREIGN KEY (`moderator_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `notifications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `body` varchar(500) DEFAULT NULL,
  `collapse_key` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `emailed_at` datetime(6) DEFAULT NULL,
  `link` varchar(255) DEFAULT NULL,
  `read_flag` bit(1) NOT NULL DEFAULT b'0',
  `title` varchar(200) NOT NULL,
  `type` enum('BOOKING_REQUESTED','BOOKING_CONFIRMED','BOOKING_REJECTED','BOOKING_CANCELLED','BOOKING_COMPLETED','NEW_MESSAGE') NOT NULL,
  `recipient_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notifications_recipient_read` (`recipient_id`,`read_flag`),
  KEY `idx_notifications_email_queue` (`read_flag`,`emailed_at`,`created_at`),
  CONSTRAINT `fk_notifications_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `reports` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `details` text DEFAULT NULL,
  `moderator_note` text DEFAULT NULL,
  `reason` enum('SUSPECTED_SCAM','ASKED_FOR_MONEY_UPFRONT','LISTING_DOES_NOT_EXIST','MISLEADING_PHOTOS','DISCRIMINATION','ABUSIVE_BEHAVIOUR','ALREADY_RENTED','OTHER') NOT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `status` enum('OPEN','UNDER_REVIEW','ACTION_TAKEN','DISMISSED') NOT NULL,
  `handled_by_id` bigint(20) DEFAULT NULL,
  `reported_user_id` bigint(20) DEFAULT NULL,
  `reporter_id` bigint(20) DEFAULT NULL,
  `room_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_reports_status` (`status`),
  KEY `idx_reports_room` (`room_id`),
  KEY `FKne0ctdjchw7ad27vxs294v0qn` (`handled_by_id`),
  KEY `FKb3bqi44mjskbnwupr31nfq5ui` (`reported_user_id`),
  KEY `FKd3qiw2om5d2oh5xb7fbdcq225` (`reporter_id`),
  CONSTRAINT `FKb3bqi44mjskbnwupr31nfq5ui` FOREIGN KEY (`reported_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKd3qiw2om5d2oh5xb7fbdcq225` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKne0ctdjchw7ad27vxs294v0qn` FOREIGN KEY (`handled_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKoa85jyt6anp34a7awtuqvht91` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `reviews` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `comment` text DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `rating` int(11) NOT NULL,
  `author_id` bigint(20) NOT NULL,
  `room_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_author_room` (`author_id`,`room_id`),
  KEY `FKoppowk1pob9qiujo31erx63x1` (`room_id`),
  CONSTRAINT `FKoppowk1pob9qiujo31erx63x1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `FKse5kx11600wtv0jh9jobvrdpi` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `room_amenities` (
  `room_id` bigint(20) NOT NULL,
  `amenity` varchar(60) DEFAULT NULL,
  KEY `FKps6ofup9gxhn8juqvproxbaud` (`room_id`),
  CONSTRAINT `FKps6ofup9gxhn8juqvproxbaud` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `room_images` (
  `room_id` bigint(20) NOT NULL,
  `url` varchar(500) DEFAULT NULL,
  `position` int(11) NOT NULL,
  PRIMARY KEY (`room_id`,`position`),
  CONSTRAINT `FKtky1jnwoh1hv50m263p2vlt0y` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `room_risk_flags` (
  `room_id` bigint(20) NOT NULL,
  `flag` varchar(80) DEFAULT NULL,
  KEY `FK4y9ry6ino11a1ekqfobjvae7n` (`room_id`),
  CONSTRAINT `FK4y9ry6ino11a1ekqfobjvae7n` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `rooms` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `available` bit(1) NOT NULL DEFAULT b'1',
  `available_from` date DEFAULT NULL,
  `bathrooms` int(11) DEFAULT NULL,
  `bedrooms` int(11) DEFAULT NULL,
  `bills_included` bit(1) DEFAULT NULL,
  `charges_amount` decimal(10,2) DEFAULT NULL,
  `city` varchar(100) NOT NULL,
  `country` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `deposit` decimal(10,2) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `furnished` bit(1) DEFAULT NULL,
  `hidden` bit(1) NOT NULL DEFAULT b'0',
  `housing_zone` enum('ZONE_1','ZONE_2','ZONE_3') DEFAULT NULL,
  `insee_code` varchar(10) DEFAULT NULL,
  `latitude` decimal(9,6) DEFAULT NULL,
  `longitude` decimal(9,6) DEFAULT NULL,
  `max_guests` int(11) DEFAULT NULL,
  `min_stay_months` int(11) DEFAULT NULL,
  `postal_code` varchar(20) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `risk_score` int(11) DEFAULT NULL,
  `room_type` enum('STUDIO','SHARED','APARTMENT','HOUSE') NOT NULL,
  `size_sqm` int(11) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `verified` bit(1) NOT NULL DEFAULT b'0',
  `version` bigint(20) NOT NULL DEFAULT 0,
  `landlord_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_rooms_city` (`city`),
  KEY `idx_rooms_price` (`price`),
  KEY `idx_rooms_available` (`available`),
  KEY `FKjnuh04e327bnyefv78431g8yx` (`landlord_id`),
  CONSTRAINT `FKjnuh04e327bnyefv78431g8yx` FOREIGN KEY (`landlord_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `user_tokens` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `type` enum('EMAIL_VERIFICATION','PASSWORD_RESET') NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_tokens_hash` (`token_hash`),
  KEY `idx_user_tokens_user_type` (`user_id`,`type`),
  CONSTRAINT `fk_user_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `arrival_date` date DEFAULT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `bio` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `date_of_birth` date DEFAULT NULL,
  `email` varchar(150) NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `email_verified` bit(1) NOT NULL DEFAULT b'0',
  `email_notifications` bit(1) NOT NULL DEFAULT b'1',
  `token_version` int(11) NOT NULL DEFAULT 0,
  `terms_accepted_at` datetime(6) DEFAULT NULL,
  `terms_version` varchar(20) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `eu_national` bit(1) DEFAULT NULL,
  `has_french_guarantor` bit(1) NOT NULL DEFAULT b'0',
  `monthly_income` decimal(10,2) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `nationality` varchar(80) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `profile_completed` bit(1) NOT NULL DEFAULT b'0',
  `role` enum('TENANT','LANDLORD','ADMIN') NOT NULL,
  `scholarship_holder` bit(1) NOT NULL DEFAULT b'0',
  `is_student` bit(1) NOT NULL DEFAULT b'0',
  `university` varchar(150) DEFAULT NULL,
  `university_lat` decimal(9,6) DEFAULT NULL,
  `university_lon` decimal(9,6) DEFAULT NULL,
  `verification_note` varchar(500) DEFAULT NULL,
  `verification_status` enum('NOT_SUBMITTED','PENDING','VERIFIED','REJECTED') NOT NULL,
  `verified_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_users_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET FOREIGN_KEY_CHECKS = 1;
