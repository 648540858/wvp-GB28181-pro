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
                                 authentication_code character varying(255),
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
                                constraint uk_jt_device_id_device_id unique (terminal_db_id, channel_id)
);

ALTER table wvp_media_server ADD COLUMN IF NOT EXISTS jtt_proxy_port integer;
