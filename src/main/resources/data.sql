create table hero
	(id identity primary key, name varchar(20));
alter table hero add constraint ak_hero unique (name);

insert into hero(name) values('Ms Nice');
insert into hero(name) values('Nurco');
insert into hero(name) values('Bombastico');
insert into hero(name) values('Fennel');
insert into hero(name) values('Magnostic');
insert into hero(name) values('Duck Man');
insert into hero(name) values('Silly Putty Man');
insert into hero(name) values('Dr Low IQ');
