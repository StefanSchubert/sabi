CREATE TABLE `localized_unit`
(
    `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `unit_id`     INT UNSIGNED NOT NULL,
    `description` VARCHAR(80)         NOT NULL,
    `language`    VARCHAR(3)          NOT NULL,
    `created_on`  TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`  TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `optlock`     INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `business_key` (`unit_id`, `language`),
    foreign key (unit_id) references unit(id)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8;

ALTER TABLE unit drop column description;

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (1, 'Karbonathärte (1 °DH = 0,178 mmol/l CAO3)', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (2, 'Temperatur in Celsius', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (3, 'Calcium-Konzentration in ppm (1 ppm = 1.023 mg/L)', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (4, 'Magnesium-Konzentration in ppm (1 ppm = 1.023 mg/L)', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (5, 'PH Wert', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (6, 'Ammonium', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (7, 'Ammoniak', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (8, 'Nitrit', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (9, 'Nitrat', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (10, 'Phosphat', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (11, 'Salinität', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (1, 'Carbonate hardness (1 °DH = 0.178 mmol/l CAO3)', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (2, 'Temperature in Celsius', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (3, 'Calcium concentration in ppm (1 ppm = 1,023 mg/L)', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (4, 'Magnesium concentration in ppm (1 ppm = 1,023 mg/L)', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (5, 'PH value', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (6, 'Ammonium', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (7, 'Ammonia', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (8, 'nitrite', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (9, 'nitrate', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (10, 'phosphate', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (11, 'Salinity', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (1, 'Dureza carbonatada (1 °DH = 0,178 mmol/l CAO3)', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (2, 'Temperatura', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (3, 'Concentración de calcio en ppm (1 ppm = 1,023 mg/L)', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (4, 'Concentración de magnesio en ppm (1 ppm = 1,023 mg/L)', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (5, 'Valor PH', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (6, 'Amonio', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (7, 'Amoníaco', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (8, 'nitrito', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (9, 'nitrato', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (10, 'fosfato', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (11, 'Salinidad', 'es', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (1, 'Dureté carbonatée (1 °DH = 0,178 mmol/l CAO3)', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (2, 'Température', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (3, 'Concentration de calcium en ppm (1 ppm = 1.023 mg/L)', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (4, 'Concentration en magnésium en ppm (1 ppm = 1.023 mg/L)', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (5, 'Valeur du PH', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (6, 'Ammonium', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (7, 'Ammoniaque', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (8, 'Nitrite', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (9, 'Nitrate', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (10, 'Phosphate', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (11, 'Salinité', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (1, 'Durezza carbonatica (1 °DH = 0,178 mmol/l CAO3)', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (2, 'Temperatura', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (3, 'Concentrazione di calcio in ppm (1 ppm = 1,023 mg/L)', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (4, 'Concentrazione di magnesio in ppm (1 ppm = 1,023 mg/L)', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (5, 'Valore PH', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (6, 'Ammonio', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (7, 'Ammoniaca', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (8, 'nitrito', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (9, 'nitrato', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (10, 'fosfato', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO sabi.localized_unit (unit_id, description, language, created_on, lastmod_on, optlock)
VALUES (11, 'Salinità', 'it', DEFAULT, DEFAULT, DEFAULT);


