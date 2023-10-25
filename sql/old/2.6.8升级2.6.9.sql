alter table device
    change deviceId device_id varchar(50) not null;

alter table device
    change streamMode stream_mode varchar(50) null;

alter table device
    change registerTime register_time varchar(50) null;

alter table device
    change keepaliveTime keepalive_time varchar(50) null;

alter table device
    change createTime create_time varchar(50) not null;

alter table device
    change updateTime update_time varchar(50) not null;

alter table device
    change subscribeCycleForCatalog subscribe_cycle_for_catalog bool default false;

alter table device
    change subscribeCycleForMobilePosition subscribe_cycle_for_mobile_position bool default false;

alter table device
    change mobilePositionSubmissionInterval mobile_position_submission_interval int default 5 not null;

alter table device
    change subscribeCycleForAlarm subscribe_cycle_for_alarm bool default false;

alter table device
    change hostAddress host_address varchar(50) null;

alter table device
    change ssrcCheck ssrc_check bool default false;

alter table device
    change geoCoordSys geo_coord_sys varchar(50) not null;

alter table device
    drop column treeType;

alter table device
    change mediaServerId media_server_id varchar(50) default 'auto' null;

alter table device
    change sdpIp sdp_ip varchar(50) null;

alter table device
    change localIp local_ip varchar(50) null;

alter table device
    change asMessageChannel as_message_channel bool default false;

alter table device
    change keepaliveIntervalTime keepalive_interval_time int null;

alter table device
    change online on_line varchar(50) null;

alter table device
    add COLUMN switch_primary_sub_stream bool default false comment '开启主子码流切换的开关（0-不开启，1-开启）现在已知支持设备为 大华、TP——LINK全系设备'


alter table device_alarm
    change deviceId device_id varchar(50) not null;

alter table device_alarm
    change channelId channel_id varchar(50) not null;

alter table device_alarm
    change alarmPriority alarm_priority varchar(50) not null;

alter table device_alarm
    change alarmMethod alarm_method varchar(50) null;

alter table device_alarm
    change alarmTime alarm_time varchar(50) not null;

alter table device_alarm
    change alarmDescription alarm_description varchar(255) null;

alter table device_alarm
    change alarmType alarm_type varchar(50) null;

alter table device_alarm
    change createTime create_time varchar(50) null;

alter table device_channel
    change channelId channel_id varchar(50) not null;

alter table device_channel
    change civilCode civil_code varchar(50) null;

alter table device_channel
    change parentId parent_id varchar(50) null;

alter table device_channel
    change safetyWay safety_way int null;

alter table device_channel
    change registerWay register_way int null;

alter table device_channel
    change certNum cert_num varchar(50) null;

alter table device_channel
    change errCode err_code int null;

alter table device_channel
    change endTime end_time varchar(50) null;

alter table device_channel
    change ipAddress ip_address varchar(50) null;

alter table device_channel
    change PTZType ptz_type int null;

alter table device_channel
    change status status bool default false;

alter table device_channel
    change streamId stream_id varchar(50) null;

alter table device_channel
    change deviceId device_id varchar(50) not null;


alter table device_channel
    change hasAudio has_audio bool default false;

alter table device_channel
    change createTime create_time varchar(50) not null;

alter table device_channel
    change updateTime update_time varchar(50) not null;

alter table device_channel
    change subCount sub_count int default 0 null;

alter table device_channel
    change longitudeGcj02 longitude_gcj02 double null;

alter table device_channel
    change latitudeGcj02 latitude_gcj02 double null;

alter table device_channel
    change longitudeWgs84 longitude_wgs84 double null;

alter table device_channel
    change latitudeWgs84 latitude_wgs84 double null;

alter table device_channel
    change businessGroupId business_group_id varchar(50) null;

alter table device_channel
    change gpsTime gps_time varchar(50) null;

alter table device_mobile_position
    change deviceId device_id varchar(50) not null;

