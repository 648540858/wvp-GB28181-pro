alter table wvp_device_channel
    add stream_identification character varying(50);

alter table wvp_device
    drop switch_primary_sub_stream;

# 第一个补丁包
alter table wvp_platform
    add send_stream_ip character varying(50);

alter table wvp_device
    change on_line on_line bool default false;

alter table wvp_device
    change id id serial primary key;

alter table wvp_device
    change ssrc_check ssrc_check bool default false;