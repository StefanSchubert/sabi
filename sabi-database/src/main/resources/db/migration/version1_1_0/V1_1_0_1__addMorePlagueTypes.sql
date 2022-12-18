-- Basic Data:
INSERT INTO plague (id, scientific_name, created_on, lastmod_on)
VALUES (5, 'Zygnema stellinum', '2022-06-05 22:00:00', '2022-06-05 22:00:00');

INSERT INTO localized_plague (id, plague_id, common_name, language, created_on, lastmod_on)
VALUES (9, 5, 'Filamentous algae', 'en', '2022-09-05 22:00:00', '2022-09-05 22:00:00');
INSERT INTO localized_plague (id, plague_id, common_name, language, created_on, lastmod_on)
VALUES (10, 5, 'Fadenalge/Sternalge', 'de', '2022-09-05 22:00:00', '2022-09-05 22:00:00');

