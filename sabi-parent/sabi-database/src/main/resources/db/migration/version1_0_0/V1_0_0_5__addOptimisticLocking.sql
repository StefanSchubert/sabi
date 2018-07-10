-- Reason: As the project is also a reference architecture, I deceided to introduce optimistic locking, too.
-- though at the moment I have no real requirement for it.


ALTER TABLE `aquarium`
  add column `optlock` INT UNSIGNED DEFAULT 0 NOT NULL;

ALTER TABLE `coral`
  add column `optlock` INT UNSIGNED DEFAULT 0 NOT NULL;

ALTER TABLE `coral_catalogue`
  add column `optlock` INT UNSIGNED DEFAULT 0 NOT NULL;

ALTER TABLE `fish`
  add column `optlock` INT UNSIGNED DEFAULT 0 NOT NULL;

ALTER TABLE `fish_catalogue`
  add column `optlock` INT UNSIGNED DEFAULT 0 NOT NULL;

ALTER TABLE `measurement`
  add column `optlock` INT UNSIGNED DEFAULT 0 NOT NULL;

ALTER TABLE `parameter`
  add column `optlock` INT UNSIGNED DEFAULT 0 NOT NULL;

ALTER TABLE `remedy`
  add column `optlock` INT UNSIGNED DEFAULT 0 NOT NULL;

ALTER TABLE `treatment`
  add column `optlock` INT UNSIGNED DEFAULT 0 NOT NULL;

ALTER TABLE `unit`
  add column `optlock` INT UNSIGNED DEFAULT 0 NOT NULL;

ALTER TABLE `users`
  add column `optlock` INT UNSIGNED DEFAULT 0 NOT NULL;

