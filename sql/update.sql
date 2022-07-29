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
alter table device_channel
    add gpsTime varchar(50) default null;


alter table device_mobile_position
    change  cnLng longitudeGcj02 double default null;
alter table device_mobile_position
    change  cnLat latitudeGcj02 double default null;
alter table device_mobile_position
    add longitudeWgs84 double default null;
alter table device_mobile_position
    add latitudeWgs84 double default null;
alter table device_mobile_position
    drop geodeticSystem;
alter table device_mobile_position
    add createTime varchar(50) default null;

alter table device_alarm
    add createTime varchar(50) default null;

alter table gb_stream
    change createStamp createTime varchar(50) default null;

alter table parent_platform
    add createTime varchar(50) default null;
alter table parent_platform
    add updateTime varchar(50) default null;

alter table stream_proxy
    add updateTime varchar(50) default null;

alter table stream_push
    add pushTime varchar(50) default null;
alter table stream_push
    add status int DEFAULT NULL;
alter table stream_push
    add updateTime varchar(50) default null;
alter table stream_push
    add pushIng int DEFAULT NULL;
alter table stream_push
    change createStamp createTime varchar(50) default null;

alter table gb_stream
    drop column status;

alter table user
    add pushKey varchar(50) default null;
update user set pushKey='453df297a57a5a7438934sda801fc3' where id=1;

alter table parent_platform
    add treeType varchar(50) not null;
update parent_platform set parent_platform.treeType='BusinessGroup';
alter table parent_platform drop shareAllLiveStream;

alter table platform_catalog
    add civilCode varchar(50) default null;
alter table platform_catalog
    add businessGroupId varchar(50) default null;

/********************* ADD ***************************/
alter table stream_push
    add self int DEFAULT NULL;


