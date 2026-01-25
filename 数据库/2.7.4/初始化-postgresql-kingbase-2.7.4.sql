/*建表*/
drop table IF EXISTS wvp_device;
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
COMMENT ON TABLE wvp_device IS '存储国标设备的基础信息及在线状态';
COMMENT ON COLUMN wvp_device.id IS '主键ID';
COMMENT ON COLUMN wvp_device.device_id IS '国标设备编号';
COMMENT ON COLUMN wvp_device.name IS '设备名称';
COMMENT ON COLUMN wvp_device.manufacturer IS '设备厂商';
COMMENT ON COLUMN wvp_device.model IS '设备型号';
COMMENT ON COLUMN wvp_device.firmware IS '固件版本号';
COMMENT ON COLUMN wvp_device.transport IS '信令传输协议（TCP/UDP）';
COMMENT ON COLUMN wvp_device.stream_mode IS '拉流方式（主动/被动）';
COMMENT ON COLUMN wvp_device.on_line IS '在线状态';
COMMENT ON COLUMN wvp_device.ip IS '设备IP地址';
COMMENT ON COLUMN wvp_device.create_time IS '创建时间';
COMMENT ON COLUMN wvp_device.update_time IS '更新时间';
COMMENT ON COLUMN wvp_device.port IS '信令端口';
COMMENT ON COLUMN wvp_device.expires IS '注册有效期';
COMMENT ON COLUMN wvp_device.subscribe_cycle_for_catalog IS '目录订阅周期';
COMMENT ON COLUMN wvp_device.subscribe_cycle_for_mobile_position IS '移动位置订阅周期';
COMMENT ON COLUMN wvp_device.mobile_position_submission_interval IS '移动位置上报间隔';
COMMENT ON COLUMN wvp_device.subscribe_cycle_for_alarm IS '报警订阅周期';
COMMENT ON COLUMN wvp_device.host_address IS '设备域名/主机地址';
COMMENT ON COLUMN wvp_device.charset IS '信令字符集';
COMMENT ON COLUMN wvp_device.ssrc_check IS '是否校验SSRC';
COMMENT ON COLUMN wvp_device.geo_coord_sys IS '坐标系类型';
COMMENT ON COLUMN wvp_device.media_server_id IS '绑定的流媒体服务ID';
COMMENT ON COLUMN wvp_device.custom_name IS '自定义显示名称';
COMMENT ON COLUMN wvp_device.sdp_ip IS 'SDP中携带的IP';
COMMENT ON COLUMN wvp_device.local_ip IS '本地局域网IP';
COMMENT ON COLUMN wvp_device.password IS '设备鉴权密码';
COMMENT ON COLUMN wvp_device.as_message_channel IS '是否作为消息通道';
COMMENT ON COLUMN wvp_device.heart_beat_interval IS '心跳间隔';
COMMENT ON COLUMN wvp_device.heart_beat_count IS '心跳失败次数';
COMMENT ON COLUMN wvp_device.position_capability IS '定位能力标识';
COMMENT ON COLUMN wvp_device.broadcast_push_after_ack IS 'ACK后是否自动推流';
COMMENT ON COLUMN wvp_device.server_id IS '所属信令服务器ID';


drop table IF EXISTS wvp_device_alarm;
create table IF NOT EXISTS wvp_device_alarm
(
    id                serial primary key,
    device_id         character varying(50) not null,
    channel_id        character varying(50) not null,
    alarm_priority    character varying(50),
    alarm_method      character varying(50),
    alarm_time        character varying(50),
    alarm_description character varying(255),
    longitude         double precision,
    latitude          double precision,
    alarm_type        character varying(50),
    create_time       character varying(50) not null
);
COMMENT ON TABLE wvp_device_alarm IS '记录各设备上报的报警信息';
COMMENT ON COLUMN wvp_device_alarm.id IS '主键ID';
COMMENT ON COLUMN wvp_device_alarm.device_id IS '国标设备ID';
COMMENT ON COLUMN wvp_device_alarm.channel_id IS '报警关联的通道ID';
COMMENT ON COLUMN wvp_device_alarm.alarm_priority IS '报警级别';
COMMENT ON COLUMN wvp_device_alarm.alarm_method IS '报警方式（视频/语音等）';
COMMENT ON COLUMN wvp_device_alarm.alarm_time IS '报警发生时间';
COMMENT ON COLUMN wvp_device_alarm.alarm_description IS '报警描述';
COMMENT ON COLUMN wvp_device_alarm.longitude IS '报警经度';
COMMENT ON COLUMN wvp_device_alarm.latitude IS '报警纬度';
COMMENT ON COLUMN wvp_device_alarm.alarm_type IS '报警类型';
COMMENT ON COLUMN wvp_device_alarm.create_time IS '数据入库时间';


