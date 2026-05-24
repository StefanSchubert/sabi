/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- 005-coral-stock: Localised fields for coral catalogue entries (mirrors fish_catalogue_i18n)

CREATE TABLE `coral_catalogue_i18n`
(
    `id`            BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `catalogue_id`  BIGINT(20) UNSIGNED NOT NULL,
    `language_code` VARCHAR(2)          NOT NULL COMMENT 'de | en | es | fr | it',
    `common_name`   VARCHAR(255)        NULL,
    `description`   TEXT                NULL     COMMENT 'max 2000 chars enforced at app layer',
    `reference_url` VARCHAR(512)        NULL,
    `created_on`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`       INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_coral_catalogue_i18n_lang` (`catalogue_id`, `language_code`),
    CONSTRAINT `fk_coral_i18n_entry`
        FOREIGN KEY (`catalogue_id`) REFERENCES `coral_catalogue` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Localised fields for coral catalogue entries; 005-coral-stock';

-- FULLTEXT index for search by common_name (FR-030)
CREATE FULLTEXT INDEX `ft_coral_i18n_name` ON `coral_catalogue_i18n` (`common_name`);

