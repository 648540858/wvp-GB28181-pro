/*
* 20240528
*/
ALTER TABLE wvp_media_server ADD COLUMN IF NOT EXISTS transcode_suffix  character varying(255);
ALTER table wvp_media_server ADD COLUMN IF NOT EXISTS type character varying(50) default 'zlm';
ALTER table wvp_media_server ADD COLUMN IF NOT EXISTS flv_port integer;
ALTER table wvp_media_server ADD COLUMN IF NOT EXISTS flv_ssl_port integer;
ALTER table wvp_media_server ADD COLUMN IF NOT EXISTS ws_flv_port integer;
ALTER table wvp_media_server ADD COLUMN IF NOT EXISTS ws_flv_ssl_port integer;


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
ALTER TABLE wvp_device_channel drop CONSTRAINT IF EXISTS uk_wvp_device_channel_unique_device_channel;
ALTER TABLE wvp_device_channel DROP CONSTRAINT IF EXISTS uk_wvp_unique_stream_push_id;
ALTER TABLE wvp_device_channel DROP CONSTRAINT IF EXISTS uk_wvp_unique_stream_proxy_id;

ALTER TABLE wvp_device_channel ADD COLUMN IF NOT EXISTS data_type integer not null;
ALTER TABLE wvp_device_channel ADD COLUMN IF NOT EXISTS data_device_id integer not null;

DO $$
    BEGIN
        IF EXISTS (SELECT column_name FROM information_schema.columns
                       WHERE TABLE_SCHEMA = (SELECT current_schema()) and  table_name = 'wvp_device_channel' and column_name = 'device_db_id')
        THEN
            update wvp_device_channel wdc set data_type = 1, data_device_id =
                (SELECT device_db_id from wvp_device_channel where device_db_id is not null and id = wdc.id )
            where device_db_id is not null;
        END IF;

        IF EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT current_schema()) and  table_name = 'wvp_device_channel' and column_name = 'stream_push_id')
        THEN
            update wvp_device_channel wdc set data_type = 2, data_device_id =
                (SELECT stream_push_id from wvp_device_channel where stream_push_id is not null and id = wdc.id )
                                          where stream_push_id is not null;
        END IF;

        IF EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT current_schema()) and  table_name = 'wvp_device_channel' and column_name = 'stream_proxy_id')
        THEN
            update wvp_device_channel wdc set data_type = 3, data_device_id = (SELECT stream_proxy_id from wvp_device_channel where stream_proxy_id is not null and id = wdc.id )
                                          where stream_proxy_id is not null;
        END IF;
    END $$;


alter table wvp_device_channel drop column IF EXISTS device_db_id;
alter table wvp_device_channel drop column IF EXISTS  stream_push_id;
alter table wvp_device_channel drop column IF EXISTS  stream_proxy_id;

/*
* 20241231
*/
alter table wvp_stream_proxy add column IF NOT EXISTS relates_media_server_id character varying(50);

/*
* 20250111
*/
ALTER TABLE wvp_cloud_record DROP CONSTRAINT IF EXISTS uk_stream_push_app_stream_path;
alter table wvp_cloud_record alter folder  type varchar(500);
alter table wvp_cloud_record alter file_path type varchar(500);

/*
* 20250211
*/
alter table wvp_device rename keepalive_interval_time to heart_beat_interval;
alter table wvp_device add column if not exists heart_beat_count integer;
alter table wvp_device add column if not exists position_capability integer;

/**
  * 20250312
 */
alter table wvp_device add column if not exists server_id character varying(50);
alter table wvp_media_server add column if not exists server_id character varying(50);
alter table wvp_stream_proxy add column if not exists server_id character varying(50);
alter table wvp_cloud_record add column if not exists server_id character varying(50);
alter table wvp_platform add column if not exists server_id character varying(50);

update wvp_device set server_id = '你的服务ID';
update wvp_media_server set server_id = '你的服务ID';
update wvp_stream_proxy set server_id = '你的服务ID';
update wvp_cloud_record set server_id = '你的服务ID';

/*
* 20250319
*/
alter table wvp_device_channel add column if not exists gps_speed double precision;
alter table wvp_device_channel add column if not exists gps_altitude double precision;
alter table wvp_device_channel add column if not exists gps_direction double precision;

/*
* 20250319
*/
update wvp_record_plan_item set start = start * 30, stop = (stop + 1) * 30
/*
* 20250402
*/
create index if not exists data_type on wvp_device_channel (data_type);
create index if not exists data_device_id on wvp_device_channel (data_device_id);

/**
* 20250414
*/
alter table wvp_cloud_record modify time_len double precision;

