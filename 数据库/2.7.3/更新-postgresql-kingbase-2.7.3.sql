/*
* 20241222
*/

alter table wvp_device_channel
    add data_type integer;

alter table wvp_device_channel
    add data_device_id integer;

update wvp_device_channel wdc
set data_type = 1, data_device_id = (SELECT device_db_id from wvp_device_channel where device_db_id is not null and id = wdc.id )  where device_db_id is not null;

update wvp_device_channel wdc
set data_type = 2, data_device_id = (SELECT stream_push_id from wvp_device_channel where stream_push_id is not null and id = wdc.id )  where stream_push_id is not null;

update wvp_device_channel wdc
set data_type = 1, data_device_id = (SELECT stream_proxy_id from wvp_device_channel where stream_proxy_id is not null and id = wdc.id )  where stream_proxy_id is not null;

alter table wvp_device_channel drop device_db_id;
alter table wvp_device_channel drop stream_push_id;
alter table wvp_device_channel drop stream_proxy_id;