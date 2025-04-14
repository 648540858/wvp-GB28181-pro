drop table if exists wvp_resources_tree;
drop table if exists wvp_platform_catalog;
drop table if exists wvp_platform_gb_stream;
drop table if exists wvp_platform_gb_channel;
drop table if exists wvp_gb_stream;
drop table if exists wvp_log;
drop table IF EXISTS wvp_device;
drop table IF EXISTS wvp_platform;
drop table IF EXISTS wvp_media_server;
drop table IF EXISTS wvp_device_mobile_position;
drop table IF EXISTS wvp_device_channel;
drop table IF EXISTS wvp_stream_proxy;
drop table IF EXISTS wvp_stream_push;

create table IF NOT EXISTS wvp_device
(
    id                                  serial primary key,
    device_id                           character varying(50) not null,
    name                                character varying(255),
    manufacturer                        character varying(255),
    model                               character varying(255),
    firmware                            character varying(255),
    transport                           character varying(50),
    stream_mode                         character varying(50),
    on_line                             bool    default false,
    register_time                       character varying(50),
    keepalive_time                      character varying(50),
    ip                                  character varying(50),
    create_time                         character varying(50),
    update_time                         character varying(50),
    port                                integer,
    expires                             integer,
    subscribe_cycle_for_catalog         integer DEFAULT 0,
    subscribe_cycle_for_mobile_position integer DEFAULT 0,
    mobile_position_submission_interval integer DEFAULT 5,
    subscribe_cycle_for_alarm           integer DEFAULT 0,
    host_address                        character varying(50),
    charset                             character varying(50),
    ssrc_check                          bool    default false,
    geo_coord_sys                       character varying(50),
    media_server_id                     character varying(50) default 'auto',
    custom_name                         character varying(255),
    sdp_ip                              character varying(50),
    local_ip                            character varying(50),
    password                            character varying(255),
    as_message_channel                  bool    default false,
    heart_beat_interval                 integer,
    heart_beat_count                    integer,
    position_capability                 integer,
    broadcast_push_after_ack            bool    default false,
    server_id                           character varying(50),
    constraint uk_device_device unique (device_id)
);

create table IF NOT EXISTS wvp_device_channel
(
    id                           serial primary key,
    device_id                    character varying(50),
    name                         character varying(255),
    manufacturer                 character varying(50),
    model                        character varying(50),
    owner                        character varying(50),
    civil_code                   character varying(50),
    block                        character varying(50),
    address                      character varying(50),
    parental                     integer,
    parent_id                    character varying(50),
    safety_way                   integer,
    register_way                 integer,
    cert_num                     character varying(50),
    certifiable                  integer,
    err_code                     integer,
    end_time                     character varying(50),
    secrecy                      integer,
    ip_address                   character varying(50),
    port                         integer,
    password                     character varying(255),
    status                       character varying(50),
    longitude                    double precision,
    latitude                     double precision,
    ptz_type                     integer,
    position_type                integer,
    room_type                    integer,
    use_type                     integer,
    supply_light_type            integer,
    direction_type               integer,
    resolution                   character varying(255),
    business_group_id            character varying(255),
    download_speed               character varying(255),
    svc_space_support_mod        integer,
    svc_time_support_mode        integer,
    create_time                  character varying(50) not null,
    update_time                  character varying(50) not null,
    sub_count                    integer,
    stream_id                    character varying(255),
    has_audio                    bool default false,
    gps_time                     character varying(50),
    stream_identification        character varying(50),
    channel_type                 int  default 0        not null,
    gb_device_id                 character varying(50),
    gb_name                      character varying(255),
    gb_manufacturer              character varying(255),
    gb_model                     character varying(255),
    gb_owner                     character varying(255),
    gb_civil_code                character varying(255),
    gb_block                     character varying(255),
    gb_address                   character varying(255),
    gb_parental                  integer,
    gb_parent_id                 character varying(255),
    gb_safety_way                integer,
    gb_register_way              integer,
    gb_cert_num                  character varying(50),
    gb_certifiable               integer,
    gb_err_code                  integer,
    gb_end_time                  character varying(50),
    gb_secrecy                   integer,
    gb_ip_address                character varying(50),
    gb_port                      integer,
    gb_password                  character varying(50),
    gb_status                    character varying(50),
    gb_longitude                 double precision,
    gb_latitude                  double precision,
    gb_business_group_id         character varying(50),
    gb_ptz_type                  integer,
    gb_position_type             integer,
    gb_room_type                 integer,
    gb_use_type                  integer,
    gb_supply_light_type         integer,
    gb_direction_type            integer,
    gb_resolution                character varying(255),
    gb_download_speed            character varying(255),
    gb_svc_space_support_mod     integer,
    gb_svc_time_support_mode     integer,
    record_plan_id               integer,
    data_type                    integer not null,
    data_device_id               integer not null,
    gps_speed                    double precision,
    gps_altitude                 double precision,
    gps_direction                double precision,
    constraint uk_wvp_unique_channel unique (gb_device_id)
);

