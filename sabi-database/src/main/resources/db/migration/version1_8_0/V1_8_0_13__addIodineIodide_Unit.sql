/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

-- Add measurement unit for iodine/iodide concentration
INSERT INTO unit (id, name)
VALUES (12, 'I (mg/l)');

INSERT INTO localized_unit
(unit_id, description, language, created_on, lastmod_on, optlock)
VALUES
    (12, 'Iod-/Iodid-Konzentration in mg/L', 'de', DEFAULT, DEFAULT, DEFAULT),
    (12, 'Iodine/Iodide concentration in mg/L', 'en', DEFAULT, DEFAULT, DEFAULT),
    (12, 'Concentración de yodo/yoduro en mg/L', 'es', DEFAULT, DEFAULT, DEFAULT),
    (12, 'Concentration en iode/iodure en mg/L', 'fr', DEFAULT, DEFAULT, DEFAULT),
    (12, 'Concentrazione di iodio/ioduro in mg/L', 'it', DEFAULT, DEFAULT, DEFAULT);

-- Add iodine/iodide measurement parameter
INSERT INTO parameter
(id, belonging_unit_id, min_threshold, max_threshold)
VALUES
    (9, 12, 0.04, 0.08);

INSERT INTO localized_parameter
(parameter_id, description, language, created_on, lastmod_on, optlock)
VALUES
    (9, 'Iod/Iodid', 'de', DEFAULT, DEFAULT, DEFAULT),
    (9, 'Iodine/Iodide', 'en', DEFAULT, DEFAULT, DEFAULT),
    (9, 'Yodo/yoduro', 'es', DEFAULT, DEFAULT, DEFAULT),
    (9, 'Iode/iodure', 'fr', DEFAULT, DEFAULT, DEFAULT),
    (9, 'Iodio/ioduro', 'it', DEFAULT, DEFAULT, DEFAULT);