drop table IF EXISTS wvp_device_mobile_position;
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
COMMENT ON TABLE wvp_device_mobile_position IS '存储移动位置订阅上报的数据';
COMMENT ON COLUMN wvp_device_mobile_position.id IS '主键ID';
COMMENT ON COLUMN wvp_device_mobile_position.device_id IS '设备ID';
COMMENT ON COLUMN wvp_device_mobile_position.channel_id IS '通道ID';
COMMENT ON COLUMN wvp_device_mobile_position.device_name IS '设备名称';
COMMENT ON COLUMN wvp_device_mobile_position.time IS '上报时间';
COMMENT ON COLUMN wvp_device_mobile_position.longitude IS '经度';
COMMENT ON COLUMN wvp_device_mobile_position.latitude IS '纬度';
COMMENT ON COLUMN wvp_device_mobile_position.altitude IS '海拔';
COMMENT ON COLUMN wvp_device_mobile_position.speed IS '速度';
COMMENT ON COLUMN wvp_device_mobile_position.direction IS '方向角';
COMMENT ON COLUMN wvp_device_mobile_position.report_source IS '上报来源';
COMMENT ON COLUMN wvp_device_mobile_position.create_time IS '入库时间';


drop table IF EXISTS wvp_device_channel;
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
    channel_type                 int  default 0  not null,
    map_level                    int  default 0,
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
    enable_broadcast             integer default 0,
    constraint uk_wvp_unique_channel unique (gb_device_id)
);
COMMENT ON TABLE wvp_device_channel IS '保存设备下的通道信息以及扩展属性';
COMMENT ON COLUMN wvp_device_channel.id IS '主键ID';
COMMENT ON COLUMN wvp_device_channel.device_id IS '所属设备ID';
COMMENT ON COLUMN wvp_device_channel.name IS '通道名称';
COMMENT ON COLUMN wvp_device_channel.manufacturer IS '厂商';
COMMENT ON COLUMN wvp_device_channel.model IS '型号';
COMMENT ON COLUMN wvp_device_channel.owner IS '归属单位';
COMMENT ON COLUMN wvp_device_channel.civil_code IS '行政区划代码';
COMMENT ON COLUMN wvp_device_channel.block IS '区域/小区编号';
COMMENT ON COLUMN wvp_device_channel.address IS '安装地址';
COMMENT ON COLUMN wvp_device_channel.parental IS '是否有子节点';
COMMENT ON COLUMN wvp_device_channel.parent_id IS '父级通道ID';
COMMENT ON COLUMN wvp_device_channel.safety_way IS '安全防范等级';
COMMENT ON COLUMN wvp_device_channel.register_way IS '注册方式';
COMMENT ON COLUMN wvp_device_channel.cert_num IS '证书编号';
COMMENT ON COLUMN wvp_device_channel.certifiable IS '是否可认证';
COMMENT ON COLUMN wvp_device_channel.err_code IS '故障状态码';
COMMENT ON COLUMN wvp_device_channel.end_time IS '服务截止时间';
COMMENT ON COLUMN wvp_device_channel.secrecy IS '保密级别';
COMMENT ON COLUMN wvp_device_channel.ip_address IS '设备IP地址';
COMMENT ON COLUMN wvp_device_channel.port IS '设备端口';
COMMENT ON COLUMN wvp_device_channel.password IS '访问密码';
COMMENT ON COLUMN wvp_device_channel.status IS '在线状态';
COMMENT ON COLUMN wvp_device_channel.longitude IS '经度';
COMMENT ON COLUMN wvp_device_channel.latitude IS '纬度';
COMMENT ON COLUMN wvp_device_channel.ptz_type IS '云台类型';
COMMENT ON COLUMN wvp_device_channel.position_type IS '点位类型';
COMMENT ON COLUMN wvp_device_channel.room_type IS '房间类型';
COMMENT ON COLUMN wvp_device_channel.use_type IS '使用性质';
COMMENT ON COLUMN wvp_device_channel.supply_light_type IS '补光方式';
COMMENT ON COLUMN wvp_device_channel.direction_type IS '朝向';
COMMENT ON COLUMN wvp_device_channel.resolution IS '分辨率';
COMMENT ON COLUMN wvp_device_channel.business_group_id IS '业务分组ID';
COMMENT ON COLUMN wvp_device_channel.download_speed IS '下载/码流速率';
COMMENT ON COLUMN wvp_device_channel.svc_space_support_mod IS '空域SVC能力';
COMMENT ON COLUMN wvp_device_channel.svc_time_support_mode IS '时域SVC能力';
COMMENT ON COLUMN wvp_device_channel.create_time IS '创建时间';
COMMENT ON COLUMN wvp_device_channel.update_time IS '更新时间';
COMMENT ON COLUMN wvp_device_channel.sub_count IS '子节点数量';
COMMENT ON COLUMN wvp_device_channel.stream_id IS '绑定的流ID';
COMMENT ON COLUMN wvp_device_channel.has_audio IS '是否有音频';
COMMENT ON COLUMN wvp_device_channel.gps_time IS 'GPS定位时间';
COMMENT ON COLUMN wvp_device_channel.stream_identification IS '流标识';
COMMENT ON COLUMN wvp_device_channel.channel_type IS '通道类型';
COMMENT ON COLUMN wvp_device_channel.map_level IS '地图层级';
COMMENT ON COLUMN wvp_device_channel.gb_device_id IS 'GB内的设备ID';
COMMENT ON COLUMN wvp_device_channel.gb_name IS 'GB上报的名称';
COMMENT ON COLUMN wvp_device_channel.gb_manufacturer IS 'GB厂商';
COMMENT ON COLUMN wvp_device_channel.gb_model IS 'GB型号';
COMMENT ON COLUMN wvp_device_channel.gb_owner IS 'GB归属';
COMMENT ON COLUMN wvp_device_channel.gb_civil_code IS 'GB行政区划';
COMMENT ON COLUMN wvp_device_channel.gb_block IS 'GB区域';
COMMENT ON COLUMN wvp_device_channel.gb_address IS 'GB地址';
COMMENT ON COLUMN wvp_device_channel.gb_parental IS 'GB子节点标识';
COMMENT ON COLUMN wvp_device_channel.gb_parent_id IS 'GB父通道';
COMMENT ON COLUMN wvp_device_channel.gb_safety_way IS 'GB安全防范';
COMMENT ON COLUMN wvp_device_channel.gb_register_way IS 'GB注册方式';
COMMENT ON COLUMN wvp_device_channel.gb_cert_num IS 'GB证书编号';
COMMENT ON COLUMN wvp_device_channel.gb_certifiable IS 'GB认证标志';
COMMENT ON COLUMN wvp_device_channel.gb_err_code IS 'GB错误码';
COMMENT ON COLUMN wvp_device_channel.gb_end_time IS 'GB截止时间';
COMMENT ON COLUMN wvp_device_channel.gb_secrecy IS 'GB保密级别';
COMMENT ON COLUMN wvp_device_channel.gb_ip_address IS 'GB IP';
COMMENT ON COLUMN wvp_device_channel.gb_port IS 'GB端口';
COMMENT ON COLUMN wvp_device_channel.gb_password IS 'GB接入密码';
COMMENT ON COLUMN wvp_device_channel.gb_status IS 'GB状态';
COMMENT ON COLUMN wvp_device_channel.gb_longitude IS 'GB经度';
COMMENT ON COLUMN wvp_device_channel.gb_latitude IS 'GB纬度';
COMMENT ON COLUMN wvp_device_channel.gb_business_group_id IS 'GB业务分组';
COMMENT ON COLUMN wvp_device_channel.gb_ptz_type IS 'GB云台类型';
COMMENT ON COLUMN wvp_device_channel.gb_position_type IS 'GB点位类型';
COMMENT ON COLUMN wvp_device_channel.gb_room_type IS 'GB房间类型';
COMMENT ON COLUMN wvp_device_channel.gb_use_type IS 'GB用途';
COMMENT ON COLUMN wvp_device_channel.gb_supply_light_type IS 'GB补光';
COMMENT ON COLUMN wvp_device_channel.gb_direction_type IS 'GB朝向';
COMMENT ON COLUMN wvp_device_channel.gb_resolution IS 'GB分辨率';
COMMENT ON COLUMN wvp_device_channel.gb_download_speed IS 'GB码流速率';
COMMENT ON COLUMN wvp_device_channel.gb_svc_space_support_mod IS 'GB空域SVC';
COMMENT ON COLUMN wvp_device_channel.gb_svc_time_support_mode IS 'GB时域SVC';
COMMENT ON COLUMN wvp_device_channel.record_plan_id IS '绑定的录像计划ID';
COMMENT ON COLUMN wvp_device_channel.data_type IS '数据类型标识';
COMMENT ON COLUMN wvp_device_channel.data_device_id IS '数据来源设备主键';
COMMENT ON COLUMN wvp_device_channel.gps_speed IS 'GPS速度';
COMMENT ON COLUMN wvp_device_channel.gps_altitude IS 'GPS海拔';
COMMENT ON COLUMN wvp_device_channel.gps_direction IS 'GPS方向';
COMMENT ON COLUMN wvp_device_channel.enable_broadcast IS '是否支持广播';


