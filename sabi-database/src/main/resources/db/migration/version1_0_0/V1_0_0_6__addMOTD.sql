CREATE TABLE `motd`
(
    `id`                BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `publish_date`      TIMESTAMP           NOT NULL,
    `vanish_date`       TIMESTAMP           NULL,
    `created_on`        TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`        TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `optlock`           INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 100
    DEFAULT CHARSET = utf8;

CREATE TABLE `localized_motd`
(
    `id`         BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `motd_id`    BIGINT(20) UNSIGNED NOT NULL,
    `text`       VARCHAR(255)        NOT NULL,
    `language`   VARCHAR(3)          NOT NULL,
    `created_on` TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on` TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `optlock`    INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `business_key` (`motd_id`, `language`),
    foreign key (motd_id) references motd (id)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 100
    DEFAULT CHARSET = utf8;



/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

-- Some Test and Basic Data:
INSERT INTO motd (id, publish_date, vanish_date, created_on, lastmod_on)
VALUES (100, '2020-01-01 11:31:12', '2020-01-03 11:31:12', '2020-01-04 11:31:12', '2020-01-04 11:31:12');
INSERT INTO motd (id, publish_date, vanish_date, created_on, lastmod_on)
VALUES (101, '2020-01-04 11:00:00', null, '2020-01-04 11:31:12', '2020-01-04 11:31:12');

INSERT INTO localized_motd (id, motd_id, text, language, created_on, lastmod_on)
VALUES (100, 100, 'Stale Test Entry', 'en', '2020-01-04 11:20:00', '2020-01-04 11:20:00');
INSERT INTO localized_motd (id, motd_id, text, language, created_on, lastmod_on)
VALUES (101, 101, 'Maintenance Work will happen each first Saturday of the Month around 8 to 9 pm GMT +1.', 'en',
        '2020-01-04 11:27:04', '2020-01-04 11:27:04');
INSERT INTO localized_motd (id, motd_id, text, language, created_on, lastmod_on)
VALUES (102, 101, 'Wartungsarbeiten sind aktuell jeden ersten Samstag im Monat von 20 bis 21 Uhr GMT+1 geplant.',
        'de', '2020-01-04 11:27:04', '2020-01-04 11:27:04');

