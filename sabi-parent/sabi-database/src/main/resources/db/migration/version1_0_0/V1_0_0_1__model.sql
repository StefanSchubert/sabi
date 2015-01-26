CREATE TABLE `users` (
  `id`             BIGINT(20) UNSIGNED   NOT NULL         AUTO_INCREMENT,
  `email`          VARCHAR(255)          NOT NULL,
  `password`       VARCHAR(255)          NOT NULL,
  `validate_token` VARCHAR(255)          NOT NULL,
  `validated`      BOOLEAN               NOT NULL         DEFAULT FALSE,
  `created_on`     TIMESTAMP DEFAULT 0   NOT NULL,
  `lastmod_on`     TIMESTAMP             NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNQ_EMAIL` (`email`)
)
  ENGINE =InnoDB
  AUTO_INCREMENT =100
  DEFAULT CHARSET =utf8;


CREATE TABLE `aquarium` (
  `id`             BIGINT(20) UNSIGNED         NOT NULL AUTO_INCREMENT,
  `size`           INTEGER,
  `size_unit`      VARCHAR(10),
  `validate_token` VARCHAR(255)                NOT NULL,
  `active`         BOOLEAN                              DEFAULT FALSE,
  `user_id`        BIGINT(20) UNSIGNED,
  `created_on`     TIMESTAMP DEFAULT 0         NOT NULL,
  `lastmod_on`     TIMESTAMP                   NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE SET NULL
)
  ENGINE =InnoDB
  AUTO_INCREMENT =100
  DEFAULT CHARSET =utf8;


CREATE TABLE `units_lookup` (
  `id`          INTEGER UNSIGNED NOT NULL,
  `unit`        VARCHAR(5)       NOT NULL,
  `description` VARCHAR(255)     NOT NULL,
  `created_on`  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8;


CREATE TABLE `parameter_lookup` (
  `id`            INTEGER UNSIGNED NOT NULL,
  `unit`          VARCHAR(5)       NOT NULL,
  `description`   VARCHAR(255)     NOT NULL,
  `min_threshold` FLOAT,
  `max_threshold` FLOAT,
  `created_on`    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8;


CREATE TABLE `measurement` (
  `id`            BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `measured_on`   DATETIME            NOT NULL,
  `measued_value` FLOAT               NOT NULL,
  `unit_id`       INTEGER   UNSIGNED  NOT NULL,
  `parameter_id`  INTEGER    UNSIGNED NOT NULL,
  `aquarium_id`   BIGINT(20) UNSIGNED NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (unit_id) REFERENCES units_lookup (id),
  FOREIGN KEY (parameter_id) REFERENCES parameter_lookup (id),
  FOREIGN KEY (aquarium_id) REFERENCES aquarium (id)
)
  ENGINE =InnoDB
  AUTO_INCREMENT =100
  DEFAULT CHARSET =utf8;
