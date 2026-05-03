/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- 002-fish-stock-catalogue: Datenmigration bestehende Katalogdaten → i18n-Tabelle
-- Bestehende description und meerwasserwiki_url werden als EN-Einträge migriert.

INSERT INTO `fish_catalogue_i18n` (`catalogue_id`, `language_code`, `common_name`, `description`, `reference_url`)
SELECT `id`,
       'en',
       NULL,
       NULLIF(`description`, ''),
       NULLIF(`meerwasserwiki_url`, '')
FROM `fish_catalogue`
WHERE `description` IS NOT NULL
   OR `meerwasserwiki_url` IS NOT NULL;

-- Hinweis: Die alten Spalten description und meerwasserwiki_url in fish_catalogue
-- werden in Version 1.6.0 entfernt. Bis dahin sind sie @Deprecated im Code markiert.

