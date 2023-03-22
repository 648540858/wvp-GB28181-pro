alter table device
    add asMessageChannel int default 0;

alter table parent_platform
    add asMessageChannel int default 0;

alter table device
    add mediaServerId varchar(50) default null;




