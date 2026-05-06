-- Public report link: maps a share-token to an aquarium with optional expiry.
-- A user may have at most one active link per aquarium (enforced by UNIQUE on aquarium_id).
-- Generating a new link replaces the previous one (the token column is updated).

CREATE TABLE `public_report_link`
(
    `id`           BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `aquarium_id`  BIGINT(20) UNSIGNED NOT NULL,
    `link_token`   VARCHAR(36)         NOT NULL COMMENT 'UUID v4 share token',
    `valid_until`  DATETIME            NULL     COMMENT 'NULL = no expiry',
    `created_on`   TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`   TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`      INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_public_report_aquarium` (`aquarium_id`),
    UNIQUE KEY `uq_public_report_token`    (`link_token`),
    CONSTRAINT `fk_public_report_aquarium`
        FOREIGN KEY (`aquarium_id`) REFERENCES `aquarium` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Public share links for HouseReef reports; one active link per aquarium';
