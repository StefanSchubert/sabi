ALTER TABLE `users`
  add column
  `username` VARCHAR(255) NULL;

update users
set username = email;

alter table users
  modify username varchar(255) not null;

ALTER TABLE users
  ADD CONSTRAINT UQ_USERNAME
UNIQUE KEY(username);


