/*建表*/
create table wvp_device (
                            id serial primary key ,
                            device_id character varying(50) not null ,
                            name character varying(255),
                            manufacturer character varying(255),
                            model character varying(255),
                            firmware character varying(255),
                            transport character varying(50),
                            stream_mode character varying(50),
                            on_line bool default false,
                            register_time character varying(50),
                            keepalive_time character varying(50),
                            ip character varying(50),
                            create_time character varying(50),
                            update_time character varying(50),
                            port integer,
                            expires integer,
                            subscribe_cycle_for_catalog integer DEFAULT 0,
                            subscribe_cycle_for_mobile_position integer DEFAULT 0,
                            mobile_position_submission_interval integer DEFAULT 5,
                            subscribe_cycle_for_alarm integer DEFAULT 0,
                            host_address character varying(50),
                            charset character varying(50),
                            ssrc_check bool default false,
                            geo_coord_sys character varying(50),
                            media_server_id character varying(50),
                            custom_name character varying(255),
                            sdp_ip character varying(50),
                            local_ip character varying(50),
                            password character varying(255),
                            as_message_channel bool default false,
                            keepalive_interval_time integer,
                            broadcast_push_after_ack bool default false,
                            constraint uk_device_device unique (device_id)
);

create table wvp_device_alarm (
                                  id serial primary key ,
                                  device_id character varying(50) not null,
                                  channel_id character varying(50) not null,
                                  alarm_priority character varying(50),
                                  alarm_method character varying(50),
                                  alarm_time character varying(50),
                                  alarm_description character varying(255),
                                  longitude double precision,
                                  latitude double precision,
                                  alarm_type character varying(50),
                                  create_time character varying(50) not null
);

create table wvp_device_channel (
                                    id serial primary key ,
                                    channel_id character varying(50) not null,
                                    name character varying(255),
                                    custom_name character varying(255),
                                    manufacture character varying(50),
                                    model character varying(50),
                                    owner character varying(50),
                                    civil_code character varying(50),
                                    block character varying(50),
                                    address character varying(50),
                                    parent_id character varying(50),
                                    safety_way integer,
                                    register_way integer,
                                    cert_num character varying(50),
                                    certifiable integer,
                                    err_code integer,
                                    end_time character varying(50),
                                    secrecy character varying(50),
                                    ip_address character varying(50),
                                    port integer,
                                    password character varying(255),
                                    ptz_type integer,
                                    custom_ptz_type integer,
                                    status bool default false,
                                    longitude double precision,
                                    custom_longitude double precision,
                                    latitude double precision,
                                    custom_latitude double precision,
                                    stream_id character varying(255),
                                    device_id character varying(50) not null,
                                    parental character varying(50),
                                    has_audio bool default false,
                                    create_time character varying(50) not null,
                                    update_time character varying(50) not null,
                                    sub_count integer,
                                    longitude_gcj02 double precision,
                                    latitude_gcj02 double precision,
                                    longitude_wgs84 double precision,
                                    latitude_wgs84 double precision,
                                    business_group_id character varying(50),
                                    gps_time character varying(50),
                                    stream_identification character varying(50),
                                    constraint uk_wvp_device_channel_unique_device_channel unique (device_id, channel_id)
);

create table wvp_device_mobile_position (
                                            id serial primary key,
                                            device_id character varying(50) not null,
                                            channel_id character varying(50) not null,
                                            device_name character varying(255),
                                            time character varying(50),
                                            longitude double precision,
                                            latitude double precision,
                                            altitude double precision,
                                            speed double precision,
                                            direction double precision,
                                            report_source character varying(50),
                                            longitude_gcj02 double precision,
                                            latitude_gcj02 double precision,
                                            longitude_wgs84 double precision,
                                            latitude_wgs84 double precision,
                                            create_time character varying(50)
);

create table wvp_gb_stream (
                               gb_stream_id serial primary key,
                               app character varying(255) not null,
                               stream character varying(255) not null,
                               gb_id character varying(50) not null,
                               name character varying(255),
                               longitude double precision,
                               latitude double precision,
                               stream_type character varying(50),
                               media_server_id character varying(50),
                               create_time character varying(50),
                               constraint uk_gb_stream_unique_gb_id unique (gb_id),
                               constraint uk_gb_stream_unique_app_stream unique (app, stream)
);

create table wvp_log (
                         id serial primary key ,
                         name character varying(50),
                         type character varying(50),
                         uri character varying(200),
                         address character varying(50),
                         result character varying(50),
                         timing bigint,
                         username character varying(50),
                         create_time character varying(50)
);

create table wvp_media_server (
                                  id character varying(255) primary key ,
                                  ip character varying(50),
                                  hook_ip character varying(50),
                                  sdp_ip character varying(50),
                                  stream_ip character varying(50),
                                  http_port integer,
                                  http_ssl_port integer,
                                  rtmp_port integer,
                                  rtmp_ssl_port integer,
                                  rtp_proxy_port integer,
                                  rtsp_port integer,
                                  rtsp_ssl_port integer,
                                  flv_port integer,
                                  flv_ssl_port integer,
                                  ws_flv_port integer,
                                  ws_flv_ssl_port integer,
                                  auto_config bool default false,
                                  secret character varying(50),
                                  type character varying(50) default 'zlm',
                                  rtp_enable bool default false,
                                  rtp_port_range character varying(50),
                                  send_rtp_port_range character varying(50),
                                  record_assist_port integer,
                                  default_server bool default false,
                                  create_time character varying(50),
                                  update_time character varying(50),
                                  hook_alive_interval integer,
                                  record_path character varying(255),
                                  record_day integer default 7,
                                  transcode_suffix character varying(255),
                                  constraint uk_media_server_unique_ip_http_port unique (ip, http_port)
);