CREATE INDEX idx_data_type ON wvp_device_channel (data_type);
CREATE INDEX idx_data_device_id ON wvp_device_channel (data_device_id);

drop table IF EXISTS wvp_media_server;
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
    mp4_port            integer,
    mp4_ssl_port        integer,
    ws_flv_port         integer,
    ws_flv_ssl_port     integer,
    jtt_proxy_port      integer,
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
);
COMMENT ON TABLE wvp_media_server IS '媒体服务器（如 ZLM）节点信息';
COMMENT ON COLUMN wvp_media_server.id IS '媒体服务器ID';
COMMENT ON COLUMN wvp_media_server.ip IS '服务器IP';
COMMENT ON COLUMN wvp_media_server.hook_ip IS 'hook回调IP';
COMMENT ON COLUMN wvp_media_server.sdp_ip IS 'SDP中使用的IP';
COMMENT ON COLUMN wvp_media_server.stream_ip IS '推流使用的IP';
COMMENT ON COLUMN wvp_media_server.http_port IS 'HTTP端口';
COMMENT ON COLUMN wvp_media_server.http_ssl_port IS 'HTTPS端口';
COMMENT ON COLUMN wvp_media_server.rtmp_port IS 'RTMP端口';
COMMENT ON COLUMN wvp_media_server.rtmp_ssl_port IS 'RTMPS端口';
COMMENT ON COLUMN wvp_media_server.rtp_proxy_port IS 'RTP代理端口';
COMMENT ON COLUMN wvp_media_server.rtsp_port IS 'RTSP端口';
COMMENT ON COLUMN wvp_media_server.rtsp_ssl_port IS 'RTSPS端口';
COMMENT ON COLUMN wvp_media_server.flv_port IS 'FLV端口';
COMMENT ON COLUMN wvp_media_server.flv_ssl_port IS 'FLV HTTPS端口';
COMMENT ON COLUMN wvp_media_server.mp4_port IS 'MP4点播端口';
COMMENT ON COLUMN wvp_media_server.mp4_ssl_port IS 'MP4 HTTPS端口';
COMMENT ON COLUMN wvp_media_server.ws_flv_port IS 'WS-FLV端口';
COMMENT ON COLUMN wvp_media_server.ws_flv_ssl_port IS 'WS-FLV HTTPS端口';
COMMENT ON COLUMN wvp_media_server.jtt_proxy_port IS 'JT/T代理端口';
COMMENT ON COLUMN wvp_media_server.auto_config IS '是否自动配置';
COMMENT ON COLUMN wvp_media_server.secret IS 'ZLM校验密钥';
COMMENT ON COLUMN wvp_media_server.type IS '节点类型';
COMMENT ON COLUMN wvp_media_server.rtp_enable IS '是否开启RTP';
COMMENT ON COLUMN wvp_media_server.rtp_port_range IS 'RTP端口范围';
COMMENT ON COLUMN wvp_media_server.send_rtp_port_range IS '发送RTP端口范围';
COMMENT ON COLUMN wvp_media_server.record_assist_port IS '录像辅助端口';
COMMENT ON COLUMN wvp_media_server.default_server IS '是否默认节点';
COMMENT ON COLUMN wvp_media_server.create_time IS '创建时间';
COMMENT ON COLUMN wvp_media_server.update_time IS '更新时间';
COMMENT ON COLUMN wvp_media_server.hook_alive_interval IS 'hook心跳间隔';
COMMENT ON COLUMN wvp_media_server.record_path IS '录像目录';
COMMENT ON COLUMN wvp_media_server.record_day IS '录像保留天数';
COMMENT ON COLUMN wvp_media_server.transcode_suffix IS '转码指令后缀';
COMMENT ON COLUMN wvp_media_server.server_id IS '对应信令服务器ID';


