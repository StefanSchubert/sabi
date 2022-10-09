INSERT INTO unit (id, name, description) VALUES (11, 'Salinity (g/ml)', 'Salinity');

INSERT INTO parameter (id, description, belonging_unit_id, min_threshold, max_threshold)
VALUES (7, 'Salzgehalt', 11,  1.020,  1.026);
