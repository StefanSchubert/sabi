INSERT INTO unit (id, name, description) VALUES (1, 'KH', 'Karbonathärte / Alkanität');
INSERT INTO unit (id, name, description) VALUES (2, '°C', 'Grad Celsius');

INSERT INTO parameter (id, description, used_threshold_unit_id, min_threshold, max_threshold)
VALUES (1, 'Karbonathärte', 1, 6.5, 10);

INSERT INTO parameter (id, description, used_threshold_unit_id, min_threshold, max_threshold)
VALUES (2, 'Temperatur', 2, 24, 27);

INSERT INTO remedy (id, productname, vendor) VALUES (1, 'KH+', 'Dupla');

INSERT INTO fish_catalogue (id, scientific_name, description, meerwasserwiki_url)
VALUES (1, 'Acreichthys tomentosus', 'Seegras Feilenfisch', 'http://meerwasserwiki.de/w/index.php?title=Acreichthys_tomentosus');