alter table device_mobile_position
    change channelId channel_id varchar(50) not null;

alter table device_mobile_position
    change deviceName device_name varchar(255) null;

alter table device_mobile_position
    change reportSource report_source varchar(50) null;

alter table device_mobile_position
    change longitudeGcj02 longitude_gcj02 double null;

alter table device_mobile_position
    change latitudeGcj02 latitude_gcj02 double null;

alter table device_mobile_position
    change longitudeWgs84 longitude_wgs84 double null;

alter table device_mobile_position
    change latitudeWgs84 latitude_wgs84 double null;

alter table device_mobile_position
    change createTime create_time varchar(50) null;

alter table gb_stream
    change gbStreamId gb_stream_id int auto_increment;

alter table gb_stream
    change gbId gb_id varchar(50) not null;

alter table gb_stream
    change streamType stream_type varchar(50) null;

alter table gb_stream
    change mediaServerId media_server_id varchar(50) null;

alter table gb_stream
    change createTime create_time varchar(50) null;

alter table log
    change createTime create_time varchar(50) not null;

alter table media_server
    change hookIp hook_ip varchar(50) not null;

alter table media_server
    add send_rtp_port_range varchar(50) not null;

alter table media_server
    add column send_rtp_port_range varchar(50) default null;

alter table media_server
    change sdpIp sdp_ip varchar(50) not null;

alter table media_server
    change streamIp stream_ip varchar(50) not null;

alter table media_server
    change httpPort http_port int not null;

alter table media_server
    change httpSSlPort http_ssl_port int not null;

alter table media_server
    change rtmpPort rtmp_port int not null;

alter table media_server
    change rtmpSSlPort rtmp_ssl_port int not null;

alter table media_server
    change rtpProxyPort rtp_proxy_port int not null;

alter table media_server
    change rtspPort rtsp_port int not null;

alter table media_server
    change rtspSSLPort rtsp_ssl_port int not null;

alter table media_server
    change autoConfig auto_config bool default true;

alter table media_server
    change rtpEnable rtp_enable bool default false;

alter table media_server
    change rtpPortRange rtp_port_range varchar(50) not null;

alter table media_server
    change recordAssistPort record_assist_port int not null;

alter table media_server
    change defaultServer default_server bool default false;

alter table media_server
    change createTime create_time varchar(50) not null;

alter table media_server
    change updateTime update_time varchar(50) not null;

alter table media_server
    change hookAliveInterval hook_alive_interval int not null;

alter table parent_platform
    change serverGBId server_gb_id varchar(50) not null;

alter table parent_platform
    change serverGBDomain server_gb_domain varchar(50) null;

alter table parent_platform
    change serverIP server_ip varchar(50) null;

alter table parent_platform
    change serverPort server_port int null;

alter table parent_platform
    change deviceGBId device_gb_id varchar(50) not null;

alter table parent_platform
    change deviceIp device_ip varchar(50) null;

alter table parent_platform
    change devicePort device_port varchar(50) null;

alter table parent_platform
    change keepTimeout keep_timeout varchar(50) null;

alter table parent_platform
    change characterSet character_set varchar(50) null;

alter table parent_platform
    change catalogId catalog_id varchar(50) not null;

alter table parent_platform
    change startOfflinePush start_offline_push bool default false;

alter table parent_platform
    change administrativeDivision administrative_division varchar(50) not null;

alter table parent_platform
    change catalogGroup catalog_group int default 1 null;

alter table parent_platform
    change createTime create_time varchar(50) null;

alter table parent_platform
    change updateTime update_time varchar(50) null;

alter table parent_platform
    drop column treeType;

alter table parent_platform
    change asMessageChannel as_message_channel bool default false;

alter table parent_platform
    change enable enable bool default false;

alter table parent_platform
    change ptz ptz bool default false;

alter table parent_platform
    change rtcp rtcp bool default false;

