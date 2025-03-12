/*
* 20241222
*/
alter table wvp_device_channel
    drop index uk_wvp_device_channel_unique_device_channel;
alter table wvp_device_channel
    drop index uk_wvp_unique_stream_push_id;
alter table wvp_device_channel
    drop index uk_wvp_unique_stream_proxy_id;

alter table wvp_device_channel
    add data_type integer not null;

alter table wvp_device_channel
    add data_device_id integer not null;

update wvp_device_channel wdc INNER JOIN
    (SELECT id, device_db_id from wvp_device_channel where device_db_id is not null ) ct on ct.id = wdc.id
set wdc.data_type = 1, wdc.data_device_id = ct.device_db_id where wdc.device_db_id is not null;

update wvp_device_channel wdc INNER JOIN
    (SELECT id, stream_push_id from wvp_device_channel where stream_push_id is not null ) ct on ct.id = wdc.id
set wdc.data_type = 2, wdc.data_device_id = ct.stream_push_id where wdc.stream_push_id is not null;

update wvp_device_channel wdc INNER JOIN
    (SELECT id, stream_proxy_id from wvp_device_channel where stream_proxy_id is not null ) ct on ct.id = wdc.id
set wdc.data_type = 3, wdc.data_device_id = ct.stream_proxy_id where wdc.stream_proxy_id is not null;

alter table wvp_device_channel drop device_db_id;
alter table wvp_device_channel drop stream_push_id;
alter table wvp_device_channel drop stream_proxy_id;

/*
* 20241231
*/
alter table wvp_stream_proxy add relates_media_server_id character varying(50);

/*
* 20250111
*/
drop index uk_stream_push_app_stream_path on wvp_cloud_record;
alter table wvp_cloud_record change folder folder varchar(500) null;
alter table wvp_cloud_record change file_path file_path varchar(500) null;

/*
* 20250211
*/
alter table wvp_device change keepalive_interval_time heart_beat_interval integer;
alter table wvp_device add heart_beat_count integer;
alter table wvp_device add position_capability integer;

/**
  * 20250312
 */
alter table wvp_device add server_id character varying(50);
alter table wvp_media_server add server_id character varying(50);
alter table wvp_stream_proxy add server_id character varying(50);
alter table wvp_cloud_record add server_id character varying(50);
alter table wvp_platform add server_id character varying(50);

/*  update wvp_device set server_id = "你服务的ID";
 *  update wvp_media_server set server_id = "你服务的ID";
 *  update wvp_stream_proxy set server_id = "你服务的ID";
 *  update wvp_cloud_record set server_id = "你服务的ID";
 */