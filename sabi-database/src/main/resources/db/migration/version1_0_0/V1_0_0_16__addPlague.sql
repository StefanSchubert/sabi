CREATE TABLE `plague`
(
    `id`              BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `scientific_name` VARCHAR(80)         NOT NULL,
    `created_on`      TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`      TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `optlock`         INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8;

CREATE TABLE `localized_plague`
(
    `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `plague_id`   BIGINT(20) UNSIGNED NOT NULL,
    `common_name` VARCHAR(80)         NOT NULL,
    `language`    VARCHAR(3)          NOT NULL,
    `created_on`  TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`  TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `optlock`     INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `business_key` (`plague_id`, `language`),
    foreign key (plague_id) references plague (id)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8;

/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

-- Basic Data:
INSERT INTO plague (id, scientific_name, created_on, lastmod_on)
VALUES (1, 'Cyanobacteria', '2022-06-05 22:00:00', '2022-06-05 22:00:00');

INSERT INTO plague (id, scientific_name, created_on, lastmod_on)
VALUES (2, 'Botryocladia', '2022-06-05 22:00:00', '2022-06-05 22:00:00');

INSERT INTO localized_plague (id, plague_id, common_name, language, created_on, lastmod_on)
VALUES (1, 1, 'Cyanobacteria', 'en', '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO localized_plague (id, plague_id, common_name, language, created_on, lastmod_on)
VALUES (2, 1, 'Cyanobakterien', 'de', '2022-06-05 22:00:00', '2022-06-05 22:00:00');

INSERT INTO localized_plague (id, plague_id, common_name, language, created_on, lastmod_on)
VALUES (3, 2, 'Bubble-Algae', 'en', '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO localized_plague (id, plague_id, common_name, language, created_on, lastmod_on)
VALUES (4, 2, 'Kugelalgen', 'de', '2022-06-05 22:00:00', '2022-06-05 22:00:00');