drop table IF EXISTS wvp_platform;
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
COMMENT ON TABLE wvp_platform IS '上级国标平台注册信息';
COMMENT ON COLUMN wvp_platform.id IS '主键ID';
COMMENT ON COLUMN wvp_platform.enable IS '是否启用该平台注册';
COMMENT ON COLUMN wvp_platform.name IS '平台名称';
COMMENT ON COLUMN wvp_platform.server_gb_id IS '上级平台国标编码';
COMMENT ON COLUMN wvp_platform.server_gb_domain IS '上级平台域编码';
COMMENT ON COLUMN wvp_platform.server_ip IS '上级平台IP';
COMMENT ON COLUMN wvp_platform.server_port IS '上级平台注册端口';
COMMENT ON COLUMN wvp_platform.device_gb_id IS '本平台向上注册的国标编码';
COMMENT ON COLUMN wvp_platform.device_ip IS '本平台信令IP';
COMMENT ON COLUMN wvp_platform.device_port IS '本平台信令端口';
COMMENT ON COLUMN wvp_platform.username IS '注册用户名';
COMMENT ON COLUMN wvp_platform.password IS '注册密码';
COMMENT ON COLUMN wvp_platform.expires IS '注册有效期';
COMMENT ON COLUMN wvp_platform.keep_timeout IS '心跳超时时间';
COMMENT ON COLUMN wvp_platform.transport IS '传输协议（UDP/TCP）';
COMMENT ON COLUMN wvp_platform.civil_code IS '行政区划代码';
COMMENT ON COLUMN wvp_platform.manufacturer IS '厂商';
COMMENT ON COLUMN wvp_platform.model IS '型号';
COMMENT ON COLUMN wvp_platform.address IS '地址';
COMMENT ON COLUMN wvp_platform.character_set IS '字符集';
COMMENT ON COLUMN wvp_platform.ptz IS '是否支持PTZ';
COMMENT ON COLUMN wvp_platform.rtcp IS '是否开启RTCP';
COMMENT ON COLUMN wvp_platform.status IS '注册状态';
COMMENT ON COLUMN wvp_platform.catalog_group IS '目录分组方式';
COMMENT ON COLUMN wvp_platform.register_way IS '注册方式';
COMMENT ON COLUMN wvp_platform.secrecy IS '保密级别';
COMMENT ON COLUMN wvp_platform.create_time IS '创建时间';
COMMENT ON COLUMN wvp_platform.update_time IS '更新时间';
COMMENT ON COLUMN wvp_platform.as_message_channel IS '是否作为消息通道';
COMMENT ON COLUMN wvp_platform.catalog_with_platform IS '是否推送平台目录';
COMMENT ON COLUMN wvp_platform.catalog_with_group IS '是否推送分组目录';
COMMENT ON COLUMN wvp_platform.catalog_with_region IS '是否推送区域目录';
COMMENT ON COLUMN wvp_platform.auto_push_channel IS '是否自动推送通道';
COMMENT ON COLUMN wvp_platform.send_stream_ip IS '推流时使用的IP';
COMMENT ON COLUMN wvp_platform.server_id IS '对应信令服务器ID';


