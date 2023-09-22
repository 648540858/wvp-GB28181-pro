alter table wvp_device_channel
    change stream_id stream_id varying(255)

alter table wvp_platform
    add auto_push_channel bool default false
