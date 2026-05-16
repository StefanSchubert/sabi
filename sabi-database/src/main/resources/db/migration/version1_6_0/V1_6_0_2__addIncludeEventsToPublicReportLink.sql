/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- Adds opt-in flag for including aquarium events in the public HouseReef Report.
-- Defaults to FALSE (0) for all existing and new report links, so no existing report is affected.

ALTER TABLE `public_report_link`
    ADD COLUMN `include_events` TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'When 1, events (past 365 days) are included in the public report';

