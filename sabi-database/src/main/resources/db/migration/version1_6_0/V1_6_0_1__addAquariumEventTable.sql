/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- Aquarium Event Logbook: free-form events per aquarium (feature 004-aquarium-events).
-- Each event captures a mandatory date, optional duration in hours, and mandatory description.
-- Sorted newest-first for authenticated logbook display (eventDate DESC).

CREATE TABLE `aquarium_event`
(
    `id`             BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `aquarium_id`    BIGINT(20) UNSIGNED NOT NULL,
    `event_date`     DATE                NOT NULL                    COMMENT 'Calendar date the event occurred (no time component)',
    `duration_hours` DECIMAL(6,2)        NULL                        COMMENT 'Optional duration in hours; must be > 0 when present',
    `description`    TEXT                NOT NULL                    COMMENT 'Free-form multi-line description; line breaks preserved',
    `created_on`     TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`     TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`        INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_aquarium_event_aquarium_date` (`aquarium_id`, `event_date`),
    CONSTRAINT `fk_aquarium_event_aquarium`
        FOREIGN KEY (`aquarium_id`) REFERENCES `aquarium` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Free-form logbook events per aquarium; feature 004-aquarium-events';

