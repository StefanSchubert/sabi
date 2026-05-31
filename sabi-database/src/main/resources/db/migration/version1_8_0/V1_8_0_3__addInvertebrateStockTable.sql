-- 006-invertebrate-tracking: Invertebrate stock per user tank.
-- Mirrors coral_stock; adds invertebrate-specific classification columns.

CREATE TABLE invertebrate_stock (
    id                         BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    aquarium_id                BIGINT(20) UNSIGNED NOT NULL,
    user_id                    BIGINT(20) UNSIGNED NOT NULL,
    invertebrate_catalogue_id  BIGINT(20) UNSIGNED          NULL,
    species_name               VARCHAR(255)        NOT NULL,
    scientific_name            VARCHAR(255)                 NULL,
    care_level                 VARCHAR(12)                  NULL COMMENT 'EASY | MODERATE | DEMANDING',
    external_ref_url           VARCHAR(512)                 NULL,
    notes                      TEXT                         NULL,
    added_on                   DATE                NOT NULL,
    departed_on                DATE                         NULL,
    departure_reason           VARCHAR(30)                  NULL COMMENT 'DIED | SOLD | GIVEN_AWAY | MOVED_TO_OTHER_TANK | OTHER',
    departure_note             TEXT                         NULL,
    -- Invertebrate-specific functional classifications
    taxonomic_category         VARCHAR(12)         NOT NULL COMMENT 'CRUSTACEAN | MOLLUSC | ECHINODERM | WORM',
    mobility                   VARCHAR(10)                  NULL COMMENT 'MOBILE | SESSILE',
    ecological_role            VARCHAR(15)                  NULL COMMENT 'CLEANUP_CREW | NEUTRAL | DETRIMENTAL',
    activity_pattern           VARCHAR(10)                  NULL COMMENT 'DIURNAL | NOCTURNAL | BOTH',
    -- Soft delete
    deleted_at                 TIMESTAMP                    NULL,
    created_on                 DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmod_on                 DATETIME                     NULL ON UPDATE CURRENT_TIMESTAMP,
    optlock                    INT                 NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX  idx_invert_stock_aquarium    (aquarium_id),
    INDEX  idx_invert_stock_user        (user_id),
    INDEX  idx_invert_stock_deleted_at  (deleted_at),
    CONSTRAINT fk_invert_stock_aquarium
        FOREIGN KEY (aquarium_id) REFERENCES aquarium (id) ON DELETE CASCADE,
    CONSTRAINT fk_invert_stock_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_invert_stock_catalogue
        FOREIGN KEY (invertebrate_catalogue_id) REFERENCES invertebrate_catalogue (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
