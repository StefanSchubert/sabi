/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- V1_7_0_8: Extend classification column from VARCHAR(5) to VARCHAR(25)
--            to accommodate new coral classification values:
--            SOFT_CORAL, GORGONIAN, DISC_ANEMONE, ENCRUSTING_ANEMONE, ANEMONE
-- Affected tables: coral_catalogue, coral_stock_entry

ALTER TABLE coral_catalogue
    MODIFY COLUMN `classification` VARCHAR(25) NOT NULL DEFAULT 'SPS' COMMENT 'LPS | SPS | SOFT_CORAL | GORGONIAN | DISC_ANEMONE | ENCRUSTING_ANEMONE | ANEMONE';

ALTER TABLE coral_stock
    MODIFY COLUMN `classification` VARCHAR(25) NULL COMMENT 'LPS | SPS | SOFT_CORAL | GORGONIAN | DISC_ANEMONE | ENCRUSTING_ANEMONE | ANEMONE snapshot; user-editable';

