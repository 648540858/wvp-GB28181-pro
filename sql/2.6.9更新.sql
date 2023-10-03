alter table wvp_device_channel
    change stream_id stream_id character varying(255)

alter table wvp_platform
    add auto_push_channel bool default false

alter table wvp_stream_proxy
    add stream_key character varying(255)

alter table wvp_media_server
    add record_assist_ip character varying(50)
