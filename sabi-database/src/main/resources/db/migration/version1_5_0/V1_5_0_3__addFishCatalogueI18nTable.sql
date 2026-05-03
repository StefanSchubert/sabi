/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- 002-fish-stock-catalogue: Lokalisierte Felder für Katalog-Einträge

CREATE TABLE `fish_catalogue_i18n`
(
    `id`               BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `catalogue_id`     BIGINT(20) UNSIGNED NOT NULL,
    `language_code`    VARCHAR(2)          NOT NULL COMMENT 'de | en | es | fr | it',
    `common_name`      VARCHAR(255)        NULL,
    `description`      TEXT                NULL     COMMENT 'max 2000 chars enforced at app layer',
    `reference_url`    VARCHAR(512)        NULL,
    `created_on`       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`          INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_catalogue_i18n_lang` (`catalogue_id`, `language_code`),
    CONSTRAINT `fk_catalogue_i18n_entry`
        FOREIGN KEY (`catalogue_id`) REFERENCES `fish_catalogue` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Localized fields for fish catalogue entries';

-- Index für Suche nach common_name
CREATE FULLTEXT INDEX `ft_i18n_common_name` ON `fish_catalogue_i18n` (`common_name`);

