alter table wvp_media_server
    add transcode_suffix character varying(255);

alter table wvp_platform_catalog
    add civil_code_for_channel character varying(50);


/* 20250207 */
alter table wvp_device_channel add gps_speed double precision;

alter table wvp_device_channel add gps_direction character varying(255);

alter table wvp_device_channel add gps_altitude character varying(255);
