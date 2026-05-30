-- 006-invertebrate-tracking: Water sensitivity link table for invertebrate stock entries.
-- Records which measurement units (water parameters) the invertebrate is sensitive to.

CREATE TABLE sabi.invertebrate_water_sensitivity (
    id                     BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    invertebrate_stock_id  BIGINT(20) UNSIGNED NOT NULL,
    unit_id                INT                 NOT NULL,
    created_on             DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmod_on             DATETIME                     NULL ON UPDATE CURRENT_TIMESTAMP,
    optlock                INT                 NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE idx_invert_water_sens_stock_unit (invertebrate_stock_id, unit_id),
    INDEX  idx_invert_water_sens_unit       (unit_id),
    CONSTRAINT fk_invert_water_sens_stock
        FOREIGN KEY (invertebrate_stock_id) REFERENCES sabi.invertebrate_stock (id) ON DELETE CASCADE,
    CONSTRAINT fk_invert_water_sens_unit
        FOREIGN KEY (unit_id) REFERENCES sabi.unit (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
