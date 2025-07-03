/*
* 20240528
*/
DELIMITER //  -- 重定义分隔符避免分号冲突
CREATE PROCEDURE `wvp_20240528`()
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_media_server' and column_name = 'transcode_suffix')
    THEN
        ALTER TABLE wvp_media_server ADD transcode_suffix  character varying(255);
    END IF;

    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_media_server' and column_name = 'type')
    THEN
        alter table wvp_media_server
            add  type character varying(50) default 'zlm';
    END IF;

    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_media_server' and column_name = 'flv_port')
    THEN
        alter table wvp_media_server  add flv_port integer;
    END IF;

    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_media_server' and column_name = 'flv_ssl_port')
    THEN
        alter table wvp_media_server add flv_ssl_port integer;
    END IF;

    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_media_server' and column_name = 'ws_flv_port')
    THEN
        alter table wvp_media_server add ws_flv_port integer;
    END IF;

    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_media_server' and column_name = 'ws_flv_ssl_port')
    THEN
        alter table wvp_media_server add ws_flv_ssl_port integer;
    END IF;
END; //
call wvp_20240528();
DROP PROCEDURE wvp_20240528;
DELIMITER ;

create table IF NOT EXISTS wvp_user_api_key (
                                  id serial primary key ,
                                  user_id bigint,
                                  app character varying(255) ,
                                  api_key text,
                                  expired_at bigint,
                                  remark character varying(255),
                                  enable bool default true,
                                  create_time character varying(50),
                                  update_time character varying(50)
);

