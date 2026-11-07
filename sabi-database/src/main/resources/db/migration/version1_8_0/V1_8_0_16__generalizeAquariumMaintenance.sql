/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

-- Generalize the water change event table to cover all recorded maintenance events:
-- water changes (percentage of tank volume) and carbon replacements (date + note only).
-- Existing rows are water changes by definition, hence the NOT NULL default.
RENAME TABLE `aquarium_water_change` TO `aquarium_maintenance`;

ALTER TABLE `aquarium_maintenance`
    ADD COLUMN `maintenance_type` VARCHAR(32) NOT NULL DEFAULT 'WATER_CHANGE'
        COMMENT 'WATER_CHANGE or CARBON_REPLACEMENT (API validation)' AFTER `aquarium_id`;

ALTER TABLE `aquarium_maintenance`
    MODIFY COLUMN `percentage` INT NULL
        COMMENT '1-100, percentage of tank volume; required for WATER_CHANGE, NULL for CARBON_REPLACEMENT (API validation)';
