CREATE TABLE `users` (
  `id`             BIGINT(20) UNSIGNED NOT NULL         AUTO_INCREMENT,
  `email`          VARCHAR(255)        NOT NULL,
  `password`       VARCHAR(255)        NOT NULL,
  `validate_token` VARCHAR(255)        NOT NULL,
  `language`       VARCHAR(2)          NOT NULL,
  `country`        VARCHAR(2)          NOT NULL,
  `validated`      BOOLEAN             NOT NULL         DEFAULT FALSE,
  `created_on`     TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  `lastmod_on`     TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNQ_EMAIL` (`email`)
)
  ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8;


CREATE TABLE `aquarium` (
  `id`          BIGINT(20) UNSIGNED NOT NULL         AUTO_INCREMENT,
  `size`        INTEGER,
  `size_unit`   VARCHAR(10),
  `description` VARCHAR(255)        NOT NULL,
  `active`      BIT                                  DEFAULT 0,
  `user_id`     BIGINT(20)          UNSIGNED,
  `setup_date`  TIMESTAMP           NULL,
  `created_on`  TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  `lastmod_on`  TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `aquarium_ibfk_1` FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE SET NULL
)
  ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8;


CREATE TABLE `unit` (
  `id`          INTEGER UNSIGNED NOT NULL,
  `name`        VARCHAR(15)      NOT NULL,
  `description` VARCHAR(255)     NOT NULL,
  `created_on`  TIMESTAMP        NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  `lastmod_on`  TIMESTAMP        NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE `parameter` (
  `id`                     INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
  `description`            VARCHAR(255)     NOT NULL,
  `used_threshold_unit_id` INTEGER UNSIGNED NOT NULL,
  `min_threshold`          FLOAT,
  `max_threshold`          FLOAT,
  `created_on`             TIMESTAMP        NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  `lastmod_on`             TIMESTAMP        NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `parameter_ibfk_1` FOREIGN KEY (used_threshold_unit_id) REFERENCES unit (id)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE `measurement` (
  `id`             BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `measured_on`    TIMESTAMP           NOT NULL,
  `measured_value` FLOAT               NOT NULL,
  `unit_id`        INTEGER UNSIGNED    NOT NULL,
  `aquarium_id`    BIGINT(20) UNSIGNED NOT NULL,
  `created_on`     TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  `lastmod_on`     TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `measurement_ibfk_1` FOREIGN KEY (unit_id) REFERENCES unit (id),
  CONSTRAINT `measurement_ibfk_2` FOREIGN KEY (aquarium_id) REFERENCES aquarium (id)
)
  ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8;

CREATE TABLE `remedy` (
  `id`          BIGINT(20) UNSIGNED NOT NULL         AUTO_INCREMENT,
  `productname` VARCHAR(60),
  `vendor`      VARCHAR(60),
  `created_on`  TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  `lastmod_on`  TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
)
  ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8;


CREATE TABLE `treatment` (
  `id`          BIGINT(20) UNSIGNED NOT NULL         AUTO_INCREMENT,
  `aquarium_id` BIGINT(20) UNSIGNED NOT NULL,
  `given_on`    DATETIME            NOT NULL,
  `amount`      FLOAT               NOT NULL,
  `unit_id`     INTEGER UNSIGNED    NOT NULL,
  `remedy_id`   BIGINT(20) UNSIGNED NOT NULL,
  `description` VARCHAR(255),
  `created_on`  TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  `lastmod_on`  TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `treatment_ibfk_1` FOREIGN KEY (unit_id) REFERENCES unit (id),
  CONSTRAINT `treatment_ibfk_2` FOREIGN KEY (aquarium_id) REFERENCES aquarium (id),
  CONSTRAINT `treatment_ibfk_3` FOREIGN KEY (remedy_id) REFERENCES remedy (id)
)
  ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8;


CREATE TABLE `fish_catalogue` (
  `id`                 BIGINT(20) UNSIGNED NOT NULL         AUTO_INCREMENT,
  `scientific_name`    VARCHAR(60),
  `description`        VARCHAR(400),
  `meerwasserwiki_url` VARCHAR(120),
  `created_on`         TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  `lastmod_on`         TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
)
  ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8;

CREATE TABLE `fish` (
  `id`                BIGINT(20) UNSIGNED NOT NULL         AUTO_INCREMENT,
  `aquarium_id`       BIGINT(20) UNSIGNED NOT NULL,
  `fish_catalogue_id` BIGINT(20) UNSIGNED NOT NULL,
  `added_on`          DATETIME            NOT NULL,
  `exodus_on`         DATETIME,
  `nickname`          VARCHAR(60),
  `observed_behavior` TEXT,
  `created_on`        TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  `lastmod_on`        TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fish_ibfk_1` FOREIGN KEY (aquarium_id) REFERENCES aquarium (id),
  CONSTRAINT `fish_ibfk_2` FOREIGN KEY (fish_catalogue_id) REFERENCES fish_catalogue (id)
)
  ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8;

-- +todo: According classification, refactor to suite jpa inheritence model
CREATE TABLE `coral_catalogue` (
  `id`              BIGINT(20) UNSIGNED NOT NULL         AUTO_INCREMENT,
  `scientific_name` VARCHAR(60),
  `description`     VARCHAR(400),
  `created_on`      TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  `lastmod_on`      TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
)
  ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8;

CREATE TABLE `coral` (
  `id`                 BIGINT(20) UNSIGNED NOT NULL         AUTO_INCREMENT,
  `aquarium_id`        BIGINT(20) UNSIGNED NOT NULL,
  `coral_catalouge_id` BIGINT(20) UNSIGNED NOT NULL,
  `added_on`           DATETIME            NOT NULL,
  `exodus_on`          DATETIME,
  `nickname`           VARCHAR(60),
  `observed_behavior`  TEXT,
  `created_on`         TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  `lastmod_on`         TIMESTAMP           NOT NULL         DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `coral_ibfk_1` FOREIGN KEY (aquarium_id) REFERENCES aquarium (id),
  CONSTRAINT `coral_ibfk_2` FOREIGN KEY (coral_catalouge_id) REFERENCES coral_catalogue (id)
)
  ENGINE = InnoDB
  AUTO_INCREMENT = 100
  DEFAULT CHARSET = utf8;