alter table parent_platform
    change status status bool default false;

alter table parent_platform
    change status status bool default false;

alter table platform_catalog
    change platformId platform_id varchar(50) not null;

alter table platform_catalog
    change parentId parent_id varchar(50) null;

alter table platform_catalog
    change civilCode civil_code varchar(50) null;

alter table platform_catalog
    change businessGroupId business_group_id varchar(50) null;

alter table platform_gb_channel
    change platformId platform_id varchar(50) not null;

alter table platform_gb_channel
    change catalogId catalog_id varchar(50) not null;

alter table platform_gb_channel
    change deviceChannelId device_channel_id int not null;

alter table platform_gb_stream
    change platformId platform_id varchar(50) not null;

alter table platform_gb_stream
    change catalogId catalog_id varchar(50) not null;

alter table platform_gb_stream
    change gbStreamId gb_stream_id int not null;

alter table stream_proxy
    change mediaServerId media_server_id varchar(50) null;

alter table stream_proxy
    change createTime create_time varchar(50) not null;

alter table stream_proxy
    change updateTime update_time varchar(50) null;

alter table stream_proxy
    change enable_remove_none_reader enable_remove_none_reader bool default false;

alter table stream_proxy
    change enable_disable_none_reader enable_disable_none_reader bool default false;

alter table stream_proxy
    change enable_audio enable_audio bool default false;

alter table stream_proxy
    change enable_mp4 enable_mp4 bool default false;

alter table stream_proxy
    change enable enable bool default false;

alter table stream_push
    change totalReaderCount total_reader_count varchar(50) null;

alter table stream_push
    change originType origin_type int null;

alter table stream_push
    change originTypeStr origin_type_str varchar(50) null;

alter table stream_push
    change createTime create_time varchar(50) null;

alter table stream_push
    change aliveSecond alive_second int null;

alter table stream_push
    change mediaServerId media_server_id varchar(50) null;

alter table stream_push
    change status status bool default false;

alter table stream_push
    change pushTime push_time varchar(50) null;

alter table stream_push
    change updateTime update_time varchar(50) null;

alter table stream_push
    change pushIng push_ing bool default false;

alter table stream_push
    change status status bool default false;

alter table stream_push
    change self self bool default false;

alter table stream_push
    drop column serverId;


alter table user
    change roleId role_id int not null;

alter table user
    change createTime create_time varchar(50) not null;

alter table user
    change updateTime update_time varchar(50) not null;

alter table user
    change pushKey push_key varchar(50) null;

alter table user_role
    change createTime create_time varchar(50) not null;

alter table user_role
    change updateTime update_time varchar(50) not null;

rename table device to wvp_device;
rename table device_alarm to wvp_device_alarm;
rename table device_channel to wvp_device_channel;
rename table device_mobile_position to wvp_device_mobile_position;
rename table gb_stream to wvp_gb_stream;
rename table log to wvp_log;
rename table media_server to wvp_media_server;
rename table parent_platform to wvp_platform;
rename table platform_catalog to wvp_platform_catalog;
rename table platform_gb_channel to wvp_platform_gb_channel;
rename table platform_gb_stream to wvp_platform_gb_stream;
rename table stream_proxy to wvp_stream_proxy;
rename table stream_push to wvp_stream_push;
rename table user to wvp_user;
rename table user_role to wvp_user_role;

alter table wvp_device add column broadcast_push_after_ack bool default false;
alter table wvp_device_channel add column custom_name varchar(255) null ;
alter table wvp_device_channel add column custom_longitude double null ;
alter table wvp_device_channel add column custom_latitude double null ;
alter table wvp_device_channel add column custom_ptz_type int null ;

create table wvp_resources_tree (
                                    id serial primary key ,
                                    is_catalog bool default true,
                                    device_channel_id integer ,
                                    gb_stream_id integer,
                                    name character varying(255),
                                    parentId integer,
                                    path character varying(255)
);






























