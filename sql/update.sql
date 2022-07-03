alter table stream_push
    add serverId varchar(50) not null;
alter table device
    add geoCoordSys varchar(50) not null;
alter table device
    add treeType varchar(50) not null;
update device set device.geoCoordSys='WGS84';
update device set device.treeType='CivilCode';
alter table device_channel
    add longitudeGcj02 double default null;
alter table device_channel
    add latitudeGcj02 double default null;
alter table device_channel
    add longitudeWgs84 double default null;
alter table device_channel
    add latitudeWgs84 double default null;
alter table device_channel
    add businessGroupId varchar(50) default null;


alter table device_mobile_position change  cnLng longitudeGcj02 double default null;
alter table device_mobile_position change  cnLat latitudeGcj02 double default null;
alter table device_mobile_position
    add longitudeWgs84 double default null;
alter table device_mobile_position
    add latitudeWgs84 double default null;
alter table device_mobile_position drop geodeticSystem;



