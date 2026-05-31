-- Aiptasia (Glasrosen / Glass Anemones) as a new plague type
-- plague id 7

INSERT INTO plague (id, scientific_name, created_on, lastmod_on)
VALUES (7, 'Aiptasia', '2026-05-31 00:00:00', '2026-05-31 00:00:00');

INSERT INTO localized_plague (plague_id, common_name, language, created_on, lastmod_on, optlock)
VALUES (7, 'Glass anemone', 'en', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_plague (plague_id, common_name, language, created_on, lastmod_on, optlock)
VALUES (7, 'Glasrose', 'de', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_plague (plague_id, common_name, language, created_on, lastmod_on, optlock)
VALUES (7, 'Anemone di vetro', 'it', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_plague (plague_id, common_name, language, created_on, lastmod_on, optlock)
VALUES (7, 'Anémone de verre', 'fr', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO localized_plague (plague_id, common_name, language, created_on, lastmod_on, optlock)
VALUES (7, 'Anémona de cristal', 'es', DEFAULT, DEFAULT, DEFAULT);
