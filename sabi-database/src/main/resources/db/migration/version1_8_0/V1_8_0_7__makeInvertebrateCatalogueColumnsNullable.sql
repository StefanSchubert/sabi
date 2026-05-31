-- 006-invertebrate-tracking: care_level and taxonomic_category should be optional
-- for UGC proposals (users only need to supply a scientific name to propose).
ALTER TABLE invertebrate_catalogue
    MODIFY COLUMN taxonomic_category VARCHAR(12) NULL    COMMENT 'CRUSTACEAN | MOLLUSC | ECHINODERM | WORM',
    MODIFY COLUMN care_level         VARCHAR(12) NULL    COMMENT 'EASY | MODERATE | DEMANDING';
