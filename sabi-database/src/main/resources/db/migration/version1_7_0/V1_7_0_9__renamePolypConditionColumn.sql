/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- MariaDB 12: 'condition' is a reserved keyword and cannot be used as an unquoted column name.
-- Rename the column to 'polyp_condition' to avoid SQLSyntaxErrorException on INSERT.
-- IF EXISTS makes this idempotent: no-op if the column was already renamed manually.
ALTER TABLE coral_polyp_condition
    CHANGE COLUMN IF EXISTS `condition` `polyp_condition` VARCHAR(30) NOT NULL;

