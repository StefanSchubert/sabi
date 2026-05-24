/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- 005-coral-stock: Polyp condition observation history per coral entry (FR-017 to FR-020)
-- Tracks VITAL, TISSUE_LOSS, PALE, LIMP, SIGNIFICANT_GROWTH conditions over time.

CREATE TABLE `coral_polyp_condition`
(
    `id`             BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `coral_stock_id` BIGINT(20) UNSIGNED NOT NULL,
    `observed_on`    DATE                NOT NULL,
    `condition`      VARCHAR(30)         NOT NULL COMMENT 'VITAL | TISSUE_LOSS | PALE | LIMP | SIGNIFICANT_GROWTH',
    PRIMARY KEY (`id`),
    INDEX `idx_cpc_coral` (`coral_stock_id`),
    CONSTRAINT `fk_cpc_coral`
        FOREIGN KEY (`coral_stock_id`) REFERENCES `coral_stock` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Polyp condition observation history per coral; 005-coral-stock';

