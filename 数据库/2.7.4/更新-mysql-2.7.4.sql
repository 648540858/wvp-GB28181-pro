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

/*
* 20250708
*/
DELIMITER //  -- 重定义分隔符避免分号冲突
CREATE PROCEDURE `wvp_20250708`()
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_media_server' and column_name = 'jtt_proxy_port')
    THEN
        ALTER TABLE wvp_media_server ADD jtt_proxy_port  integer;
    END IF;
END; //
call wvp_20250708();
DROP PROCEDURE wvp_20250708;
DELIMITER ;

/*
* 20250917
*/
DELIMITER //  -- 重定义分隔符避免分号冲突
CREATE PROCEDURE `wvp_20250917`()
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_media_server' and column_name = 'mp4_port')
    THEN
        ALTER TABLE wvp_media_server ADD mp4_port integer;
    END IF;

    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_media_server' and column_name = 'mp4_ssl_port')
    THEN
        ALTER TABLE wvp_media_server ADD mp4_ssl_port integer;
    END IF;
END; //
call wvp_20250917();
DROP PROCEDURE wvp_20250917;
DELIMITER ;

/*
* 20250924
*/
DELIMITER //  -- 重定义分隔符避免分号冲突
CREATE PROCEDURE `wvp_20250924`()
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_device_channel' and column_name = 'enable_ptz')
    THEN
        ALTER TABLE wvp_device_channel ADD enable_ptz integer default 0;
    END IF;

    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_device_channel' and column_name = 'enable_broadcast')
    THEN
        ALTER TABLE wvp_device_channel ADD enable_broadcast integer default 0;
    END IF;
END; //
call wvp_20250924();
DROP PROCEDURE wvp_20250924;
DELIMITER ;






