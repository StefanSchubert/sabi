INSERT INTO parameter (id, description, used_threshold_unit_id, min_threshold, max_threshold)
VALUES (3, 'Magnesium', 4, 1270, 1320);

/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

-- Convert mg/L thresholds into ppm for Carbon
update parameter
set parameter.min_threshold = 1.157,
    parameter.max_threshold = 1.78
where id = 1;