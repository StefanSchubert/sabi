-- 006-invertebrate-tracking: Add include_invertebrates flag to public_report_link.
-- Mirrors include_corals column (V1_7_0_7).

ALTER TABLE public_report_link
    ADD COLUMN include_invertebrates TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'When 1, invertebrate stock is included in the public report';
