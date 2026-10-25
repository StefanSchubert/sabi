/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- sabi-69: Email measurement reminders
-- Adds email reminder settings to the users table and a delivery record table
-- used for per-cycle idempotency (unique user_id + cycle_date) and audit.

ALTER TABLE `users`
    ADD COLUMN `email_reminder_enabled` BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'Opt-in flag for email measurement reminders',
    ADD COLUMN `email_reminder_interval_days` INT NOT NULL DEFAULT 7
        COMMENT 'Reminder cadence in days (1-30)',
    ADD COLUMN `email_reminder_next_send_date` DATE NULL
        COMMENT 'Next scheduled send date; NULL while disabled';

CREATE INDEX idx_users_email_reminder_due
    ON `users` (`email_reminder_enabled`, `email_reminder_next_send_date`);

CREATE TABLE reminder_delivery_record (
    id           BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id      BIGINT(20) UNSIGNED NOT NULL,
    cycle_date   DATE                NOT NULL,
    status       VARCHAR(10)         NOT NULL COMMENT 'SENT | FAILED',
    attempts     INT                 NOT NULL DEFAULT 0,
    sent_at      DATETIME                    NULL,
    created_on   DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmod_on   DATETIME                    NULL ON UPDATE CURRENT_TIMESTAMP,
    optlock      INT                 NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_reminder_delivery_user_cycle UNIQUE (user_id, cycle_date),
    CONSTRAINT fk_reminder_delivery_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