drop table IF EXISTS wvp_platform_channel;
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
COMMENT ON TABLE wvp_platform_channel IS '国标平台下发的通道映射关系';
COMMENT ON COLUMN wvp_platform_channel.id IS '主键ID';
COMMENT ON COLUMN wvp_platform_channel.platform_id IS '平台ID';
COMMENT ON COLUMN wvp_platform_channel.device_channel_id IS '本地通道表主键';
COMMENT ON COLUMN wvp_platform_channel.custom_device_id IS '自定义国标编码';
COMMENT ON COLUMN wvp_platform_channel.custom_name IS '自定义名称';
COMMENT ON COLUMN wvp_platform_channel.custom_manufacturer IS '自定义厂商';
COMMENT ON COLUMN wvp_platform_channel.custom_model IS '自定义型号';
COMMENT ON COLUMN wvp_platform_channel.custom_owner IS '自定义归属';
COMMENT ON COLUMN wvp_platform_channel.custom_civil_code IS '自定义行政区划';
COMMENT ON COLUMN wvp_platform_channel.custom_block IS '自定义区域';
COMMENT ON COLUMN wvp_platform_channel.custom_address IS '自定义地址';
COMMENT ON COLUMN wvp_platform_channel.custom_parental IS '自定义父/子标识';
COMMENT ON COLUMN wvp_platform_channel.custom_parent_id IS '自定义父节点';
COMMENT ON COLUMN wvp_platform_channel.custom_safety_way IS '自定义安全防范';
COMMENT ON COLUMN wvp_platform_channel.custom_register_way IS '自定义注册方式';
COMMENT ON COLUMN wvp_platform_channel.custom_cert_num IS '自定义证书编号';
COMMENT ON COLUMN wvp_platform_channel.custom_certifiable IS '自定义可认证标志';
COMMENT ON COLUMN wvp_platform_channel.custom_err_code IS '自定义错误码';
COMMENT ON COLUMN wvp_platform_channel.custom_end_time IS '自定义截止时间';
COMMENT ON COLUMN wvp_platform_channel.custom_secrecy IS '自定义保密级别';
COMMENT ON COLUMN wvp_platform_channel.custom_ip_address IS '自定义IP';
COMMENT ON COLUMN wvp_platform_channel.custom_port IS '自定义端口';
COMMENT ON COLUMN wvp_platform_channel.custom_password IS '自定义密码';
COMMENT ON COLUMN wvp_platform_channel.custom_status IS '自定义状态';
COMMENT ON COLUMN wvp_platform_channel.custom_longitude IS '自定义经度';
COMMENT ON COLUMN wvp_platform_channel.custom_latitude IS '自定义纬度';
COMMENT ON COLUMN wvp_platform_channel.custom_ptz_type IS '自定义云台类型';
COMMENT ON COLUMN wvp_platform_channel.custom_position_type IS '自定义点位类型';
COMMENT ON COLUMN wvp_platform_channel.custom_room_type IS '自定义房间类型';
COMMENT ON COLUMN wvp_platform_channel.custom_use_type IS '自定义用途';
COMMENT ON COLUMN wvp_platform_channel.custom_supply_light_type IS '自定义补光';
COMMENT ON COLUMN wvp_platform_channel.custom_direction_type IS '自定义朝向';
COMMENT ON COLUMN wvp_platform_channel.custom_resolution IS '自定义分辨率';
COMMENT ON COLUMN wvp_platform_channel.custom_business_group_id IS '自定义业务分组';
COMMENT ON COLUMN wvp_platform_channel.custom_download_speed IS '自定义码流速率';
COMMENT ON COLUMN wvp_platform_channel.custom_svc_space_support_mod IS '自定义空域SVC';
COMMENT ON COLUMN wvp_platform_channel.custom_svc_time_support_mode IS '自定义时域SVC';


drop table IF EXISTS wvp_platform_group;
create table IF NOT EXISTS wvp_platform_group
(
    id          serial primary key,
    platform_id integer,
    group_id    integer,
    constraint uk_wvp_platform_group_platform_id_group_id unique (platform_id, group_id)
);
COMMENT ON TABLE wvp_platform_group IS '平台与分组（行政区划/组织）关系';
COMMENT ON COLUMN wvp_platform_group.id IS '主键ID';
COMMENT ON COLUMN wvp_platform_group.platform_id IS '平台ID';
COMMENT ON COLUMN wvp_platform_group.group_id IS '分组ID';


