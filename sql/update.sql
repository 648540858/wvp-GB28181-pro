ALTER TABLE stream_proxy ADD status bit(1) not null;

# 去除子查询优化查询速度
alter table device_channel
    add subCount int default 0 null;

update device_channel dc set dc.subCount = (select  te.count from (SELECT count(0) as count FROM device_channel WHERE parentId = dc.channelId) te)