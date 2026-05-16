/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- 005-coral-stock: Growth measurement history per coral entry (FR-013 to FR-016)
-- Tracks surface area, size, volume, and branch count over time.

CREATE TABLE `coral_growth_history`
(
    `id`                BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `coral_stock_id`    BIGINT(20) UNSIGNED NOT NULL,
    `measured_on`       DATE                NOT NULL,
    `measurement_type`  VARCHAR(30)         NOT NULL COMMENT 'SURFACE_AREA_CM2 | SIZE_CM | VOLUME_CM3 | BRANCH_COUNT',
    `measurement_value` DECIMAL(8,1)        NOT NULL COMMENT 'Positive; BRANCH_COUNT = integer (app-enforced)',
    PRIMARY KEY (`id`),
    INDEX `idx_cgh_coral` (`coral_stock_id`),
    CONSTRAINT `fk_cgh_coral`
        FOREIGN KEY (`coral_stock_id`) REFERENCES `coral_stock` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Growth measurement history per coral; 005-coral-stock';