/*
* 20241222
*/
DELIMITER //  -- 重定义分隔符避免分号冲突
CREATE PROCEDURE `wvp_20241222`()
BEGIN
    IF EXISTS (SELECT column_name FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_device_channel' and INDEX_NAME = 'uk_wvp_device_channel_unique_device_channel')
    THEN
        alter table wvp_device_channel drop index uk_wvp_device_channel_unique_device_channel;
    END IF;

    IF EXISTS (SELECT column_name FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_device_channel' and INDEX_NAME = 'uk_wvp_unique_stream_push_id')
    THEN
        alter table wvp_device_channel drop index uk_wvp_unique_stream_push_id;
    END IF;

    IF EXISTS (SELECT column_name FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_device_channel' and INDEX_NAME = 'uk_wvp_unique_stream_proxy_id')
    THEN
        alter table wvp_device_channel drop index uk_wvp_unique_stream_proxy_id;
    END IF;

    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_device_channel' and column_name = 'data_type')
    THEN
        alter table wvp_device_channel add data_type integer not null;
    END IF;

    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_device_channel' and column_name = 'data_device_id')
    THEN
        alter table wvp_device_channel add data_device_id integer not null;
    END IF;

    IF EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_device_channel' and column_name = 'device_db_id')
    THEN
        update wvp_device_channel wdc INNER JOIN
            (SELECT id, device_db_id from wvp_device_channel where device_db_id is not null ) ct on ct.id = wdc.id
        set wdc.data_type = 1, wdc.data_device_id = ct.device_db_id where wdc.device_db_id is not null;
        alter table wvp_device_channel drop device_db_id;
    END IF;

    IF EXISTS (SELECT column_name FROM information_schema.columns
               WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_device_channel' and column_name = 'stream_push_id')
    THEN
        update wvp_device_channel wdc INNER JOIN
            (SELECT id, stream_push_id from wvp_device_channel where stream_push_id is not null ) ct on ct.id = wdc.id
        set wdc.data_type = 2, wdc.data_device_id = ct.stream_push_id where wdc.stream_push_id is not null;
        alter table wvp_device_channel drop stream_push_id;
    END IF;

    IF EXISTS (SELECT column_name FROM information_schema.columns
               WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_device_channel' and column_name = 'stream_proxy_id')
    THEN
        update wvp_device_channel wdc INNER JOIN
            (SELECT id, stream_proxy_id from wvp_device_channel where stream_proxy_id is not null ) ct on ct.id = wdc.id
        set wdc.data_type = 3, wdc.data_device_id = ct.stream_proxy_id where wdc.stream_proxy_id is not null;
        alter table wvp_device_channel drop stream_proxy_id;
    END IF;
END; //
call wvp_20241222();
DROP PROCEDURE wvp_20241222;
DELIMITER ;
/*
* 20241231
*/
DELIMITER //
CREATE PROCEDURE `wvp_20241231`()
BEGIN
    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_stream_proxy' and column_name = 'relates_media_server_id')
    THEN
        alter table wvp_stream_proxy add relates_media_server_id character varying(50);
    END IF;
END; //
call wvp_20241231();
DROP PROCEDURE wvp_20241231;
DELIMITER ;
/*
* 20250111
*/
DELIMITER //  -- 重定义分隔符避免分号冲突
CREATE PROCEDURE `wvp_20250111`()
BEGIN
    IF EXISTS (SELECT column_name FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_cloud_record' and INDEX_NAME = 'uk_stream_push_app_stream_path')
    THEN
        alter table wvp_cloud_record drop index uk_stream_push_app_stream_path ;
    END IF;

    IF EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_cloud_record' and column_name = 'folder')
    THEN
        alter table wvp_cloud_record modify folder varchar(500) null;
    END IF;

    IF EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_cloud_record' and column_name = 'file_path')
    THEN
        alter table wvp_cloud_record modify file_path varchar(500) null;
    END IF;
END; //
call wvp_20250111();
DROP PROCEDURE wvp_20250111;
DELIMITER ;

/*
* 20250211
*/
DELIMITER //  -- 重定义分隔符避免分号冲突
CREATE PROCEDURE `wvp_20250211`()
BEGIN
    IF EXISTS (SELECT column_name FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_device' and column_name = 'keepalive_interval_time')
    THEN
        alter table wvp_device change keepalive_interval_time heart_beat_interval integer after as_message_channel;
    END IF;

    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_device' and column_name = 'heart_beat_count')
    THEN
        alter table wvp_device add heart_beat_count integer;
    END IF;

    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_device' and column_name = 'position_capability')
    THEN
        alter table wvp_device add position_capability integer;
    END IF;
END; //
call wvp_20250211();
DROP PROCEDURE wvp_20250211;
DELIMITER ;

/**
  * 20250312
 */
DELIMITER //  -- 重定义分隔符避免分号冲突
CREATE PROCEDURE `wvp_20250312`()
BEGIN
    DECLARE serverId VARCHAR(32) DEFAULT '你的服务ID';
    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_device' and column_name = 'server_id')
    THEN
        alter table wvp_device add server_id character varying(50);
        update wvp_device set server_id = serverId;
    END IF;

    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_media_server' and column_name = 'server_id')
    THEN
        alter table wvp_media_server add server_id character varying(50);
        update wvp_media_server set server_id = serverId;
    END IF;

    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_stream_proxy' and column_name = 'server_id')
    THEN
        alter table wvp_stream_proxy add server_id character varying(50);
        update wvp_stream_proxy set server_id = serverId;
    END IF;

    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_cloud_record' and column_name = 'server_id')
    THEN
        alter table wvp_cloud_record add server_id character varying(50);
        update wvp_cloud_record set server_id = serverId;
    END IF;

    IF not EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and table_name = 'wvp_platform' and column_name = 'server_id')
    THEN
        alter table wvp_platform add server_id character varying(50);
    END IF;
END; //
call wvp_20250312();
DROP PROCEDURE wvp_20250312;
DELIMITER ;

/*
* 20250319
*/
DELIMITER //  -- 重定义分隔符避免分号冲突
CREATE PROCEDURE `wvp_20250319`()
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
               WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_device_channel' and column_name = 'gps_speed')
    THEN
        alter table wvp_device_channel add gps_speed double precision;
    END IF;

    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_device_channel' and column_name = 'gps_altitude')
    THEN
        alter table wvp_device_channel add gps_altitude double precision;
    END IF;

    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_device_channel' and column_name = 'gps_direction')
    THEN
        alter table wvp_device_channel add gps_direction double precision;
    END IF;
END; //
call wvp_20250319();
DROP PROCEDURE wvp_20250319;
DELIMITER ;

/*
* 20250319
*/
update wvp_record_plan_item set start = start * 30, stop = (stop + 1) * 30

/*
* 20250402
*/
DELIMITER //  -- 重定义分隔符避免分号冲突
CREATE PROCEDURE `wvp_20250402`()
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_device_channel' and INDEX_NAME = 'data_type')
    THEN
        create index data_type on wvp_device_channel (data_type);
    END IF;
    IF NOT EXISTS (SELECT column_name FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_device_channel' and INDEX_NAME = 'data_device_id')
    THEN
        create index data_device_id on wvp_device_channel (data_device_id);
    END IF;

END; //
call wvp_20250402();
DROP PROCEDURE wvp_20250402;
DELIMITER ;

/**
* 20250414
*/
alter table wvp_cloud_record modify time_len double precision;



