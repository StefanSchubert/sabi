/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- V1_5_0_8 — Add fish_size_history table
-- Each record tracks the approximate size (in cm) of a fish at a given date.
-- This enables correlating biomass with water quality measurements over time.
-- Part of 002-fish-stock-catalogue.

CREATE TABLE IF NOT EXISTS `fish_size_history` (
    `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `fish_id`     BIGINT(20) UNSIGNED NOT NULL,
    `measured_on` DATE                NOT NULL,
    `size_cm`     DECIMAL(5,1)        NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_fsh_fish` FOREIGN KEY (`fish_id`)
        REFERENCES `fish` (`id`) ON DELETE CASCADE,
    INDEX `idx_fsh_fish_id` (`fish_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

