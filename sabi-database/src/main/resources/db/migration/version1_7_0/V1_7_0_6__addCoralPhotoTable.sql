/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- 005-coral-stock: Coral photo metadata (actual bytes on filesystem, C-4)
-- One photo per coral entry (unique key on coral_stock_id).

CREATE TABLE `coral_photo`
(
    `id`             BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `coral_stock_id` BIGINT(20) UNSIGNED NOT NULL,
    `file_path`      VARCHAR(512)        NOT NULL COMMENT 'Path relative to sabi.photo.dir/coral/',
    `content_type`   VARCHAR(50)         NOT NULL COMMENT 'image/jpeg | image/png | image/webp | image/gif',
    `upload_date`    DATE                NOT NULL,
    `created_on`     TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`     TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`        INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_coral_photo_stock` (`coral_stock_id`),
    CONSTRAINT `fk_cp_coral_stock`
        FOREIGN KEY (`coral_stock_id`) REFERENCES `coral_stock` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Coral photo metadata; actual bytes on filesystem (C-4); 005-coral-stock';

