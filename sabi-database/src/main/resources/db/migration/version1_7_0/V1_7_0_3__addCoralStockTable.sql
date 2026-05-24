/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- Old unsued datastructure from early prototyping; remove if exists for clean slate.
drop table if  exists `coral`;

-- 005-coral-stock: Main coral stock table (analogue to fish table)
-- Supports soft-delete via deleted_at for aquarium cascade deletion.

CREATE TABLE `coral_stock`
(
    `id`                  BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `aquarium_id`         BIGINT(20) UNSIGNED NOT NULL,
    `user_id`             BIGINT(20) UNSIGNED NOT NULL                    COMMENT 'Ownership: FK to users.id',
    `coral_catalogue_id`  BIGINT(20) UNSIGNED NULL                        COMMENT 'Optional FK to coral_catalogue.id; ON DELETE SET NULL',
    `species_name`        VARCHAR(255)        NOT NULL                    COMMENT 'Free-text common/species name (mandatory)',
    `scientific_name`     VARCHAR(255)        NULL                        COMMENT 'Snapshot from catalogue at link time; user-editable',
    `classification`      VARCHAR(5)          NULL                        COMMENT 'LPS | SPS snapshot; user-editable',
    `care_level`          VARCHAR(12)         NULL                        COMMENT 'EASY | MODERATE | DEMANDING snapshot',
    `external_ref_url`    VARCHAR(512)        NULL,
    `notes`               TEXT                NULL,
    `added_on`            DATE                NOT NULL,
    `departed_on`         DATE                NULL                        COMMENT 'NULL = currently present',
    `departure_reason`    VARCHAR(30)         NULL                        COMMENT 'DIED | SOLD | GIVEN_AWAY | MOVED_TO_OTHER_TANK | OTHER',
    `departure_note`      TEXT                NULL                        COMMENT 'max 500 chars enforced at app layer',
    `deleted_at`          TIMESTAMP           NULL                        COMMENT 'Soft-delete; set on aquarium deletion',
    `created_on`          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`             INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_cs_aquarium`  (`aquarium_id`),
    INDEX `idx_cs_user`      (`user_id`),
    INDEX `idx_cs_deleted_at` (`deleted_at`),
    CONSTRAINT `fk_cs_aquarium`
        FOREIGN KEY (`aquarium_id`) REFERENCES `aquarium` (`id`)
        ON DELETE CASCADE,
    CONSTRAINT `fk_cs_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_cs_catalogue`
        FOREIGN KEY (`coral_catalogue_id`) REFERENCES `coral_catalogue` (`id`)
        ON DELETE SET NULL
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Coral stock per aquarium; soft-delete via deleted_at; 005-coral-stock';

