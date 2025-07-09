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
                               gb_device_id character varying(255),
                               gb_name character varying(255),
                               gb_manufacturer character varying(255),
                               gb_model character varying(255),
                               gb_civil_code character varying(8),
                               gb_block character varying(255),
                               gb_address character varying(255),
                               gb_parental bool default false,
                               gb_parent_id character varying(255),
                               gb_register_way integer default 1,
                               gb_security_level_code character varying(255),
                               gb_secrecy integer default 0,
                               gb_ip_address character varying(255),
                               gb_port integer,
                               gb_password character varying(255),
                               gb_status bool default false,
                               gb_longitude double precision,
                               gb_latitude double precision,
                               gb_business_group_id character varying(255),
                               gb_ptz_type integer,
                               gb_photoelectric_imaging_type integer,
                               gb_capture_position_type integer,
                               gb_room_type integer,
                               gb_supply_light_type integer default 1,
                               gb_direction_type integer,
                               gb_resolution character varying(255),
                               gb_stream_number_list character varying(255),
                               gb_download_speed character varying(255),
                               gb_svc_space_support_mode integer,
                               gb_svc_time_support_mode integer,
                               gb_ssvc_ratio_support_list character varying(255),
                               gb_mobile_device_type integer,
                               gb_horizontal_field_angle double precision,
                               gb_vertical_field_angle double precision,
                               gb_max_view_distance double precision,
                               gb_grassroots_code character varying(255),
                               gb_point_type integer,
                               gb_point_common_name character varying(255),
                               gb_mac character varying(255),
                               gb_function_type character varying(255),
                               gb_encode_type character varying(255),
                               gb_install_time character varying(255),
                               gb_management_unit character varying(255),
                               gb_contact_info character varying(255),
                               gb_record_save_days character varying(255),
                               gb_industrial_classification character varying(255),
                               constraint uk_jt_device_id_device_id unique (terminal_db_id, channel_id)
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


