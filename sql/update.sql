alter table stream_push
    add serverId varchar(50) not null;
alter table device
    add geoCoordSys varchar(50) not null;
update device set device.geoCoordSys='WGS84';
alter table device_channel
    add longitudeGcj02 double default null;
alter table device_channel
    add latitudeGcj02 double default null;
alter table device_channel
    add longitudeWgs84 double default null;
alter table device_channel
    add latitudeWgs84 double default null;