drop table IF EXISTS wvp_platform_region;
create table IF NOT EXISTS wvp_platform_region
(
    id          serial primary key,
    platform_id integer,
    region_id   integer,
    constraint uk_wvp_platform_region_platform_id_group_id unique (platform_id, region_id)
);
COMMENT ON TABLE wvp_platform_region IS '平台与区域关系';
COMMENT ON COLUMN wvp_platform_region.id IS '主键ID';
COMMENT ON COLUMN wvp_platform_region.platform_id IS '平台ID';
COMMENT ON COLUMN wvp_platform_region.region_id IS '区域ID';


drop table IF EXISTS wvp_stream_proxy;
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
    create_time                character varying(50),
    name                       character varying(255),
    update_time                character varying(50),
    stream_key                 character varying(255),
    server_id                  character varying(50),
    enable_disable_none_reader bool default false,
    relates_media_server_id    character varying(50),
    constraint uk_stream_proxy_app_stream unique (app, stream)
);
COMMENT ON TABLE wvp_stream_proxy IS '拉流代理/转推配置';
COMMENT ON COLUMN wvp_stream_proxy.id IS '主键ID';
COMMENT ON COLUMN wvp_stream_proxy.type IS '代理类型（拉流/推流）';
COMMENT ON COLUMN wvp_stream_proxy.app IS '应用名';
COMMENT ON COLUMN wvp_stream_proxy.stream IS '流ID';
COMMENT ON COLUMN wvp_stream_proxy.src_url IS '源地址';
COMMENT ON COLUMN wvp_stream_proxy.timeout IS '拉流超时时间';
COMMENT ON COLUMN wvp_stream_proxy.ffmpeg_cmd_key IS 'FFmpeg命令模板键';
COMMENT ON COLUMN wvp_stream_proxy.rtsp_type IS 'RTSP拉流方式';
COMMENT ON COLUMN wvp_stream_proxy.media_server_id IS '指定媒体服务器ID';
COMMENT ON COLUMN wvp_stream_proxy.enable_audio IS '是否启用音频';
COMMENT ON COLUMN wvp_stream_proxy.enable_mp4 IS '是否录制MP4';
COMMENT ON COLUMN wvp_stream_proxy.pulling IS '当前是否在拉流';
COMMENT ON COLUMN wvp_stream_proxy.enable IS '是否启用该代理';
COMMENT ON COLUMN wvp_stream_proxy.create_time IS '创建时间';
COMMENT ON COLUMN wvp_stream_proxy.name IS '代理名称';
COMMENT ON COLUMN wvp_stream_proxy.update_time IS '更新时间';
COMMENT ON COLUMN wvp_stream_proxy.stream_key IS '唯一流标识';
COMMENT ON COLUMN wvp_stream_proxy.server_id IS '信令服务器ID';
COMMENT ON COLUMN wvp_stream_proxy.enable_disable_none_reader IS '是否无人观看时自动停流';
COMMENT ON COLUMN wvp_stream_proxy.relates_media_server_id IS '关联的媒体服务器ID';


drop table IF EXISTS wvp_stream_push;
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
COMMENT ON TABLE wvp_stream_push IS '推流会话记录';
COMMENT ON COLUMN wvp_stream_push.id IS '主键ID';
COMMENT ON COLUMN wvp_stream_push.app IS '应用名';
COMMENT ON COLUMN wvp_stream_push.stream IS '流ID';
COMMENT ON COLUMN wvp_stream_push.create_time IS '创建时间';
COMMENT ON COLUMN wvp_stream_push.media_server_id IS '推流所在媒体服务器';
COMMENT ON COLUMN wvp_stream_push.server_id IS '信令服务器ID';
COMMENT ON COLUMN wvp_stream_push.push_time IS '推流开始时间';
COMMENT ON COLUMN wvp_stream_push.status IS '推流状态';
COMMENT ON COLUMN wvp_stream_push.update_time IS '更新时间';
COMMENT ON COLUMN wvp_stream_push.pushing IS '是否正在推流';
COMMENT ON COLUMN wvp_stream_push.self IS '是否本地发起';
COMMENT ON COLUMN wvp_stream_push.start_offline_push IS '是否离线后自动重推';


