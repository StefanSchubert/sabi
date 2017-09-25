INSERT INTO unit (id, name, description) VALUES (1, 'KH', 'Karbonathärte / Alkanität');
INSERT INTO unit (id, name, description) VALUES (2, '°C', 'Grad Celsius');

INSERT INTO parameter (id, description, used_threshold_unit_id, min_threshold, max_threshold)
VALUES (1, 'Karbonathärte', 1, 6.5, 10);

INSERT INTO parameter (id, description, used_threshold_unit_id, min_threshold, max_threshold)
VALUES (2, 'Temperatur', 2, 24, 27);

INSERT INTO remedy (id, productname, vendor) VALUES (1, 'KH+', 'Dupla');

INSERT INTO fish_catalogue (id, scientific_name, description, meerwasserwiki_url)
VALUES (1, 'Acreichthys tomentosus', 'Seegras Feilenfisch', 'http://meerwasserwiki.de/w/index.php?title=Acreichthys_tomentosus');

-- Test users password is: 'UNHASHED_NONSENCE'
insert into users (id,email,password,validate_token, validated, language, country, created_on, lastmod_on)
values (1, 'sabi@bluewhale.de', '0380e26cca8b16cc581278450d91b1a9','NO_IDEA',1 ,'de','DE' ,CURRENT_DATE ,CURRENT_DATE );

insert into aquarium (id,size,size_unit, description, active, user_id, created_on, lastmod_on)
values (1,80,'LITER','Nano-Reef',1,1,CURRENT_DATE , CURRENT_DATE );

insert into aquarium (id,size,size_unit, description, active, user_id, created_on, lastmod_on)
values (2,200,'LITER','Freshwater',1,1,CURRENT_DATE , CURRENT_DATE );

