alter table media_server
    drop column streamNoneReaderDelayMS;

alter table stream_proxy
    add enable_disable_none_reader bit(1) default null;

alter table device
    add mediaServerId varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'auto';

alter table device
    add custom_name varchar(255) default null;

alter table device
    add sdpIp varchar(50) default null;

alter table device
    add password varchar(255) default null;

alter table device
    modify ip varchar(50) null;

alter table device
    modify port int null;

alter table device
    modify expires int null;

alter table device
    modify subscribeCycleForCatalog int null;

alter table device
    modify hostAddress varchar(50) null;
