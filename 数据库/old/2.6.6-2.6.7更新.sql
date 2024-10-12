alter table device
    add asMessageChannel int default 0;

alter table parent_platform
    add asMessageChannel int default 0;

alter table device
    add mediaServerId varchar(50) default null;

ALTER TABLE device
    ADD COLUMN `switchPrimarySubStream` bit(1) NOT NULL DEFAULT b'0' COMMENT '开启主子码流切换的开关（0-不开启，1-开启）现在已知支持设备为 大华、TP——LINK全系设备' AFTER `keepalive_interval_time`


