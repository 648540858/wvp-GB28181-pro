alter table wvp_device_channel
    change stream_id stream_id varying(255)

alter table wvp_device_channel
    add common_gb_channel_id int
