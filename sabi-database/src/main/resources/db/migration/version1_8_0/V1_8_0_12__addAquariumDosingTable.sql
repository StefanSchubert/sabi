/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

-- Restore aquarium_event to its original generic form. V1_8_0_11 is immutable, so its
-- prematurely-added structured dosing columns must be removed by this follow-up migration.
ALTER TABLE `aquarium_event`
    DROP COLUMN `dosing_end_on`,
    DROP COLUMN `note`,
    DROP COLUMN `solution_description`,
    DROP COLUMN `dosing_method`,
    DROP COLUMN `dosing_interval`,
    DROP COLUMN `category`,
    DROP COLUMN `product_name`,
    DROP COLUMN `amount_unit`,
    DROP COLUMN `amount`,
    DROP COLUMN `event_time`,
    DROP COLUMN `event_type`;

-- Standalone manual and automated dosing records. This intentionally does not extend aquarium_event.
CREATE TABLE `aquarium_dosing`
(
    `id`                   BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `aquarium_id`          BIGINT(20) UNSIGNED NOT NULL,
    `recorded_on`          DATETIME            NOT NULL COMMENT 'Start or recording timestamp',
    `dosing_type`          VARCHAR(32)         NOT NULL COMMENT 'MANUAL_ADDITION or AUTOMATED_DOSING',
    `product_name`         VARCHAR(255)        NOT NULL,
    `amount`               DECIMAL(10,3)       NOT NULL COMMENT 'Must be positive; validated by API',
    `amount_unit`          VARCHAR(30)         NOT NULL,
    `category`             VARCHAR(80)         NULL,
    `dosing_interval`      VARCHAR(40)         NULL COMMENT 'Required for automated dosing',
    `dosing_method`        VARCHAR(80)         NULL,
    `solution_description` TEXT                NULL,
    `note`                 TEXT                NULL,
    `dosing_end_on`        DATETIME            NULL,
    `created_on`           TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`           TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`              INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_aquarium_dosing_aquarium_recorded_on` (`aquarium_id`, `recorded_on`),
    CONSTRAINT `fk_aquarium_dosing_aquarium`
        FOREIGN KEY (`aquarium_id`) REFERENCES `aquarium` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Standalone manual and automated aquarium dosing records';