drop table IF EXISTS wvp_cloud_record;
create table IF NOT EXISTS wvp_cloud_record
(
    id              serial primary key,
    app             character varying(255),
    stream          character varying(255),
    call_id         character varying(255),
    start_time      int8,
    end_time        int8,
    media_server_id character varying(50),
    server_id       character varying(50),
    file_name       character varying(255),
    folder          character varying(500),
    file_path       character varying(500),
    collect         bool default false,
    file_size       int8,
    time_len        double precision
);
COMMENT ON TABLE wvp_cloud_record IS '云端录像记录';
COMMENT ON COLUMN wvp_cloud_record.id IS '主键ID';
COMMENT ON COLUMN wvp_cloud_record.app IS '应用名';
COMMENT ON COLUMN wvp_cloud_record.stream IS '流ID';
COMMENT ON COLUMN wvp_cloud_record.call_id IS '会话ID';
COMMENT ON COLUMN wvp_cloud_record.start_time IS '录像开始时间';
COMMENT ON COLUMN wvp_cloud_record.end_time IS '录像结束时间';
COMMENT ON COLUMN wvp_cloud_record.media_server_id IS '媒体服务器ID';
COMMENT ON COLUMN wvp_cloud_record.server_id IS '信令服务器ID';
COMMENT ON COLUMN wvp_cloud_record.file_name IS '文件名';
COMMENT ON COLUMN wvp_cloud_record.folder IS '目录';
COMMENT ON COLUMN wvp_cloud_record.file_path IS '完整路径';
COMMENT ON COLUMN wvp_cloud_record.collect IS '是否收藏';
COMMENT ON COLUMN wvp_cloud_record.file_size IS '文件大小';
COMMENT ON COLUMN wvp_cloud_record.time_len IS '时长';


drop table IF EXISTS wvp_user;
create table IF NOT EXISTS wvp_user
(
    id          serial primary key,
    username    character varying(255),
    password    character varying(255),
    role_id     integer,
    create_time character varying(50),
    update_time character varying(50),
    push_key    character varying(50),
    constraint uk_user_username unique (username)
);
COMMENT ON TABLE wvp_user IS '平台用户信息';
COMMENT ON COLUMN wvp_user.id IS '主键ID';
COMMENT ON COLUMN wvp_user.username IS '用户名';
COMMENT ON COLUMN wvp_user.password IS '密码（MD5）';
COMMENT ON COLUMN wvp_user.role_id IS '角色ID';
COMMENT ON COLUMN wvp_user.create_time IS '创建时间';
COMMENT ON COLUMN wvp_user.update_time IS '更新时间';
COMMENT ON COLUMN wvp_user.push_key IS '推送密钥';


drop table IF EXISTS wvp_user_role;
create table IF NOT EXISTS wvp_user_role
(
    id          serial primary key,
    name        character varying(50),
    authority   character varying(50),
    create_time character varying(50),
    update_time character varying(50)
);
COMMENT ON TABLE wvp_user_role IS '用户角色信息';
COMMENT ON COLUMN wvp_user_role.id IS '主键ID';
COMMENT ON COLUMN wvp_user_role.name IS '角色名称';
COMMENT ON COLUMN wvp_user_role.authority IS '权限标识';
COMMENT ON COLUMN wvp_user_role.create_time IS '创建时间';
COMMENT ON COLUMN wvp_user_role.update_time IS '更新时间';



drop table IF EXISTS wvp_user_api_key;
create table IF NOT EXISTS wvp_user_api_key
(
    id          serial primary key,
    user_id     int8,
    app         character varying(255),
    api_key     text,
    expired_at  int8,
    remark      character varying(255),
    enable      bool default true,
    create_time character varying(50),
    update_time character varying(50)
);
COMMENT ON COLUMN wvp_user_api_key.id IS '主键ID';
COMMENT ON COLUMN wvp_user_api_key.user_id IS '关联用户ID';
COMMENT ON COLUMN wvp_user_api_key.app IS '应用标识';
COMMENT ON COLUMN wvp_user_api_key.api_key IS 'API Key';
COMMENT ON COLUMN wvp_user_api_key.expired_at IS '过期时间戳';
COMMENT ON COLUMN wvp_user_api_key.remark IS '备注';
COMMENT ON COLUMN wvp_user_api_key.enable IS '是否启用';
COMMENT ON COLUMN wvp_user_api_key.create_time IS '创建时间';
COMMENT ON COLUMN wvp_user_api_key.update_time IS '更新时间';



/*初始数据*/
INSERT INTO wvp_user
VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3', 1, '2021-04-13 14:14:57', '2021-04-13 14:14:57',
        '3e80d1762a324d5b0ff636e0bd16f1e3');
INSERT INTO wvp_user_role
VALUES (1, 'admin', '0', '2021-04-13 14:14:57', '2021-04-13 14:14:57');

drop table IF EXISTS wvp_common_group;
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
    alias            varchar(255) default null,
    constraint uk_common_group_device_platform unique (device_id)
);
COMMENT ON TABLE wvp_common_group IS '通用分组表，存储行业或组织结构';
COMMENT ON COLUMN wvp_common_group.id IS '主键ID';
COMMENT ON COLUMN wvp_common_group.device_id IS '分组对应的平台或设备ID';
COMMENT ON COLUMN wvp_common_group.name IS '分组名称';
COMMENT ON COLUMN wvp_common_group.parent_id IS '父级分组ID';
COMMENT ON COLUMN wvp_common_group.parent_device_id IS '父级分组对应的设备ID';
COMMENT ON COLUMN wvp_common_group.business_group IS '业务分组编码';
COMMENT ON COLUMN wvp_common_group.create_time IS '创建时间';
COMMENT ON COLUMN wvp_common_group.update_time IS '更新时间';
COMMENT ON COLUMN wvp_common_group.civil_code IS '行政区划代码';
COMMENT ON COLUMN wvp_common_group.alias IS '别名';


