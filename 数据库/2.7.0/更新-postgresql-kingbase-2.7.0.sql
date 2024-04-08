alter table wvp_device_channel
    add stream_identification character varying(50);

alter table wvp_device
    drop switch_primary_sub_stream;

alter table wvp_platform
    add send_stream_ip character varying(50);