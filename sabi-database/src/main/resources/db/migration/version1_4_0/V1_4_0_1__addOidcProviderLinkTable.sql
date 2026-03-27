/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- sabi-150: OpenID Connect Login via Google
-- Creates the oidc_provider_link table that links Sabi user accounts to OIDC provider identities.
-- GDPR: provider_subject is personal data; deleted automatically via CASCADE on user removal.

CREATE TABLE `oidc_provider_link`
(
    `id`               BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT(20) NOT NULL,
    `provider`         VARCHAR(20) NOT NULL COMMENT 'GOOGLE | APPLE | MICROSOFT',
    `provider_subject` VARCHAR(255) NOT NULL COMMENT 'Immutable sub claim from ID token',
    `linked_at`        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_on`       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `optlock`          INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_provider_subject` (`provider`, `provider_subject`),
    UNIQUE KEY `uq_user_provider`    (`user_id`, `provider`),
    CONSTRAINT `fk_oidc_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'GDPR: provider_subject is personal data; deleted via CASCADE on user removal';

