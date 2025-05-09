/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

alter table parameter drop foreign key parameter_ibfk_1;

alter table parameter
    change used_threshold_unit_id belonging_unit_id int unsigned not null;

alter table parameter
    add constraint parameter_unit
        foreign key (belonging_unit_id) references unit (id);