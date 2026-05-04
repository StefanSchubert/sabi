/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 *
 * 002-fish-stock-catalogue: Adds an optional free-text departure note
 * to the fish table, allowing users to record remarks when logging out a fish.
 */

ALTER TABLE `fish`
    ADD COLUMN `departure_note` TEXT NULL
        COMMENT 'Optional free-text remark recorded at departure time.'
        AFTER `departure_reason`;

