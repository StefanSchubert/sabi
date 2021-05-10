update unit
set name = 'KH (mg/l HCO3)',
    description = 'Karbonathärte (KH) = Hydrogenkarbonat (HCO3), SI-Unit mg/l HCO3 | 1 °dH KH =0, 36 mmol/l = 21,8 mg/l HCO3 (Hydrogenkarbonat)'
where id = 1;

update unit
set description = 'Temperatur';

insert into unit (id,name,description) values (3,'Ca (mg/l)','Calcium');
insert into unit (id,name,description) values (4,'Mg (mg/l)','Magnesium');
insert into unit (id,name,description) values (5,'PH','PH-Wert');
insert into unit (id,name,description) values (6,'NH4+ (mg/l)','Ammonium');
insert into unit (id,name,description) values (7,'NH3 (mg/l)','Ammoniak');
insert into unit (id,name,description) values (8,'NO2- (mg/l)','Nitrit');
insert into unit (id,name,description) values (9,'NO3- (mg/l)','Nitrat');
insert into unit (id,name,description) values (10,'PO4 (mg/l)','Phosphat');
