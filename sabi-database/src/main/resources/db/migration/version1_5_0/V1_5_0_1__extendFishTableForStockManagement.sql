/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- 002-fish-stock-catalogue: TankFishStock — erweitert bestehende fish-Tabelle
-- Fügt Pflichtfeld common_name, optionale Felder, Departure-Semantik und Soft-Delete hinzu

-- 1. Pflichtfelder hinzufügen (vorerst mit DEFAULT für bestehende Datensätze)
ALTER TABLE `fish`
    ADD COLUMN `common_name`       VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Species common name (free text, mandatory)' AFTER `nickname`,
    ADD COLUMN `scientific_name`   VARCHAR(255) NULL     COMMENT 'Snapshot from catalogue at link time; user-editable' AFTER `common_name`,
    ADD COLUMN `external_ref_url`  VARCHAR(512) NULL     COMMENT 'Optional external reference URL' AFTER `scientific_name`,
    ADD COLUMN `departure_reason`  VARCHAR(30)  NULL     COMMENT 'DECEASED | REMOVED_REHOMED | UNKNOWN' AFTER `exodus_on`,
    ADD COLUMN `deleted_at`        TIMESTAMP    NULL     DEFAULT NULL COMMENT 'Soft-delete; set when parent aquarium is deleted' AFTER `lastmod_on`,
    MODIFY COLUMN `fish_catalogue_id` BIGINT(20) UNSIGNED NULL COMMENT 'Optional FK to fish_catalogue; NULL = free-text only';

-- 2. common_name aus nickname befüllen (Datenmigration für Bestandsdaten)
UPDATE `fish` SET `common_name` = COALESCE(NULLIF(`nickname`, ''), 'Unknown') WHERE `common_name` = '';

-- 3. DEFAULT entfernen (Pflichtfeld ab jetzt)
ALTER TABLE `fish` MODIFY COLUMN `common_name` VARCHAR(255) NOT NULL;

-- 4. Index für Soft-Delete-Filter
CREATE INDEX `idx_fish_deleted_at` ON `fish` (`deleted_at`);

