/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- sabi-dark-mode: Dark Mode user preference
-- Adds dark_mode flag to users table.
-- dark_mode = 1 means the user has activated the dark mode.

ALTER TABLE `users`
    ADD COLUMN `dark_mode` TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '1 if user has activated dark mode for the UI';
