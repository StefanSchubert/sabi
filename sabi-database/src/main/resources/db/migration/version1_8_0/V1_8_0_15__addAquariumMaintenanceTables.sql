/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

-- Per-aquarium maintenance profile: regular water exchange interval and activated carbon setting.
-- The row is created lazily on first update; reads and the AI export fall back to defaults
-- (no regular interval, carbon disabled) when the row is missing.
CREATE TABLE `aquarium_maintenance_profile`
(
    `id`                               BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `aquarium_id`                      BIGINT(20) UNSIGNED NOT NULL,
    `regular_interval_days`            INT                 NULL COMMENT 'Days between regular water changes; must be set together with regular_percentage',
    `regular_percentage`               INT                 NULL COMMENT 'Percentage of tank volume for regular water changes; must be set together with regular_interval_days',
    `carbon_enabled`                   TINYINT(1)          NOT NULL DEFAULT 0,
    `carbon_mode`                      VARCHAR(32)         NULL COMMENT 'CONTINUOUS or INTERVAL; NOT NULL when carbon_enabled, NULL when disabled',
    `carbon_replacement_interval_days` INT                 NULL COMMENT 'Required when carbon_mode = INTERVAL, NULL otherwise',
    `created_on`                       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`                       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`                          INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_aquarium_maintenance_profile_aquarium` (`aquarium_id`),
    CONSTRAINT `fk_aquarium_maintenance_profile_aquarium`
        FOREIGN KEY (`aquarium_id`) REFERENCES `aquarium` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Per-aquarium maintenance profile: regular water change interval and activated carbon setting';

-- One row per recorded water change event. Date-only precision, back-dating allowed,
-- future dates rejected by API validation.
CREATE TABLE `aquarium_water_change`
(
    `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `aquarium_id` BIGINT(20) UNSIGNED NOT NULL,
    `changed_on`  DATE                NOT NULL COMMENT 'Date-only; must not be in the future (API validation)',
    `percentage`  INT                 NOT NULL COMMENT '1-100, percentage of tank volume (API validation)',
    `note`        VARCHAR(200)        NULL,
    `created_on`  TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`  TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`     INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_aquarium_water_change_aquarium_changed_on` (`aquarium_id`, `changed_on`),
    CONSTRAINT `fk_aquarium_water_change_aquarium`
        FOREIGN KEY (`aquarium_id`) REFERENCES `aquarium` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Recorded water change events per aquarium, listed newest first via composite index';
