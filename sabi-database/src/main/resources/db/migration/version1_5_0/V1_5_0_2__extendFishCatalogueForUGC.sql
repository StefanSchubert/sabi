/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- 002-fish-stock-catalogue: UGC-Workflow für fish_catalogue

ALTER TABLE `fish_catalogue`
    ADD COLUMN `status`          VARCHAR(10)  NOT NULL DEFAULT 'PUBLIC'
                                 COMMENT 'PENDING | PUBLIC | REJECTED' AFTER `id`,
    ADD COLUMN `proposer_user_id` BIGINT(20) UNSIGNED NULL
                                 COMMENT 'FK to users.id; NULL for legacy/system entries' AFTER `status`,
    ADD COLUMN `proposal_date`   DATE         NULL
                                 COMMENT 'Date the UGC proposal was submitted' AFTER `proposer_user_id`,
    MODIFY COLUMN `scientific_name` VARCHAR(255) NULL;

-- Unique-Constraint via Virtual Column (PENDING + PUBLIC Einträge sperren den Namen)
ALTER TABLE `fish_catalogue`
    ADD COLUMN `active_scientific_name` VARCHAR(255)
        GENERATED ALWAYS AS (
            IF(`status` IN ('PENDING', 'PUBLIC'), `scientific_name`, NULL)
        ) VIRTUAL COMMENT 'Used for partial-unique-index on active entries';

CREATE UNIQUE INDEX `uq_fish_catalogue_active_name`
    ON `fish_catalogue` (`active_scientific_name`);

-- Index für schnelle Status-Abfragen
CREATE INDEX `idx_fish_catalogue_status` ON `fish_catalogue` (`status`);
CREATE INDEX `idx_fish_catalogue_proposer` ON `fish_catalogue` (`proposer_user_id`);

-- FK für proposer_user_id (optional; NULL für Legacy-Einträge)
ALTER TABLE `fish_catalogue`
    ADD CONSTRAINT `fk_fish_catalogue_proposer`
        FOREIGN KEY (`proposer_user_id`) REFERENCES `users` (`id`)
        ON DELETE SET NULL;

