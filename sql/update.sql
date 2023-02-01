-- 2.6.6->2.6.7
alter table device
    add keepaliveIntervalTime int default null;