/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- 005-coral-stock: Adds opt-in flag for including coral stock in the public HouseReef Report.
-- Defaults to FALSE (0) for all existing and new report links, so no existing report is affected.
-- Parallel to the existing include_events column added in V1_6_0_2.

ALTER TABLE `public_report_link`
    ADD COLUMN `include_corals` TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'When 1, active corals are included in the public report; 005-coral-stock';

