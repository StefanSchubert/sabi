-- 006-invertebrate-tracking (follow-up): Add include_fish flag to public_report_link.
-- DEFAULT 1 ensures backward compatibility: existing links keep showing fish.
ALTER TABLE public_report_link
    ADD COLUMN include_fish TINYINT(1) NOT NULL DEFAULT 1;
