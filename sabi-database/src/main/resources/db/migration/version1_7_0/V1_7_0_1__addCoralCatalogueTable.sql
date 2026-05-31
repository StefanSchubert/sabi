/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- 005-coral-stock: Extend existing coral_catalogue table (created in V1_0_0_1) with UGC workflow
-- columns required for PENDING/PUBLIC/REJECTED approval workflow (FR-024 to FR-034).
--
-- NOTE: coral_catalogue already exists since V1_0_0_1 with columns:
--   id, scientific_name VARCHAR(60), description VARCHAR(400), created_on, lastmod_on, optlock
--
-- We extend it here WITHOUT recreating it (Flyway immutability rule).
-- The old `description` column is kept for backward-compatibility (existing data).
-- i18n descriptions go into the new coral_catalogue_i18n table (V1_7_0_2).

-- Step 1: Widen scientific_name to 255 chars (was VARCHAR(60))
ALTER TABLE `coral_catalogue`
    MODIFY COLUMN `scientific_name` VARCHAR(255) NOT NULL;

-- Step 2: Add classification, care_level and UGC workflow columns.
--   DEFAULT values ensure backward compatibility with existing (legacy) rows:
--   - classification: 'SPS' — conservative fallback; admin should update existing entries
--   - care_level:     'MODERATE'
--   - status:         'PUBLIC' — existing catalogue entries remain publicly visible
ALTER TABLE `coral_catalogue`
    ADD COLUMN `classification`   VARCHAR(5)          NOT NULL DEFAULT 'SPS'      COMMENT 'LPS | SPS',
    ADD COLUMN `care_level`       VARCHAR(12)         NOT NULL DEFAULT 'MODERATE'  COMMENT 'EASY | MODERATE | DEMANDING',
    ADD COLUMN `status`           VARCHAR(10)         NOT NULL DEFAULT 'PUBLIC'    COMMENT 'PENDING | PUBLIC | REJECTED',
    ADD COLUMN `proposer_user_id` BIGINT(20) UNSIGNED          NULL               COMMENT 'FK to users.id; NULL for system/legacy entries',
    ADD COLUMN `proposal_date`    DATE                         NULL               COMMENT 'Date the UGC proposal was submitted';

-- Step 3: Indexes for status/proposer lookups
ALTER TABLE `coral_catalogue`
    ADD INDEX `idx_coral_catalogue_status`   (`status`),
    ADD INDEX `idx_coral_catalogue_proposer` (`proposer_user_id`);

-- Step 4: FK to users (nullable — legacy entries have no proposer)
ALTER TABLE `coral_catalogue`
    ADD CONSTRAINT `fk_coral_catalogue_proposer`
        FOREIGN KEY (`proposer_user_id`) REFERENCES `users` (`id`)
        ON DELETE SET NULL;

-- Step 5: Virtual column for partial-unique index
--   Only PENDING and PUBLIC entries compete for a scientific name; REJECTED entries are excluded.
ALTER TABLE `coral_catalogue`
    ADD COLUMN `active_scientific_name` VARCHAR(255)
        GENERATED ALWAYS AS (
            IF(`status` IN ('PENDING', 'PUBLIC'), `scientific_name`, NULL)
        ) VIRTUAL COMMENT 'Used for partial-unique-index on active entries';

CREATE UNIQUE INDEX `uq_coral_catalogue_active_name`
    ON `coral_catalogue` (`active_scientific_name`);

