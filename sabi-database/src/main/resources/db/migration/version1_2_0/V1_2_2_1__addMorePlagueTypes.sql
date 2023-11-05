-- Basic Data:
INSERT INTO plague (id, scientific_name, created_on, lastmod_on)
VALUES (6, 'Asterina', '2023-11-05 20:38:00', '2023-11-05 20:38:00');

INSERT INTO localized_plague (id, plague_id, common_name, language, created_on, lastmod_on)
VALUES (11, 6, 'Asterina starfish', 'en', '2023-11-05 20:38:00', '2023-11-05 20:38:00');
INSERT INTO localized_plague (id, plague_id, common_name, language, created_on, lastmod_on)
VALUES (12, 6, 'Asterina Seestern', 'de', '2023-11-05 20:38:00', '2023-11-05 20:38:00');
