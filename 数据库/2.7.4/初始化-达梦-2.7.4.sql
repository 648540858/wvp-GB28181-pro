/*============================================================================
 * 达梦数据库(DM8)原生初始化脚本 2.7.4
 *---------------------------------------------------------------------------
 * 本脚本按达梦数据库原生规范编写，不兼容其他数据库。
 * 关键差异点：
 *   1) 自增主键使用达梦原生标识列  IDENTITY(1,1)
 *   2) 达梦原生不支持 DROP TABLE IF EXISTS / CREATE TABLE IF NOT EXISTS，
 *      表存在性判断通过匿名块查询系统表 SYSOBJECTS 实现
 *   3) 类型使用达梦原生类型：
 *        varchar / int / bigint / double / clob / bit
 *   4) 布尔(bit)列默认 false=>0，true=>1；插入布尔字面量使用 1
 *============================================================================*/

/*--------------------------------------------------------------------------
 *建表（每张表前通过匿名块判断表是否存在，存在则先删除）
 *--------------------------------------------------------------------------*/

-- 存储国标设备的基础信息及在线状态
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_DEVICE' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_DEVICE';
END IF;
END;
/
CREATE TABLE wvp_device
(
    id                                  int identity(1,1) primary key,
    device_id                           varchar(50) not null,
    name                                varchar(255),
    manufacturer                        varchar(255),
    model                               varchar(255),
    firmware                            varchar(255),
    transport                           varchar(50),
    stream_mode                         varchar(50),
    on_line                             bit default 0,
    ip                                  varchar(50),
    create_time                         varchar(50),
    update_time                         varchar(50),
    port                                int,
    expires                             int,
    subscribe_cycle_for_catalog         int default 0,
    subscribe_cycle_for_mobile_position int default 0,
    mobile_position_submission_interval int default 5,
    subscribe_cycle_for_alarm           int default 0,
    host_address                        varchar(50),
    charset                             varchar(50),
    ssrc_check                          bit default 0,
    geo_coord_sys                       varchar(50),
    media_server_id                     varchar(50) default 'auto',
    custom_name                         varchar(255),
    sdp_ip                              varchar(50),
    local_ip                            varchar(50),
    password                            varchar(255),
    as_message_channel                  bit default 0,
    heart_beat_interval                 int,
    heart_beat_count                    int,
    position_capability                 int,
    broadcast_push_after_ack            bit default 0,
    server_id                           varchar(50),
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

-- 记录各设备上报的报警信息
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_DEVICE_ALARM' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_DEVICE_ALARM';
END IF;
END;
/
CREATE TABLE wvp_device_alarm
(
    id                int identity(1,1) primary key,
    device_id         varchar(50) not null,
    channel_id        varchar(50) not null,
    alarm_priority    varchar(50),
    alarm_method      varchar(50),
    alarm_time        varchar(50),
    alarm_description varchar(255),
    longitude         double,
    latitude          double,
    alarm_type        varchar(50),
    create_time       varchar(50) not null
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

-- 存储移动位置订阅上报的数据
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_MOBILE_POSITION' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_MOBILE_POSITION';
END IF;
END;
/
CREATE TABLE wvp_mobile_position
(
    id              int identity(1,1) primary key,
    channel_id      varchar(50) not null,
    timestamp       bigint,
    longitude       double,
    latitude        double,
    altitude        double,
    speed           double,
    direction       double,
    create_time     varchar(50)
);
COMMENT ON TABLE wvp_mobile_position IS '存储移动位置订阅上报的数据';
COMMENT ON COLUMN wvp_mobile_position.id IS '主键ID';
COMMENT ON COLUMN wvp_mobile_position.channel_id IS '通道ID';
COMMENT ON COLUMN wvp_mobile_position.timestamp IS '上报时间';
COMMENT ON COLUMN wvp_mobile_position.longitude IS '经度';
COMMENT ON COLUMN wvp_mobile_position.latitude IS '纬度';
COMMENT ON COLUMN wvp_mobile_position.altitude IS '海拔';
COMMENT ON COLUMN wvp_mobile_position.speed IS '速度';
COMMENT ON COLUMN wvp_mobile_position.direction IS '方向角';
COMMENT ON COLUMN wvp_mobile_position.create_time IS '入库时间';

-- 保存设备下的通道信息以及扩展属性
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_DEVICE_CHANNEL' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_DEVICE_CHANNEL';
END IF;
END;
/
CREATE TABLE wvp_device_channel
(
    id                           int identity(1,1) primary key,
    device_id                    varchar(50),
    name                         varchar(255),
    manufacturer                 varchar(50),
    model                        varchar(50),
    owner                        varchar(50),
    civil_code                   varchar(50),
    block                        varchar(50),
    address                      varchar(50),
    parental                     int,
    parent_id                    varchar(50),
    safety_way                   int,
    register_way                 int,
    cert_num                     varchar(50),
    certifiable                  int,
    err_code                     int,
    end_time                     varchar(50),
    secrecy                      int,
    ip_address                   varchar(50),
    port                         int,
    password                     varchar(255),
    status                       varchar(50),
    longitude                    double,
    latitude                     double,
    ptz_type                     int,
    position_type                int,
    room_type                    int,
    use_type                     int,
    supply_light_type            int,
    direction_type               int,
    resolution                   varchar(255),
    business_group_id            varchar(255),
    download_speed               varchar(255),
    svc_space_support_mod        int,
    svc_time_support_mode        int,
    create_time                  varchar(50) not null,
    update_time                  varchar(50) not null,
    sub_count                    int,
    stream_id                    varchar(255),
    has_audio                    bit default 0,
    gps_time                     varchar(50),
    stream_identification        varchar(50),
    channel_type                 int  default 0 not null,
    map_level                    int  default 0,
    gb_device_id                 varchar(50),
    gb_name                      varchar(255),
    gb_manufacturer              varchar(255),
    gb_model                     varchar(255),
    gb_owner                     varchar(255),
    gb_civil_code                varchar(255),
    gb_block                     varchar(255),
    gb_address                   varchar(255),
    gb_parental                  int,
    gb_parent_id                 varchar(255),
    gb_safety_way                int,
    gb_register_way              int,
    gb_cert_num                  varchar(50),
    gb_certifiable               int,
    gb_err_code                  int,
    gb_end_time                  varchar(50),
    gb_secrecy                   int,
    gb_ip_address                varchar(50),
    gb_port                      int,
    gb_password                  varchar(50),
    gb_status                    varchar(50),
    gb_longitude                 double,
    gb_latitude                  double,
    gb_business_group_id         varchar(50),
    gb_ptz_type                  int,
    gb_position_type             int,
    gb_room_type                 int,
    gb_use_type                  int,
    gb_supply_light_type         int,
    gb_direction_type            int,
    gb_resolution                varchar(255),
    gb_download_speed            varchar(255),
    gb_svc_space_support_mod     int,
    gb_svc_time_support_mode     int,
    record_plan_id               int,
    data_type                    int not null,
    data_device_id               int not null,
    gps_speed                    double,
    gps_altitude                 double,
    gps_direction                double,
    enable_broadcast             int default 0,
    constraint uk_wvp_unique_channel unique (gb_device_id),
    constraint uk_device_channel_source unique (data_device_id, device_id)
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

-- 媒体服务器（如 ZLM）节点信息
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_MEDIA_SERVER' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_MEDIA_SERVER';
END IF;
END;
/
CREATE TABLE wvp_media_server
(
    id                  varchar(255) primary key,
    ip                  varchar(50),
    hook_ip             varchar(50),
    sdp_ip              varchar(50),
    stream_ip           varchar(50),
    http_port           int,
    http_ssl_port       int,
    rtmp_port           int,
    rtmp_ssl_port       int,
    rtp_proxy_port      int,
    rtsp_port           int,
    rtsp_ssl_port       int,
    flv_port            int,
    flv_ssl_port        int,
    mp4_port            int,
    mp4_ssl_port        int,
    ws_flv_port         int,
    ws_flv_ssl_port     int,
    jtt_proxy_port      int,
    auto_config         bit default 0,
    secret              varchar(50),
    type                varchar(50) default 'zlm',
    rtp_enable          bit default 0,
    rtp_port_range      varchar(50),
    send_rtp_port_range varchar(50),
    record_assist_port  int,
    default_server      bit default 0,
    create_time         varchar(50),
    update_time         varchar(50),
    hook_alive_interval int,
    record_path         varchar(255),
    record_day          int  default 7,
    transcode_suffix    varchar(255),
    server_id           varchar(50)
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

-- 上级国标平台注册信息
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_PLATFORM' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_PLATFORM';
END IF;
END;
/
CREATE TABLE wvp_platform
(
    id                    int identity(1,1) primary key,
    enable                bit default 0,
    name                  varchar(255),
    server_gb_id          varchar(50),
    server_gb_domain      varchar(50),
    server_ip             varchar(50),
    server_port           int,
    device_gb_id          varchar(50),
    device_ip             varchar(50),
    device_port           varchar(50),
    username              varchar(255),
    password              varchar(50),
    expires               varchar(50),
    keep_timeout          varchar(50),
    transport             varchar(50),
    civil_code            varchar(50),
    manufacturer          varchar(255),
    model                 varchar(255),
    address               varchar(255),
    character_set         varchar(50),
    ptz                   bit default 0,
    rtcp                  bit default 0,
    status                bit default 0,
    catalog_group         int,
    register_way          int,
    secrecy               int,
    create_time           varchar(50),
    update_time           varchar(50),
    as_message_channel    bit default 0,
    catalog_with_platform int default 1,
    catalog_with_group    int default 1,
    catalog_with_region   int default 1,
    auto_push_channel     bit default 1,
    send_stream_ip        varchar(50),
    server_id             varchar(50),
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

-- 国标平台下发的通道映射关系
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_PLATFORM_CHANNEL' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_PLATFORM_CHANNEL';
END IF;
END;
/
CREATE TABLE wvp_platform_channel
(
    id                           int identity(1,1) primary key,
    platform_id                  int,
    device_channel_id            int,
    custom_device_id             varchar(50),
    custom_name                  varchar(255),
    custom_manufacturer          varchar(50),
    custom_model                 varchar(50),
    custom_owner                 varchar(50),
    custom_civil_code            varchar(50),
    custom_block                 varchar(50),
    custom_address               varchar(50),
    custom_parental              int,
    custom_parent_id             varchar(50),
    custom_safety_way            int,
    custom_register_way          int,
    custom_cert_num              varchar(50),
    custom_certifiable           int,
    custom_err_code              int,
    custom_end_time              varchar(50),
    custom_secrecy               int,
    custom_ip_address            varchar(50),
    custom_port                  int,
    custom_password              varchar(255),
    custom_status                varchar(50),
    custom_longitude             double,
    custom_latitude              double,
    custom_ptz_type              int,
    custom_position_type         int,
    custom_room_type             int,
    custom_use_type              int,
    custom_supply_light_type     int,
    custom_direction_type        int,
    custom_resolution            varchar(255),
    custom_business_group_id     varchar(255),
    custom_download_speed        varchar(255),
    custom_svc_space_support_mod int,
    custom_svc_time_support_mode int,
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

-- 平台与分组（行政区划/组织）关系
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_PLATFORM_GROUP' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_PLATFORM_GROUP';
END IF;
END;
/
CREATE TABLE wvp_platform_group
(
    id          int identity(1,1) primary key,
    platform_id int,
    group_id    int,
    constraint uk_wvp_platform_group_platform_id_group_id unique (platform_id, group_id)
);
COMMENT ON TABLE wvp_platform_group IS '平台与分组（行政区划/组织）关系';
COMMENT ON COLUMN wvp_platform_group.id IS '主键ID';
COMMENT ON COLUMN wvp_platform_group.platform_id IS '平台ID';
COMMENT ON COLUMN wvp_platform_group.group_id IS '分组ID';

-- 平台与区域关系
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_PLATFORM_REGION' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_PLATFORM_REGION';
END IF;
END;
/
CREATE TABLE wvp_platform_region
(
    id          int identity(1,1) primary key,
    platform_id int,
    region_id   int,
    constraint uk_wvp_platform_region_platform_id_group_id unique (platform_id, region_id)
);
COMMENT ON TABLE wvp_platform_region IS '平台与区域关系';
COMMENT ON COLUMN wvp_platform_region.id IS '主键ID';
COMMENT ON COLUMN wvp_platform_region.platform_id IS '平台ID';
COMMENT ON COLUMN wvp_platform_region.region_id IS '区域ID';

-- 拉流代理/转推配置
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_STREAM_PROXY' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_STREAM_PROXY';
END IF;
END;
/
CREATE TABLE wvp_stream_proxy
(
    id                         int identity(1,1) primary key,
    type                       varchar(50),
    app                        varchar(255),
    stream                     varchar(255),
    src_url                    varchar(255),
    timeout                    int,
    ffmpeg_cmd_key             varchar(255),
    rtsp_type                  varchar(50),
    media_server_id            varchar(50),
    enable_audio               bit default 0,
    enable_mp4                 bit default 0,
    pulling                    bit default 0,
    enable                     bit default 0,
    create_time                varchar(50),
    name                       varchar(255),
    update_time                varchar(50),
    stream_key                 varchar(255),
    server_id                  varchar(50),
    enable_disable_none_reader bit default 0,
    relates_media_server_id    varchar(50),
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

-- 推流会话记录
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_STREAM_PUSH' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_STREAM_PUSH';
END IF;
END;
/
CREATE TABLE wvp_stream_push
(
    id                 int identity(1,1) primary key,
    app                varchar(255),
    stream             varchar(255),
    create_time        varchar(50),
    media_server_id    varchar(50),
    server_id          varchar(50),
    push_time          varchar(50),
    status             bit default 0,
    update_time        varchar(50),
    pushing            bit default 0,
    self               bit default 0,
    start_offline_push bit default 1,
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

-- 云端录像记录
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_CLOUD_RECORD' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_CLOUD_RECORD';
END IF;
END;
/
CREATE TABLE wvp_cloud_record
(
    id              int identity(1,1) primary key,
    app             varchar(255),
    stream          varchar(255),
    call_id         varchar(255),
    start_time      bigint,
    end_time        bigint,
    media_server_id varchar(50),
    server_id       varchar(50),
    file_name       varchar(255),
    folder          varchar(500),
    file_path       varchar(500),
    collect         bit default 0,
    file_size       bigint,
    time_len        double
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

-- 平台用户信息
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_USER' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_USER';
END IF;
END;
/
CREATE TABLE wvp_user
(
    id               int identity(1,1) primary key,
    username         varchar(255),
    password         varchar(255),
    role_id          int,
    create_time      varchar(50),
    update_time      varchar(50),
    push_key         varchar(50),
    default_password bit default 0,
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
COMMENT ON COLUMN wvp_user.default_password IS '是否使用默认密码';

-- 用户角色信息
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_USER_ROLE' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_USER_ROLE';
END IF;
END;
/
CREATE TABLE wvp_user_role
(
    id          int identity(1,1) primary key,
    name        varchar(50),
    authority   varchar(50),
    create_time varchar(50),
    update_time varchar(50)
);
COMMENT ON TABLE wvp_user_role IS '用户角色信息';
COMMENT ON COLUMN wvp_user_role.id IS '主键ID';
COMMENT ON COLUMN wvp_user_role.name IS '角色名称';
COMMENT ON COLUMN wvp_user_role.authority IS '权限标识';
COMMENT ON COLUMN wvp_user_role.create_time IS '创建时间';
COMMENT ON COLUMN wvp_user_role.update_time IS '更新时间';

-- API Key 信息
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_USER_API_KEY' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_USER_API_KEY';
END IF;
END;
/
CREATE TABLE wvp_user_api_key
(
    id          int identity(1,1) primary key,
    user_id     bigint,
    app         varchar(255),
    api_key     clob,
    expired_at  bigint,
    remark      varchar(255),
    enable      bit default 1,
    create_time varchar(50),
    update_time varchar(50)
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

/*--------------------------------------------------------------------------
 * 初始数据（管理员账号与角色）
 *--------------------------------------------------------------------------*/
SET IDENTITY_INSERT wvp_user ON;
INSERT INTO wvp_user (id, username, password, role_id, create_time, update_time, push_key, default_password)
VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3', 1, '2021-04-13 14:14:57', '2021-04-13 14:14:57',
        '3e80d1762a324d5b0ff636e0bd16f1e3', 1);
SET IDENTITY_INSERT wvp_user OFF;

SET IDENTITY_INSERT wvp_user_role ON;
INSERT INTO wvp_user_role (id, name, authority, create_time, update_time)
VALUES (1, 'admin', '0', '2021-04-13 14:14:57', '2021-04-13 14:14:57');
SET IDENTITY_INSERT wvp_user_role OFF;

-- 通用分组表，存储行业或组织结构
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_COMMON_GROUP' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_COMMON_GROUP';
END IF;
END;
/
CREATE TABLE wvp_common_group
(
    id               int identity(1,1) primary key,
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

-- 通用行政区域表
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_COMMON_REGION' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_COMMON_REGION';
END IF;
END;
/
CREATE TABLE wvp_common_region
(
    id               int identity(1,1) primary key,
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

-- 录像计划基础信息
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_RECORD_PLAN' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_RECORD_PLAN';
END IF;
END;
/
CREATE TABLE wvp_record_plan
(
    id              int identity(1,1) primary key,
    snap            bit default 0,
    name            varchar(255) NOT NULL,
    create_time     varchar(50),
    update_time     varchar(50)
);
COMMENT ON TABLE wvp_record_plan IS '录像计划基础信息';
COMMENT ON COLUMN wvp_record_plan.id IS '主键ID';
COMMENT ON COLUMN wvp_record_plan.snap IS '是否抓图计划';
COMMENT ON COLUMN wvp_record_plan.name IS '计划名称';
COMMENT ON COLUMN wvp_record_plan.create_time IS '创建时间';
COMMENT ON COLUMN wvp_record_plan.update_time IS '更新时间';

-- 录像计划条目表
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_RECORD_PLAN_ITEM' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_RECORD_PLAN_ITEM';
END IF;
END;
/
CREATE TABLE wvp_record_plan_item
(
    id          int identity(1,1) primary key,
    start     int,
    stop        int,
    week_day    int,
    plan_id     int,
    create_time varchar(50),
    update_time varchar(50)
);
COMMENT ON TABLE wvp_record_plan_item IS '录像计划条目表';
COMMENT ON COLUMN wvp_record_plan_item.id IS '主键ID';
COMMENT ON COLUMN wvp_record_plan_item.start IS '开始时间（分钟）';
COMMENT ON COLUMN wvp_record_plan_item.stop IS '结束时间（分钟）';
COMMENT ON COLUMN wvp_record_plan_item.week_day IS '星期（0-6）';
COMMENT ON COLUMN wvp_record_plan_item.plan_id IS '所属录像计划ID';
COMMENT ON COLUMN wvp_record_plan_item.create_time IS '创建时间';
COMMENT ON COLUMN wvp_record_plan_item.update_time IS '更新时间';

-- 交通部 JT/T 1076 终端信息
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_JT_TERMINAL' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_JT_TERMINAL';
END IF;
END;
/
CREATE TABLE wvp_jt_terminal (
                                 id             int identity(1,1) primary key,
                                 phone_number   varchar(50),
                                 terminal_id    varchar(50),
                                 province_id    varchar(50),
                                 province_text  varchar(100),
                                 city_id        varchar(50),
                                 city_text      varchar(100),
                                 maker_id       varchar(50),
                                 model          varchar(50),
                                 plate_color    varchar(50),
                                 plate_no       varchar(50),
                                 longitude      double,
                                 latitude       double,
                                 status         bit default 0,
                                 register_time  varchar(50) default null,
                                 update_time    varchar(50) not null,
                                 create_time    varchar(50) not null,
                                 geo_coord_sys  varchar(50),
                                 media_server_id varchar(50) default 'auto',
                                 sdp_ip         varchar(50),
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

-- 交通部 JT/T 1076 通道信息
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_JT_CHANNEL' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_JT_CHANNEL';
END IF;
END;
/
CREATE TABLE wvp_jt_channel (
                                id             int identity(1,1) primary key,
                                terminal_db_id int,
                                channel_id     int,
                                has_audio      bit default 0,
                                name           varchar(255),
                                update_time    varchar(50) not null,
                                create_time    varchar(50) not null,
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

-- 报警信息表
DECLARE
V_CNT INT;
BEGIN
SELECT COUNT(*) INTO V_CNT FROM SYSOBJECTS WHERE NAME = 'WVP_ALARM' AND TYPE$ = 'SCHOBJ';
IF V_CNT > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE WVP_ALARM';
END IF;
END;
/
CREATE TABLE wvp_alarm (
                           id          int identity(1,1) primary key,
                           channel_id  int,
                           description varchar(255),
                           snap_path   varchar(255),
                           record_path varchar(255),
                           longitude   double,
                           latitude    double,
                           alarm_type  int,
                           alarm_time  bigint
);
COMMENT ON COLUMN wvp_alarm.id IS '主键ID';
COMMENT ON COLUMN wvp_alarm.channel_id IS '关联通道的数据库id';
COMMENT ON COLUMN wvp_alarm.description IS '报警描述';
COMMENT ON COLUMN wvp_alarm.snap_path IS '报警快照路径';
COMMENT ON COLUMN wvp_alarm.record_path IS '报警录像路径';
COMMENT ON COLUMN wvp_alarm.longitude IS '报警附带的经度';
COMMENT ON COLUMN wvp_alarm.latitude IS '报警附带的纬度';
COMMENT ON COLUMN wvp_alarm.alarm_type IS '报警类别';
COMMENT ON COLUMN wvp_alarm.alarm_time IS '报警时间';
