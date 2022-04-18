alter table parent_platform
    add startOfflinePush int default 0 null;

alter table parent_platform
    add administrativeDivision varchar(50) not null;

alter table parent_platform
    add catalogGroup int default 1 null;

alter table device
    add ssrcCheck int default 0 null;

