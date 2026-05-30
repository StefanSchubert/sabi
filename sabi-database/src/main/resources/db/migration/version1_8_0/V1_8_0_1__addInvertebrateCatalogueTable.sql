-- 006-invertebrate-tracking: Invertebrate catalogue with UGC workflow (PENDING/PUBLIC/REJECTED).
-- Mirrors coral_catalogue; adds active_scientific_name virtual column for unique index.

CREATE TABLE invertebrate_catalogue (
    id                     BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    scientific_name        VARCHAR(255)        NOT NULL,
    taxonomic_category     VARCHAR(12)         NOT NULL  COMMENT 'CRUSTACEAN | MOLLUSC | ECHINODERM | WORM',
    care_level             VARCHAR(12)         NOT NULL DEFAULT 'MODERATE' COMMENT 'EASY | MODERATE | DEMANDING',
    status                 VARCHAR(10)         NOT NULL DEFAULT 'PENDING'  COMMENT 'PENDING | PUBLIC | REJECTED',
    proposer_user_id       BIGINT(20) UNSIGNED          NULL               COMMENT 'FK to users.id',
    proposal_date          DATE                         NULL,
    created_on             DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmod_on             DATETIME                     NULL ON UPDATE CURRENT_TIMESTAMP,
    optlock                INT                 NOT NULL DEFAULT 0,
    -- Virtual generated column: only non-null when status is PENDING or PUBLIC
    active_scientific_name VARCHAR(255) AS (IF(status IN ('PENDING', 'PUBLIC'), scientific_name, NULL)) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE  idx_invert_catalogue_active_name (active_scientific_name),
    INDEX   idx_invert_catalogue_status      (status),
    INDEX   idx_invert_catalogue_proposer    (proposer_user_id),
    CONSTRAINT fk_invert_catalogue_proposer
        FOREIGN KEY (proposer_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
