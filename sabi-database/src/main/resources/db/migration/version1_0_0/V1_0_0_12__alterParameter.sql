/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

alter table sabi.parameter drop foreign key parameter_ibfk_1;

alter table sabi.parameter
    change used_threshold_unit_id belonging_unit_id int unsigned not null;

alter table sabi.parameter
    add constraint parameter_unit
        foreign key (belonging_unit_id) references unit (id);