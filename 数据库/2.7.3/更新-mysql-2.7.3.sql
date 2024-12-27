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