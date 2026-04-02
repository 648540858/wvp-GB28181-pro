drop table IF EXISTS wvp_jt_terminal;
create table IF NOT EXISTS wvp_jt_terminal (
                                 id serial primary key,
                                 phone_number character varying(50),
                                 terminal_id character varying(50),
                                 province_id character varying(50),
                                 province_text character varying(100),
                                 city_id character varying(50),
                                 city_text character varying(100),
                                 maker_id character varying(50),
                                 model character varying(50),
                                 plate_color character varying(50),
                                 plate_no character varying(50),
                                 longitude double precision,
                                 latitude double precision,
                                 status bool default false,
                                 register_time character varying(50) default null,
                                 update_time character varying(50) not null,
                                 create_time character varying(50) not null,
                                 geo_coord_sys character varying(50),
                                 media_server_id character varying(50) default 'auto',
                                 sdp_ip character varying(50),
                                 constraint uk_jt_device_id_device_id unique (id, phone_number)
);
drop table IF EXISTS wvp_jt_channel;
create table IF NOT EXISTS wvp_jt_channel (
                                id serial primary key,
                                terminal_db_id integer,
                                channel_id integer,
                                has_audio bool default false,
                                name character varying(255),
                                update_time character varying(50) not null,
                                create_time character varying(50) not null,
                                constraint uk_jt_channel_id_device_id unique (terminal_db_id, channel_id)
);

ALTER table wvp_media_server ADD COLUMN IF NOT EXISTS jtt_proxy_port integer;
ALTER table wvp_media_server ADD COLUMN IF NOT EXISTS mp4_port integer;
ALTER table wvp_media_server ADD COLUMN IF NOT EXISTS mp4_ssl_port integer;

ALTER table wvp_device_channel ADD COLUMN IF NOT EXISTS enable_broadcast integer default 0;
ALTER table wvp_device_channel ADD COLUMN IF NOT EXISTS map_level integer default 0;
ALTER table wvp_common_group ADD COLUMN IF NOT EXISTS alias varchar(255) default null;
ALTER table wvp_stream_proxy DROP COLUMN IF EXISTS enable_remove_none_reader;

drop index uk_media_server_unique_ip_http_port on wvp_media_server;

ALTER table wvp_device DROP COLUMN IF EXISTS register_time;
ALTER table wvp_device DROP COLUMN IF EXISTS keepalive_time;

drop table IF EXISTS wvp_alarm;
create table IF NOT EXISTS wvp_alarm (
    id serial primary key,
    channel_id integer,
    description character varying(255),
    snap_path character varying(255),
    record_path character varying(255),
    longitude double precision,
    latitude double precision,
    alarm_type integer,
    alarm_time bigint
)
COMMENT ON COLUMN wvp_alarm.id IS '主键ID';
COMMENT ON COLUMN wvp_alarm.channel_id IS '关联通道的数据库id';
COMMENT ON COLUMN wvp_alarm.description IS '报警描述';
COMMENT ON COLUMN wvp_alarm.snap_path IS '报警快照路径';
COMMENT ON COLUMN wvp_alarm.record_path IS '报警录像路径';
COMMENT ON COLUMN wvp_alarm.longitude IS '报警附带的经度';
COMMENT ON COLUMN wvp_alarm.latitude IS '报警附带的纬度';
COMMENT ON COLUMN wvp_alarm.alarm_type IS '报警类别';
COMMENT ON COLUMN wvp_alarm.alarm_time IS '报警时间';