drop table IF EXISTS wvp_common_region;
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
COMMENT ON TABLE wvp_common_region IS '通用行政区域表';
COMMENT ON COLUMN wvp_common_region.id IS '主键ID';
COMMENT ON COLUMN wvp_common_region.device_id IS '区域对应的平台或设备ID';
COMMENT ON COLUMN wvp_common_region.name IS '区域名称';
COMMENT ON COLUMN wvp_common_region.parent_id IS '父级区域ID';
COMMENT ON COLUMN wvp_common_region.parent_device_id IS '父级区域的设备ID';
COMMENT ON COLUMN wvp_common_region.create_time IS '创建时间';
COMMENT ON COLUMN wvp_common_region.update_time IS '更新时间';


drop table IF EXISTS wvp_record_plan;
create table IF NOT EXISTS wvp_record_plan
(
    id              serial primary key,
    snap            bool default false,
    name            varchar(255) NOT NULL,
    create_time     character varying(50),
    update_time     character varying(50)
);
COMMENT ON TABLE wvp_record_plan IS '录像计划基础信息';
COMMENT ON COLUMN wvp_record_plan.id IS '主键ID';
COMMENT ON COLUMN wvp_record_plan.snap IS '是否抓图计划';
COMMENT ON COLUMN wvp_record_plan.name IS '计划名称';
COMMENT ON COLUMN wvp_record_plan.create_time IS '创建时间';
COMMENT ON COLUMN wvp_record_plan.update_time IS '更新时间';


drop table IF EXISTS wvp_record_plan_item;
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
COMMENT ON TABLE wvp_record_plan_item IS '录像计划条目表';
COMMENT ON COLUMN wvp_record_plan_item.id IS '主键ID';
COMMENT ON COLUMN wvp_record_plan_item."start" IS '开始时间（分钟）';
COMMENT ON COLUMN wvp_record_plan_item.stop IS '结束时间（分钟）';
COMMENT ON COLUMN wvp_record_plan_item.week_day IS '星期（0-6）';
COMMENT ON COLUMN wvp_record_plan_item.plan_id IS '所属录像计划ID';
COMMENT ON COLUMN wvp_record_plan_item.create_time IS '创建时间';
COMMENT ON COLUMN wvp_record_plan_item.update_time IS '更新时间';


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
COMMENT ON TABLE wvp_jt_terminal IS '交通部 JT/T 1076 终端信息';
COMMENT ON COLUMN wvp_jt_terminal.id IS '主键ID';
COMMENT ON COLUMN wvp_jt_terminal.phone_number IS '终端SIM卡号';
COMMENT ON COLUMN wvp_jt_terminal.terminal_id IS '终端设备ID';
COMMENT ON COLUMN wvp_jt_terminal.province_id IS '所在省份ID';
COMMENT ON COLUMN wvp_jt_terminal.province_text IS '所在省份名称';
COMMENT ON COLUMN wvp_jt_terminal.city_id IS '所在城市ID';
COMMENT ON COLUMN wvp_jt_terminal.city_text IS '所在城市名称';
COMMENT ON COLUMN wvp_jt_terminal.maker_id IS '厂商ID';
COMMENT ON COLUMN wvp_jt_terminal.model IS '终端型号';
COMMENT ON COLUMN wvp_jt_terminal.plate_color IS '车牌颜色';
COMMENT ON COLUMN wvp_jt_terminal.plate_no IS '车牌号码';
COMMENT ON COLUMN wvp_jt_terminal.longitude IS '经度';
COMMENT ON COLUMN wvp_jt_terminal.latitude IS '纬度';
COMMENT ON COLUMN wvp_jt_terminal.status IS '在线状态';
COMMENT ON COLUMN wvp_jt_terminal.register_time IS '注册时间';
COMMENT ON COLUMN wvp_jt_terminal.update_time IS '更新时间';
COMMENT ON COLUMN wvp_jt_terminal.create_time IS '创建时间';
COMMENT ON COLUMN wvp_jt_terminal.geo_coord_sys IS '坐标系';
COMMENT ON COLUMN wvp_jt_terminal.media_server_id IS '媒体服务器ID';
COMMENT ON COLUMN wvp_jt_terminal.sdp_ip IS 'SDP IP';

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
COMMENT ON TABLE wvp_jt_channel IS '交通部 JT/T 1076 通道信息';
COMMENT ON COLUMN wvp_jt_channel.id IS '主键ID';
COMMENT ON COLUMN wvp_jt_channel.terminal_db_id IS '所属终端记录ID';
COMMENT ON COLUMN wvp_jt_channel.channel_id IS '通道号';
COMMENT ON COLUMN wvp_jt_channel.has_audio IS '是否有音频';
COMMENT ON COLUMN wvp_jt_channel.name IS '通道名称';
COMMENT ON COLUMN wvp_jt_channel.update_time IS '更新时间';
COMMENT ON COLUMN wvp_jt_channel.create_time IS '创建时间';
