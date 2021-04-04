/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

alter table unit modify name VARCHAR(20) not null;

update unit
set name = 'KH (mmol/l CaCO3)',
    description = 'Karbonathärte (1 °DH = 0,178 mmol/l CAO3)'
where id = 1;
