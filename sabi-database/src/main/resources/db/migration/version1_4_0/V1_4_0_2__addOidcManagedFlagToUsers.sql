/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- sabi-150: OpenID Connect Login via Google
-- Adds oidc_managed flag to users table.
-- oidc_managed = 1 means the account was auto-provisioned via OIDC and has no local password.

ALTER TABLE `users`
    ADD COLUMN `oidc_managed` TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '1 if account was auto-provisioned via OIDC and has no local password';

