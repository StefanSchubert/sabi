/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

INSERT INTO unit (id, name, description) VALUES (1, 'KH', 'Karbonathärte / Alkanität');
INSERT INTO unit (id, name, description) VALUES (2, '°C', 'Grad Celsius');

INSERT INTO parameter (id, description, used_threshold_unit_id, min_threshold, max_threshold)
VALUES (1, 'Karbonathärte', 1, 6.5, 10);

INSERT INTO parameter (id, description, used_threshold_unit_id, min_threshold, max_threshold)
VALUES (2, 'Temperatur', 2, 24, 27);

INSERT INTO remedy (id, productname, vendor) VALUES (1, 'KH+', 'Dupla');

INSERT INTO fish_catalogue (id, scientific_name, description, meerwasserwiki_url)
VALUES (1, 'Acreichthys tomentosus', 'Seegras Feilenfisch',
        'http://meerwasserwiki.de/w/index.php?title=Acreichthys_tomentosus');

-- Test users password is: 'test'
INSERT INTO users (id, email, password, validate_token, validated, language, country, created_on, lastmod_on)
VALUES
  (1, 'sabi@bluewhale.de', '098f6bcd4621d373cade4e832627b4f6', 'NO_IDEA', 1, 'de', 'DE', CURRENT_DATE, CURRENT_DATE);

INSERT INTO aquarium (id, size, size_unit, description, active, user_id, created_on, lastmod_on)
VALUES (1, 80, 'LITER', 'Nano-Reef', 1, 1, CURRENT_DATE, CURRENT_DATE);

INSERT INTO aquarium (id, size, size_unit, description, active, user_id, created_on, lastmod_on)
VALUES (2, 200, 'LITER', 'Freshwater', 1, 1, CURRENT_DATE, CURRENT_DATE);

INSERT INTO sabi.measurement (id, measured_on, measured_value, unit_id, aquarium_id, created_on, lastmod_on)
VALUES (100, '2018-01-17 19:05:29', 27, 2, 1, CURRENT_DATE, CURRENT_DATE);
INSERT INTO sabi.measurement (id, measured_on, measured_value, unit_id, aquarium_id, created_on, lastmod_on)
VALUES (101, '2018-01-17 19:06:51', 15.5, 1, 1, CURRENT_DATE, CURRENT_DATE);

