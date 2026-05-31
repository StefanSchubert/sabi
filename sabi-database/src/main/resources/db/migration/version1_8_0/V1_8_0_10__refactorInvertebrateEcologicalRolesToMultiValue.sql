-- 006-invertebrate-tracking: Replace single ecological_role column with a join table.
-- An invertebrate may now have zero, one, or multiple ecological roles simultaneously.
-- Old roles (CLEANUP_CREW, NEUTRAL, DETRIMENTAL) are not migrated — they were not
-- semantically compatible with the new fine-grained role set.

ALTER TABLE invertebrate_stock
    DROP COLUMN ecological_role;

CREATE TABLE invertebrate_stock_ecological_roles (
    invertebrate_stock_id  BIGINT(20) UNSIGNED NOT NULL,
    ecological_role        VARCHAR(30)         NOT NULL,
    PRIMARY KEY (invertebrate_stock_id, ecological_role),
    CONSTRAINT fk_invert_roles_stock
        FOREIGN KEY (invertebrate_stock_id) REFERENCES invertebrate_stock (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