create table wvp_platform (
                              id serial primary key ,
                              enable bool default false,
                              name character varying(255),
                              server_gb_id character varying(50),
                              server_gb_domain character varying(50),
                              server_ip character varying(50),
                              server_port integer,
                              device_gb_id character varying(50),
                              device_ip character varying(50),
                              device_port character varying(50),
                              username character varying(255),
                              password character varying(50),
                              expires character varying(50),
                              keep_timeout character varying(50),
                              transport character varying(50),
                              character_set character varying(50),
                              catalog_id character varying(50),
                              ptz bool default false,
                              rtcp bool default false,
                              status bool default false,
                              start_offline_push bool default false,
                              administrative_division character varying(50),
                              catalog_group integer,
                              create_time character varying(50),
                              update_time character varying(50),
                              as_message_channel bool default false,
                              auto_push_channel bool default false,
                              send_stream_ip character varying(50),
                              constraint uk_platform_unique_server_gb_id unique (server_gb_id)
);

create table wvp_platform_catalog (
                                      id character varying(50),
                                      platform_id character varying(50),
                                      name character varying(255),
                                      parent_id character varying(50),
                                      civil_code character varying(50),
                                      business_group_id character varying(50),
                                      constraint uk_platform_catalog_id_platform_id unique (id, platform_id)
);

create table wvp_platform_gb_channel (
                                         id serial primary key ,
                                         platform_id character varying(50),
                                         catalog_id character varying(50),
                                         device_channel_id integer,
                                         constraint uk_platform_gb_channel_platform_id_catalog_id_device_channel_id unique (platform_id, catalog_id, device_channel_id)
);

create table wvp_platform_gb_stream (
                                        id serial primary key,
                                        platform_id character varying(50),
                                        catalog_id character varying(50),
                                        gb_stream_id integer,
                                        constraint uk_platform_gb_stream_platform_id_catalog_id_gb_stream_id unique (platform_id, catalog_id, gb_stream_id)
);

create table wvp_stream_proxy (
                                  id serial primary key,
                                  type character varying(50),
                                  app character varying(255),
                                  stream character varying(255),
                                  url character varying(255),
                                  src_url character varying(255),
                                  dst_url character varying(255),
                                  timeout_ms integer,
                                  ffmpeg_cmd_key character varying(255),
                                  rtp_type character varying(50),
                                  media_server_id character varying(50),
                                  enable_audio bool default false,
                                  enable_mp4 bool default false,
                                  enable bool default false,
                                  status boolean,
                                  enable_remove_none_reader bool default false,
                                  create_time character varying(50),
                                  name character varying(255),
                                  update_time character varying(50),
                                  stream_key character varying(255),
                                  enable_disable_none_reader bool default false,
                                  constraint uk_stream_proxy_app_stream unique (app, stream)
);

create table wvp_stream_push (
                                 id serial primary key,
                                 app character varying(255),
                                 stream character varying(255),
                                 total_reader_count character varying(50),
                                 origin_type integer,
                                 origin_type_str character varying(50),
                                 create_time character varying(50),
                                 alive_second integer,
                                 media_server_id character varying(50),
                                 server_id character varying(50),
                                 push_time character varying(50),
                                 status bool default false,
                                 update_time character varying(50),
                                 push_ing bool default false,
                                 self bool default false,
                                 constraint uk_stream_push_app_stream unique (app, stream)
);
create table wvp_cloud_record (
                                  id serial primary key,
                                  app character varying(255),
                                  stream character varying(255),
                                  call_id character varying(255),
                                  start_time bigint,
                                  end_time bigint,
                                  media_server_id character varying(50),
                                  file_name character varying(255),
                                  folder character varying(255),
                                  file_path character varying(255),
                                  collect bool default false,
                                  file_size bigint,
                                  time_len bigint,
                                  constraint uk_stream_push_app_stream_path unique (app, stream, file_path)
);

create table wvp_user (
                          id serial primary key,
                          username character varying(255),
                          password character varying(255),
                          role_id integer,
                          create_time character varying(50),
                          update_time character varying(50),
                          push_key character varying(50),
                          constraint uk_user_username unique (username)
);

create table wvp_user_role (
                               id serial primary key,
                               name character varying(50),
                               authority character varying(50),
                               create_time character varying(50),
                               update_time character varying(50)
);
create table wvp_resources_tree (
                                    id serial primary key ,
                                    is_catalog bool default true,
                                    device_channel_id integer ,
                                    gb_stream_id integer,
                                    name character varying(255),
                                    parentId integer,
                                    path character varying(255)
);

create table wvp_user_api_key (
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


/*初始数据*/
INSERT INTO wvp_user VALUES (1, 'admin','21232f297a57a5a743894a0e4a801fc3',1,'2021-04-13 14:14:57','2021-04-13 14:14:57','3e80d1762a324d5b0ff636e0bd16f1e3');
INSERT INTO wvp_user_role VALUES (1, 'admin','0','2021-04-13 14:14:57','2021-04-13 14:14:57');



