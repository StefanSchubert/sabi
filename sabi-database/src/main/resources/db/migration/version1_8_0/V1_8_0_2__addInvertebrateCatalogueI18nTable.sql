-- 006-invertebrate-tracking: i18n localised fields per invertebrate catalogue entry.
-- Mirrors coral_catalogue_i18n.

CREATE TABLE sabi.invertebrate_catalogue_i18n (
    id             BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    catalogue_id   BIGINT(20) UNSIGNED NOT NULL,
    language_code  VARCHAR(2)          NOT NULL,
    common_name    VARCHAR(255)                 NULL,
    description    TEXT                         NULL,
    reference_url  VARCHAR(512)                 NULL,
    created_on     DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmod_on     DATETIME                     NULL ON UPDATE CURRENT_TIMESTAMP,
    optlock        INT                 NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE idx_invert_cat_i18n_cat_lang (catalogue_id, language_code),
    INDEX  idx_invert_cat_i18n_lang     (language_code),
    CONSTRAINT fk_invert_cat_i18n_catalogue
        FOREIGN KEY (catalogue_id) REFERENCES sabi.invertebrate_catalogue (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
