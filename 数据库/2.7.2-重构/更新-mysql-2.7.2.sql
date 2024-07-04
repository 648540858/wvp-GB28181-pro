alter table wvp_device_channel
    drop column custom_name;

alter table wvp_device_channel
    drop column channel_id;

alter table wvp_device_channel
    drop column custom_longitude;

alter table wvp_device_channel
    drop column custom_latitude;

alter table wvp_device_channel
    drop column custom_ptz_type;

alter table wvp_device_channel
    drop column longitude_gcj02;

alter table wvp_device_channel
    drop column latitude_gcj02;

alter table wvp_device_channel
    drop column longitude_wgs84;

alter table wvp_device_channel
    drop column latitude_wgs84;

alter table wvp_device_channel
    rename column manufacture to manufacturer;

alter table wvp_device_channel
    modify column parental integer;

alter table wvp_device_channel
    modify column secrecy integer;

alter table wvp_device_channel
    modify column status character varying(50);

alter table wvp_device_channel
    modify column device_id character varying(50);

alter table wvp_device_channel
    add position_type integer;

alter table wvp_device_channel
    add room_type integer;

alter table wvp_device_channel
    add use_type integer;

alter table wvp_device_channel
    add supply_light_type integer;

alter table wvp_device_channel
    add direction_type integer;

alter table wvp_device_channel
    add resolution character varying(255);

alter table wvp_device_channel
    modify column business_group_id character varying(255);

alter table wvp_device_channel
    add download_speed character varying(255);

alter table wvp_device_channel
    add svc_space_support_mod integer;

alter table wvp_device_channel
    add svc_time_support_mode integer;

alter table wvp_device_channel
    add device_db_id integer;

