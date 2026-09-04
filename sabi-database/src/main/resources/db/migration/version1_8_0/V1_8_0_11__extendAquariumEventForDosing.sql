-- Extend aquarium_event so structured additions and automated dosing can be recorded.
-- Existing generic logbook events remain supported through event_type = 'GENERIC'.

ALTER TABLE `aquarium_event`
    ADD COLUMN `event_type` VARCHAR(32) NOT NULL DEFAULT 'GENERIC' AFTER `event_date`,
    ADD COLUMN `event_time` VARCHAR(5) NULL COMMENT '24h HH:mm time of the event or dosing start' AFTER `event_type`,
    ADD COLUMN `amount` DECIMAL(10,3) NULL COMMENT 'Added/dosed amount of product or solution' AFTER `duration_hours`,
    ADD COLUMN `amount_unit` VARCHAR(30) NULL COMMENT 'Unit for amount, e.g. ml, g, tablets, drops' AFTER `amount`,
    ADD COLUMN `product_name` VARCHAR(255) NULL COMMENT 'Product, substance or solution name' AFTER `amount_unit`,
    ADD COLUMN `category` VARCHAR(80) NULL COMMENT 'Optional free-text category' AFTER `product_name`,
    ADD COLUMN `dosing_interval` VARCHAR(40) NULL COMMENT 'Optional dosing interval/frequency, e.g. day' AFTER `category`,
    ADD COLUMN `dosing_method` VARCHAR(80) NULL COMMENT 'Optional dosing method, e.g. dosing pump' AFTER `dosing_interval`,
    ADD COLUMN `solution_description` TEXT NULL COMMENT 'Optional solution/concentration description' AFTER `dosing_method`,
    ADD COLUMN `note` TEXT NULL COMMENT 'Optional user note for dosing/addition records' AFTER `solution_description`,
    ADD COLUMN `dosing_end_on` TIMESTAMP NULL COMMENT 'Optional end of automated dosing schedule' AFTER `note`;