create index if not exists data_type on wvp_device_channel (data_type);
create index if not exists data_device_id on wvp_device_channel (data_device_id);

create table IF NOT EXISTS wvp_device_mobile_position
(
    id              serial primary key,
    device_id       character varying(50) not null,
    channel_id      character varying(50) not null,
    device_name     character varying(255),
    time            character varying(50),
    longitude       double precision,
    latitude        double precision,
    altitude        double precision,
    speed           double precision,
    direction       double precision,
    report_source   character varying(50),
    create_time     character varying(50)
);

create table IF NOT EXISTS wvp_media_server
(
    id                  character varying(255) primary key,
    ip                  character varying(50),
    hook_ip             character varying(50),
    sdp_ip              character varying(50),
    stream_ip           character varying(50),
    http_port           integer,
    http_ssl_port       integer,
    rtmp_port           integer,
    rtmp_ssl_port       integer,
    rtp_proxy_port      integer,
    rtsp_port           integer,
    rtsp_ssl_port       integer,
    flv_port            integer,
    flv_ssl_port        integer,
    ws_flv_port         integer,
    ws_flv_ssl_port     integer,
    auto_config         bool                  default false,
    secret              character varying(50),
    type                character varying(50) default 'zlm',
    rtp_enable          bool                  default false,
    rtp_port_range      character varying(50),
    send_rtp_port_range character varying(50),
    record_assist_port  integer,
    default_server      bool                  default false,
    create_time         character varying(50),
    update_time         character varying(50),
    hook_alive_interval integer,
    record_path         character varying(255),
    record_day          integer               default 7,
    transcode_suffix    character varying(255),
    server_id           character varying(50),
    constraint uk_media_server_unique_ip_http_port unique (ip, http_port, server_id)
);

create table IF NOT EXISTS wvp_common_group
(
    id               serial primary key,
    device_id        varchar(50)  NOT NULL,
    name             varchar(255) NOT NULL,
    parent_id        int,
    parent_device_id varchar(50) DEFAULT NULL,
    business_group   varchar(50)  NOT NULL,
    create_time      varchar(50)  NOT NULL,
    update_time      varchar(50)  NOT NULL,
    civil_code       varchar(50) default null,
    constraint uk_common_group_device_platform unique (device_id)
);

create table IF NOT EXISTS wvp_common_region
(
    id               serial primary key,
    device_id        varchar(50)  NOT NULL,
    name             varchar(255) NOT NULL,
    parent_id        int,
    parent_device_id varchar(50) DEFAULT NULL,
    create_time      varchar(50)  NOT NULL,
    update_time      varchar(50)  NOT NULL,
    constraint uk_common_region_device_id unique (device_id)
);

create table IF NOT EXISTS wvp_record_plan
(
    id              serial primary key,
    snap            bool default false,
    name            varchar(255) NOT NULL,
    create_time     character varying(50),
    update_time     character varying(50)
);

create table IF NOT EXISTS wvp_record_plan_item
(
    id              serial primary key,
    "start"           int,
    stop            int,
    week_day        int,
    plan_id        int,
    create_time     character varying(50),
    update_time     character varying(50)
);

create table IF NOT EXISTS wvp_platform
(
    id                    serial primary key,
    enable                bool default false,
    name                  character varying(255),
    server_gb_id          character varying(50),
    server_gb_domain      character varying(50),
    server_ip             character varying(50),
    server_port           integer,
    device_gb_id          character varying(50),
    device_ip             character varying(50),
    device_port           character varying(50),
    username              character varying(255),
    password              character varying(50),
    expires               character varying(50),
    keep_timeout          character varying(50),
    transport             character varying(50),
    civil_code            character varying(50),
    manufacturer          character varying(255),
    model                 character varying(255),
    address               character varying(255),
    character_set         character varying(50),
    ptz                   bool default false,
    rtcp                  bool default false,
    status                bool default false,
    catalog_group         integer,
    register_way          integer,
    secrecy               integer,
    create_time           character varying(50),
    update_time           character varying(50),
    as_message_channel    bool default false,
    catalog_with_platform integer default 1,
    catalog_with_group    integer default 1,
    catalog_with_region   integer default 1,
    auto_push_channel     bool default true,
    send_stream_ip        character varying(50),
    server_id             character varying(50),
    constraint uk_platform_unique_server_gb_id unique (server_gb_id)
);

