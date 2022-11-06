CREATE TABLE `user_measurement_reminder`
(
    `id`         BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT(20) UNSIGNED NOT NULL,
    `unit_id`    INTEGER UNSIGNED    NOT NULL,
    `pastdays`   INTEGER UNSIGNED             DEFAULT 14,
    `active`     BIT                          DEFAULT 1,
    `created_on` TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on` TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UNQ_USER_UNIT` (`user_id`, `unit_id`),
    FOREIGN KEY (unit_id) REFERENCES unit (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 100
    DEFAULT CHARSET = utf8;

-- Sample Test Data for sabi user:
INSERT INTO user_measurement_reminder (id, user_id, unit_id)
VALUES (1, 1, 1);
