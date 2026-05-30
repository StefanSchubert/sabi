-- 006-invertebrate-tracking: Photo metadata for invertebrate stock entries.
-- Mirrors coral_photo. One photo per invertebrate entry (UNIQUE constraint).

CREATE TABLE invertebrate_photo (
    id                     BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    invertebrate_stock_id  BIGINT(20) UNSIGNED NOT NULL,
    file_path              VARCHAR(512)        NOT NULL,
    content_type           VARCHAR(50)         NOT NULL,
    upload_date            DATE                NOT NULL,
    created_on             DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmod_on             DATETIME                     NULL ON UPDATE CURRENT_TIMESTAMP,
    optlock                INT                 NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE idx_invert_photo_stock_id (invertebrate_stock_id),
    CONSTRAINT fk_invert_photo_stock
        FOREIGN KEY (invertebrate_stock_id) REFERENCES invertebrate_stock (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