create table IF NOT EXISTS wvp_platform_channel
(
    id                           serial primary key,
    platform_id                  integer,
    device_channel_id            integer,
    custom_device_id             character varying(50),
    custom_name                  character varying(255),
    custom_manufacturer          character varying(50),
    custom_model                 character varying(50),
    custom_owner                 character varying(50),
    custom_civil_code            character varying(50),
    custom_block                 character varying(50),
    custom_address               character varying(50),
    custom_parental              integer,
    custom_parent_id             character varying(50),
    custom_safety_way            integer,
    custom_register_way          integer,
    custom_cert_num              character varying(50),
    custom_certifiable           integer,
    custom_err_code              integer,
    custom_end_time              character varying(50),
    custom_secrecy               integer,
    custom_ip_address            character varying(50),
    custom_port                  integer,
    custom_password              character varying(255),
    custom_status                character varying(50),
    custom_longitude             double precision,
    custom_latitude              double precision,
    custom_ptz_type              integer,
    custom_position_type         integer,
    custom_room_type             integer,
    custom_use_type              integer,
    custom_supply_light_type     integer,
    custom_direction_type        integer,
    custom_resolution            character varying(255),
    custom_business_group_id     character varying(255),
    custom_download_speed        character varying(255),
    custom_svc_space_support_mod integer,
    custom_svc_time_support_mode integer,
    constraint uk_platform_gb_channel_platform_id_catalog_id_device_channel_id unique (platform_id, device_channel_id),
    constraint uk_platform_gb_channel_device_id unique (custom_device_id)
);

create table IF NOT EXISTS wvp_platform_group
(
    id          serial primary key,
    platform_id integer,
    group_id    integer,
    constraint uk_wvp_platform_group_platform_id_group_id unique (platform_id, group_id)
);

create table IF NOT EXISTS wvp_platform_region
(
    id          serial primary key,
    platform_id integer,
    region_id   integer,
    constraint uk_wvp_platform_region_platform_id_group_id unique (platform_id, region_id)
);

create table IF NOT EXISTS wvp_stream_proxy
(
    id                         serial primary key,
    type                       character varying(50),
    app                        character varying(255),
    stream                     character varying(255),
    src_url                    character varying(255),
    timeout                    integer,
    ffmpeg_cmd_key             character varying(255),
    rtsp_type                  character varying(50),
    media_server_id            character varying(50),
    enable_audio               bool default false,
    enable_mp4                 bool default false,
    pulling                    bool default false,
    enable                     bool default false,
    enable_remove_none_reader  bool default false,
    create_time                character varying(50),
    name                       character varying(255),
    update_time                character varying(50),
    stream_key                 character varying(255),
    server_id                  character varying(50),
    enable_disable_none_reader bool default false,
    relates_media_server_id    character varying(50),
    constraint uk_stream_proxy_app_stream unique (app, stream)
);

create table IF NOT EXISTS wvp_stream_push
(
    id                 serial primary key,
    app                character varying(255),
    stream             character varying(255),
    create_time        character varying(50),
    media_server_id    character varying(50),
    server_id          character varying(50),
    push_time          character varying(50),
    status             bool default false,
    update_time        character varying(50),
    pushing            bool default false,
    self               bool default false,
    start_offline_push bool default true,
    constraint uk_stream_push_app_stream unique (app, stream)
);
alter table wvp_cloud_record add column if not exists server_id character varying(50);
ALTER TABLE wvp_cloud_record DROP CONSTRAINT IF EXISTS uk_stream_push_app_stream_path;
alter table wvp_cloud_record alter folder  type varchar(500);
alter table wvp_cloud_record alter file_path type varchar(500);
update wvp_cloud_record set server_id = '你的服务ID';

/**
* 20250414
*/
alter table wvp_cloud_record modify time_len double precision;
