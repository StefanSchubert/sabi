CREATE TABLE `localized_parameter`
(
    `id`           BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `parameter_id` INT UNSIGNED NOT NULL,
    `description` VARCHAR(80)         NOT NULL,
    `language`    VARCHAR(3)          NOT NULL,
    `created_on`  TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`  TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `optlock`     INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `business_key` (`parameter_id`, `language`),
    foreign key (parameter_id) references parameter(id)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8;

ALTER TABLE parameter drop column description;

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (1, 'Karbonathärte', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (2, 'Temperatur', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (3, 'Magnesium', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (4, 'Calcium', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (5, 'PH', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (6, 'Phosphat', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (7, 'Salzgehalt', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (1, 'Carbonate hardness', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (2, 'temperature', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (3, 'magnesium', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (4, 'calcium', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (5, 'PH', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (6, 'phosphate', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (7, 'Salinity', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (2, 'temperatura', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (3, 'magnesio', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (4, 'calcio', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (5, 'PH', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (6, 'fosfato', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (7, 'Salinidad', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (1, 'Dureza del carbonato', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (1, 'Durezza del carbonato', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (2, 'temperatura', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (3, 'magnesio', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (4, 'calcio', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (5, 'PH', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (6, 'fosfato', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (7, 'Salinità', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (1, 'Dureté carbonatée', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (2, 'Température', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (3, 'Magnésium', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (4, 'Calcium', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (5, 'PH', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (6, 'Phosphate', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_parameter (parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES (7, 'Teneur en sel', 'fr', DEFAULT, DEFAULT, DEFAULT);

