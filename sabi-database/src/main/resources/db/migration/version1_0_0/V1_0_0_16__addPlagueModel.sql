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

CREATE TABLE `plague_status`
(
    `id`         BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `created_on` TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on` TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `optlock`    INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8;

CREATE TABLE `localized_plague_status`
(
    `id`               BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `plague_status_id` BIGINT(20) UNSIGNED NOT NULL,
    `description`      VARCHAR(80)         NOT NULL,
    `language`         VARCHAR(3)          NOT NULL,
    `created_on`       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `optlock`          INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `business_key` (`plague_status_id`, `language`),
    foreign key (plague_status_id) references plague_status (id)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8;

CREATE TABLE `plague_record`
(
    `id`                     BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `aquarium_id`            BIGINT(20) UNSIGNED NOT NULL,
    `user_id`                BIGINT(20) UNSIGNED NOT NULL,
    `observed_on`            TIMESTAMP           NOT NULL,
    `plague_id`              BIGINT(20) UNSIGNED NOT NULL,
    `observed_plague_status` BIGINT(20) UNSIGNED NOT NULL,
    `created_on`             TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`             TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `optlock`                INT UNSIGNED                 DEFAULT 0,
    PRIMARY KEY (`id`),
    FOREIGN KEY (plague_id) REFERENCES plague (id),
    FOREIGN KEY (observed_plague_status) REFERENCES plague_status (id),
    FOREIGN KEY (aquarium_id) REFERENCES aquarium (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 100
    DEFAULT CHARSET = utf8;

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

INSERT INTO plague_status (id, created_on, lastmod_on)
VALUES (1, '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO plague_status (id, created_on, lastmod_on)
VALUES (2, '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO plague_status (id, created_on, lastmod_on)
VALUES (3, '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO plague_status (id, created_on, lastmod_on)
VALUES (4, '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO plague_status (id, created_on, lastmod_on)
VALUES (5, '2022-06-05 22:00:00', '2022-06-05 22:00:00');

-- english
INSERT INTO localized_plague_status (id, plague_status_id, description, language, created_on, lastmod_on)
VALUES (1, 1, 'First spots visible', 'en', '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO localized_plague_status (id, plague_status_id, description, language, created_on, lastmod_on)
VALUES (2, 2, 'Seems to be spreading', 'en', '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO localized_plague_status (id, plague_status_id, description, language, created_on, lastmod_on)
VALUES (3, 3, 'Growing seems to stop', 'en', '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO localized_plague_status (id, plague_status_id, description, language, created_on, lastmod_on)
VALUES (4, 4, 'Slowly retreating', 'en', '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO localized_plague_status (id, plague_status_id, description, language, created_on, lastmod_on)
VALUES (5, 5, 'Vanished (will close plage log)', 'en', '2022-06-05 22:00:00', '2022-06-05 22:00:00');

/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

-- german
INSERT INTO localized_plague_status (id, plague_status_id, description, language, created_on, lastmod_on)
VALUES (6, 1, 'Erstes Aufkommen', 'de', '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO localized_plague_status (id, plague_status_id, description, language, created_on, lastmod_on)
VALUES (7, 2, 'Verbreitet sich', 'de', '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO localized_plague_status (id, plague_status_id, description, language, created_on, lastmod_on)
VALUES (8, 3, 'Ausdehnung stagniert', 'de', '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO localized_plague_status (id, plague_status_id, description, language, created_on, lastmod_on)
VALUES (9, 4, 'Langsam rückgänig', 'de', '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO localized_plague_status (id, plague_status_id, description, language, created_on, lastmod_on)
VALUES (10, 5, 'Verschwunden (plague beendet, log schließt)', 'de', '2022-06-05 22:00:00', '2022-06-05 22:00:00');

-- Some Testdata
INSERT INTO plague_record (id, aquarium_id, user_id, observed_on, plague_id, observed_plague_status, created_on, lastmod_on)
VALUES (1, 1, 1, '2022-06-05 22:00:00', 1, 1, '2022-06-05 22:00:00', '2022-06-05 22:00:00');
INSERT INTO plague_record (id, aquarium_id, user_id, observed_on, plague_id, observed_plague_status, created_on, lastmod_on)
VALUES (2, 1, 1, '2022-07-05 08:00:00', 1, 5, '2022-07-05 08:00:00', '2022-07-08 22:00:00');
