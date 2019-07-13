-- Reason: we coul not leverage spring-data because of the datamodel.
-- Fish -> Aquarium -> User required a join. Though logically ok, it makes no sence according
-- performance (requires joins) and additional effort. as we could not leverage spring-data this way.
-- So though redundant, we add the user relation ship directly to the detail tables as well.


ALTER TABLE `fish`
  add column
  `user_id`     BIGINT(20)          UNSIGNED;

ALTER TABLE `coral`
  add column
  `user_id`     BIGINT(20)          UNSIGNED;

ALTER TABLE `measurement`
  add column
  `user_id`     BIGINT(20)          UNSIGNED;

ALTER TABLE `treatment`
  add column
  `user_id`     BIGINT(20)          UNSIGNED;



-- Performing Datamigration
update fish set user_id = (select user_id from aquarium where fish.aquarium_id = aquarium.id);
update coral set user_id = (select user_id from aquarium where coral.aquarium_id = aquarium.id);
update measurement set user_id = (select user_id from aquarium where measurement.aquarium_id = aquarium.id);
update treatment set user_id = (select user_id from aquarium where treatment.aquarium_id = aquarium.id);



-- enforce integration
--
-- tbd. set null or introducing a dummy user?
--
alter table fish
  add FOREIGN KEY (user_id) REFERENCES users (id)
  ON DELETE SET NULL;

alter table coral
  add FOREIGN KEY (user_id) REFERENCES users (id)
  ON DELETE SET NULL;

alter table measurement
  add FOREIGN KEY (user_id) REFERENCES users (id)
  ON DELETE SET NULL;

alter table treatment
  add FOREIGN KEY (user_id) REFERENCES users (id)
  ON DELETE SET NULL;

