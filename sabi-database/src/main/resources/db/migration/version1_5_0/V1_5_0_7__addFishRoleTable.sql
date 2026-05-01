/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

-- Fish role assignment feature: master role table, localized descriptions, and assignment join table.

-- 1. fish_role: master table (enum-like, app-managed list)
CREATE TABLE `fish_role`
(
    `id`         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `enum_key`   VARCHAR(40)  NOT NULL COMMENT 'Program-internal key e.g. INDICATOR_FISH',
    `created_on` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`    INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_fish_role_enum_key` (`enum_key`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8
    COMMENT = 'Master list of fish roles (functional classification)';

-- 2. Seed data: 10 roles as per issue description
INSERT INTO `fish_role` (`enum_key`)
VALUES ('INDICATOR_FISH'),
       ('REFERENCE_FISH'),
       ('ALGAE_CLEANER'),
       ('DETRITUS_CLEANER'),
       ('PARASITE_EATER'),
       ('SOCIAL_DYNAMIC_MARKER'),
       ('SYSTEM_DRIVER'),
       ('TIME_REFERENCE'),
       ('EYE_CATCHER'),
       ('OTHER');

-- 3. localized_fish_role: i18n names and descriptions
CREATE TABLE `localized_fish_role`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `role_id`       INT UNSIGNED    NOT NULL,
    `language_code` VARCHAR(2)      NOT NULL COMMENT 'de | en | es | fr | it',
    `name`          VARCHAR(80)     NOT NULL,
    `description`   VARCHAR(512)    NULL,
    `created_on`    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`       INT UNSIGNED    NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_fish_role_lang` (`role_id`, `language_code`),
    CONSTRAINT `fk_localized_fish_role`
        FOREIGN KEY (`role_id`) REFERENCES `fish_role` (`id`) ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8
    COMMENT = 'Localized names and descriptions for fish roles';

-- 4. Seed i18n data (5 languages × 10 roles = 50 rows)
-- German (de)
INSERT INTO `localized_fish_role` (`role_id`, `language_code`, `name`, `description`)
VALUES (1, 'de', 'Indikator-Fisch',
        'Fr\u00fchzeitige Erkennung von Problemen mit Nitrit, Sauerstoff oder Temperaturschwankungen'),
       (2, 'de', 'Referenz-Fisch',
        'Diese Art ist als robust bekannt. Verhält sie sich seltsam, ist das System definitiv aus dem Gleichgewicht'),
       (3, 'de', 'Algenfresser', 'Frisst Algen und hält deren Wachstum in Grenzen'),
       (4, 'de', 'Detritusfresser', 'Hilft den Bodengrund sauber zu halten'),
       (5, 'de', 'Parasitenvertilger', 'Bietet anderen Bewohnern Reinigungsservice'),
       (6, 'de', 'Sozialverhalts-Indikator',
        'Zeigt Territorialverhalten. Zu häufige Beobachtung deutet auf Überbelegung und Dauerstress hin'),
       (7, 'de', 'Systemtreiber',
        'Gro\u00dfe oder viel fressende Arten, die die nat\u00fcrliche Nitrat-/Phosphatproduktion beeinflussen'),
       (8, 'de', 'Zeitreferenz',
        'Fische mit ausgeprägtem Tag-/Nacht-Rhythmus. Ihre Aktivität kann zur Überwachung der zirkadianen Stabilität genutzt werden'),
       (9, 'de', 'Hingucker', 'Ein Fisch, den man einfach gerne beobachtet'),
       (10, 'de', 'Sonstige', 'Andere Rollen');

-- English (en)
INSERT INTO `localized_fish_role` (`role_id`, `language_code`, `name`, `description`)
VALUES (1, 'en', 'Indicator Fish',
        'Early detection of problems with nitrite, oxygen, or volatile temperatures'),
       (2, 'en', 'Reference Fish',
        'This species is known to be sturdy. If they behave strangely, your system is definitely out of balance'),
       (3, 'en', 'Algae Cleaner', 'Eats algae and keeps them in limit'),
       (4, 'en', 'Detritus Cleaner', 'Helps to keep the ground nice and clear'),
       (5, 'en', 'Parasite Eater', 'Offers doctor services to other inhabitants'),
       (6, 'en', 'Social Dynamic Marker',
        'Shows territorial behavior. If observed too often, you may have too many fish causing permanent stress'),
       (7, 'en', 'System Driver',
        'Big or heavy-eating species that have an impact on natural nitrate/phosphate generation'),
       (8, 'en', 'Time Reference',
        'Fish with a clearly defined day/night rhythm. Their activity can be measured to check circadian stability'),
       (9, 'en', 'Eye Catcher', 'A fish you simply love to observe'),
       (10, 'en', 'Other', 'Other roles');

-- Spanish (es)
INSERT INTO `localized_fish_role` (`role_id`, `language_code`, `name`, `description`)
VALUES (1, 'es', 'Pez Indicador',
        'Detecci\u00f3n temprana de problemas con nitrito, ox\u00edgeno o temperaturas variables'),
       (2, 'es', 'Pez de Referencia',
        'Esta especie es conocida por ser robusta. Si se comporta de forma extra\u00f1a, el sistema est\u00e1 desequilibrado'),
       (3, 'es', 'Limpiador de Algas', 'Come algas y las mantiene bajo control'),
       (4, 'es', 'Limpiador de Detritos', 'Ayuda a mantener el fondo limpio y despejado'),
       (5, 'es', 'Comedor de Par\u00e1sitos', 'Ofrece servicios de limpieza a otros habitantes'),
       (6, 'es', 'Marcador Din\u00e1mico Social',
        'Muestra comportamiento territorial. Si se observa con demasiada frecuencia, puede haber demasiados peces'),
       (7, 'es', 'Motor del Sistema',
        'Especies grandes o comedoras que impactan en la generaci\u00f3n natural de nitratos/fosfatos'),
       (8, 'es', 'Referencia Temporal',
        'Peces con un claro ritmo d\u00eda/noche. Su actividad puede medirse para comprobar la estabilidad circadiana'),
       (9, 'es', 'Llamativo', 'Un pez que simplemente te encanta observar'),
       (10, 'es', 'Otro', 'Otras funciones');

-- French (fr)
INSERT INTO `localized_fish_role` (`role_id`, `language_code`, `name`, `description`)
VALUES (1, 'fr', 'Poisson Indicateur',
        'D\u00e9tection pr\u00e9coce des probl\u00e8mes de nitrite, d\'oxyg\u00e8ne ou de temp\u00e9ratures instables'),
       (2, 'fr', 'Poisson de R\u00e9f\u00e9rence',
        'Cette esp\u00e8ce est connue pour \u00eatre robuste. Un comportement \u00e9trange indique un d\u00e9s\u00e9quilibre'),
       (3, 'fr', 'Nettoyeur d\'Algues', 'Mange les algues et les maintient sous contr\u00f4le'),
       (4, 'fr', 'Nettoyeur de D\u00e9tritus', 'Aide \u00e0 maintenir le fond propre'),
       (5, 'fr', 'Mangeur de Parasites', 'Offre des services de nettoyage aux autres habitants'),
       (6, 'fr', 'Marqueur de Dynamique Sociale',
        'Montre un comportement territorial. Trop fr\u00e9quent indique une surpopulation et du stress permanent'),
       (7, 'fr', 'Moteur du Syst\u00e8me',
        'Esp\u00e8ces grosses ou \u00e0 forte consommation ayant un impact sur la g\u00e9n\u00e9ration naturelle de nitrates/phosphates'),
       (8, 'fr', 'R\u00e9f\u00e9rence Temporelle',
        'Poissons avec un rythme jour/nuit clairement d\u00e9fini, utilisable pour surveiller la stabilit\u00e9 circadienne'),
       (9, 'fr', 'Spectacle', 'Un poisson qu\'on adore simplement observer'),
       (10, 'fr', 'Autre', 'Autres r\u00f4les');

-- Italian (it)
INSERT INTO `localized_fish_role` (`role_id`, `language_code`, `name`, `description`)
VALUES (1, 'it', 'Pesce Indicatore',
        'Rilevamento precoce di problemi con nitrito, ossigeno o temperature variabili'),
       (2, 'it', 'Pesce di Riferimento',
        'Questa specie \u00e8 nota per essere robusta. Un comportamento strano indica uno squilibrio del sistema'),
       (3, 'it', 'Pulitore di Alghe', 'Mangia le alghe e le mantiene sotto controllo'),
       (4, 'it', 'Pulitore di Detriti', 'Aiuta a mantenere il fondo pulito'),
       (5, 'it', 'Mangiatore di Parassiti', 'Offre servizi di pulizia agli altri abitanti'),
       (6, 'it', 'Marcatore Dinamico Sociale',
        'Mostra comportamento territoriale. Troppo frequente indica sovrappopolazione e stress permanente'),
       (7, 'it', 'Motore del Sistema',
        'Specie grandi o con elevato consumo che influenzano la produzione naturale di nitrati/fosfati'),
       (8, 'it', 'Riferimento Temporale',
        'Pesci con un chiaro ritmo giorno/notte, misurabile per verificare la stabilit\u00e0 circadiana'),
       (9, 'it', 'Pesce Attraente', 'Un pesce che semplicemente ami osservare'),
       (10, 'it', 'Altro', 'Altri ruoli');

-- 5. fish_role_assignment: many-to-many join table (no audit columns needed)
CREATE TABLE `fish_role_assignment`
(
    `fish_id` BIGINT UNSIGNED NOT NULL,
    `role_id` INT UNSIGNED    NOT NULL,
    PRIMARY KEY (`fish_id`, `role_id`),
    CONSTRAINT `fk_fra_fish` FOREIGN KEY (`fish_id`) REFERENCES `fish` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_fra_role` FOREIGN KEY (`role_id`) REFERENCES `fish_role` (`id`) ON DELETE CASCADE
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8
    COMMENT = 'Many-to-many assignment of fish roles to individual fish entries';
