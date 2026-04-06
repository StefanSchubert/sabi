/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- 002-fish-stock-catalogue: Metadaten für Fisch-Fotos (Bytes auf Filesystem, C-4)

CREATE TABLE `fish_photo`
(
    `id`           BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `fish_id`      BIGINT(20) UNSIGNED NOT NULL,
    `file_path`    VARCHAR(512)        NOT NULL COMMENT 'Path relative to sabi.fish.photo.storage-dir',
    `content_type` VARCHAR(50)         NOT NULL COMMENT 'image/jpeg | image/png | image/webp | image/gif',
    `file_size`    BIGINT UNSIGNED     NOT NULL COMMENT 'Bytes',
    `upload_date`  DATE                NOT NULL,
    `created_on`   TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`   TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`      INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_fish_photo` (`fish_id`),
    CONSTRAINT `fk_fish_photo_fish`
        FOREIGN KEY (`fish_id`) REFERENCES `fish` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Fish photo metadata; actual bytes on filesystem (C-4, FR-025)';

