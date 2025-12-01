/*建表*/
-- 存储国标设备的基础信息及在线状态
drop table IF EXISTS wvp_device;
create table IF NOT EXISTS wvp_device
(
    id                                  serial primary key COMMENT '主键ID',
    device_id                           character varying(50) not null COMMENT '国标设备编号',
    name                                character varying(255) COMMENT '设备名称',
    manufacturer                        character varying(255) COMMENT '设备厂商',
    model                               character varying(255) COMMENT '设备型号',
    firmware                            character varying(255) COMMENT '固件版本号',
    transport                           character varying(50) COMMENT '信令传输协议（TCP/UDP）',
    stream_mode                         character varying(50) COMMENT '拉流方式（主动/被动）',
    on_line                             bool    default false COMMENT '在线状态',
    register_time                       character varying(50) COMMENT '注册时间',
    keepalive_time                      character varying(50) COMMENT '最近心跳时间',
    ip                                  character varying(50) COMMENT '设备IP地址',
    create_time                         character varying(50) COMMENT '创建时间',
    update_time                         character varying(50) COMMENT '更新时间',
    port                                integer COMMENT '信令端口',
    expires                             integer COMMENT '注册有效期',
    subscribe_cycle_for_catalog         integer DEFAULT 0 COMMENT '目录订阅周期',
    subscribe_cycle_for_mobile_position integer DEFAULT 0 COMMENT '移动位置订阅周期',
    mobile_position_submission_interval integer DEFAULT 5 COMMENT '移动位置上报间隔',
    subscribe_cycle_for_alarm           integer DEFAULT 0 COMMENT '报警订阅周期',
    host_address                        character varying(50) COMMENT '设备域名/主机地址',
    charset                             character varying(50) COMMENT '信令字符集',
    ssrc_check                          bool    default false COMMENT '是否校验SSRC',
    geo_coord_sys                       character varying(50) COMMENT '坐标系类型',
    media_server_id                     character varying(50) default 'auto' COMMENT '绑定的流媒体服务ID',
    custom_name                         character varying(255) COMMENT '自定义显示名称',
    sdp_ip                              character varying(50) COMMENT 'SDP中携带的IP',
    local_ip                            character varying(50) COMMENT '本地局域网IP',
    password                            character varying(255) COMMENT '设备鉴权密码',
    as_message_channel                  bool    default false COMMENT '是否作为消息通道',
    heart_beat_interval                 integer COMMENT '心跳间隔',
    heart_beat_count                    integer COMMENT '心跳失败次数',
    position_capability                 integer COMMENT '定位能力标识',
    broadcast_push_after_ack            bool    default false COMMENT 'ACK后是否自动推流',
    server_id                           character varying(50) COMMENT '所属信令服务器ID',
    constraint uk_device_device unique (device_id)
);

-- 记录各设备上报的报警信息
drop table IF EXISTS wvp_device_alarm;
create table IF NOT EXISTS wvp_device_alarm
(
    id                serial primary key COMMENT '主键ID',
    device_id         character varying(50) not null COMMENT '国标设备ID',
    channel_id        character varying(50) not null COMMENT '报警关联的通道ID',
    alarm_priority    character varying(50) COMMENT '报警级别',
    alarm_method      character varying(50) COMMENT '报警方式（视频/语音等）',
    alarm_time        character varying(50) COMMENT '报警发生时间',
    alarm_description character varying(255) COMMENT '报警描述',
    longitude         double precision COMMENT '报警经度',
    latitude          double precision COMMENT '报警纬度',
    alarm_type        character varying(50) COMMENT '报警类型',
    create_time       character varying(50) not null COMMENT '数据入库时间'
);

-- 存储移动位置订阅上报的数据
drop table IF EXISTS wvp_device_mobile_position;
create table IF NOT EXISTS wvp_device_mobile_position
(
    id              serial primary key COMMENT '主键ID',
    device_id       character varying(50) not null COMMENT '设备ID',
    channel_id      character varying(50) not null COMMENT '通道ID',
    device_name     character varying(255) COMMENT '设备名称',
    time            character varying(50) COMMENT '上报时间',
    longitude       double precision COMMENT '经度',
    latitude        double precision COMMENT '纬度',
    altitude        double precision COMMENT '海拔',
    speed           double precision COMMENT '速度',
    direction       double precision COMMENT '方向角',
    report_source   character varying(50) COMMENT '上报来源',
    create_time     character varying(50) COMMENT '入库时间'
);

-- 保存设备下的通道信息以及扩展属性
drop table IF EXISTS wvp_device_channel;
create table IF NOT EXISTS wvp_device_channel
(
    id                           serial primary key COMMENT '主键ID',
    device_id                    character varying(50) COMMENT '所属设备ID',
    name                         character varying(255) COMMENT '通道名称',
    manufacturer                 character varying(50) COMMENT '厂商',
    model                        character varying(50) COMMENT '型号',
    owner                        character varying(50) COMMENT '归属单位',
    civil_code                   character varying(50) COMMENT '行政区划代码',
    block                        character varying(50) COMMENT '区域/小区编号',
    address                      character varying(50) COMMENT '安装地址',
    parental                     integer COMMENT '是否有子节点',
    parent_id                    character varying(50) COMMENT '父级通道ID',
    safety_way                   integer COMMENT '安全防范等级',
    register_way                 integer COMMENT '注册方式',
    cert_num                     character varying(50) COMMENT '证书编号',
    certifiable                  integer COMMENT '是否可认证',
    err_code                     integer COMMENT '故障状态码',
    end_time                     character varying(50) COMMENT '服务截止时间',
    secrecy                      integer COMMENT '保密级别',
    ip_address                   character varying(50) COMMENT '设备IP地址',
    port                         integer COMMENT '设备端口',
    password                     character varying(255) COMMENT '访问密码',
    status                       character varying(50) COMMENT '在线状态',
    longitude                    double precision COMMENT '经度',
    latitude                     double precision COMMENT '纬度',
    ptz_type                     integer COMMENT '云台类型',
    position_type                integer COMMENT '点位类型',
    room_type                    integer COMMENT '房间类型',
    use_type                     integer COMMENT '使用性质',
    supply_light_type            integer COMMENT '补光方式',
    direction_type               integer COMMENT '朝向',
    resolution                   character varying(255) COMMENT '分辨率',
    business_group_id            character varying(255) COMMENT '业务分组ID',
    download_speed               character varying(255) COMMENT '下载/码流速率',
    svc_space_support_mod        integer COMMENT '空域SVC能力',
    svc_time_support_mode        integer COMMENT '时域SVC能力',
    create_time                  character varying(50) not null COMMENT '创建时间',
    update_time                  character varying(50) not null COMMENT '更新时间',
    sub_count                    integer COMMENT '子节点数量',
    stream_id                    character varying(255) COMMENT '绑定的流ID',
    has_audio                    bool default false COMMENT '是否有音频',
    gps_time                     character varying(50) COMMENT 'GPS定位时间',
    stream_identification        character varying(50) COMMENT '流标识',
    channel_type                 int  default 0 not null COMMENT '通道类型',
    map_level                    int  default 0 COMMENT '地图层级',
    gb_device_id                 character varying(50) COMMENT 'GB内的设备ID',
    gb_name                      character varying(255) COMMENT 'GB上报的名称',
    gb_manufacturer              character varying(255) COMMENT 'GB厂商',
    gb_model                     character varying(255) COMMENT 'GB型号',
    gb_owner                     character varying(255) COMMENT 'GB归属',
    gb_civil_code                character varying(255) COMMENT 'GB行政区划',
    gb_block                     character varying(255) COMMENT 'GB区域',
    gb_address                   character varying(255) COMMENT 'GB地址',
    gb_parental                  integer COMMENT 'GB子节点标识',
    gb_parent_id                 character varying(255) COMMENT 'GB父通道',
    gb_safety_way                integer COMMENT 'GB安全防范',
    gb_register_way              integer COMMENT 'GB注册方式',
    gb_cert_num                  character varying(50) COMMENT 'GB证书编号',
    gb_certifiable               integer COMMENT 'GB认证标志',
    gb_err_code                  integer COMMENT 'GB错误码',
    gb_end_time                  character varying(50) COMMENT 'GB截止时间',
    gb_secrecy                   integer COMMENT 'GB保密级别',
    gb_ip_address                character varying(50) COMMENT 'GB IP',
    gb_port                      integer COMMENT 'GB端口',
    gb_password                  character varying(50) COMMENT 'GB接入密码',
    gb_status                    character varying(50) COMMENT 'GB状态',
    gb_longitude                 double COMMENT 'GB经度',
    gb_latitude                  double COMMENT 'GB纬度',
    gb_business_group_id         character varying(50) COMMENT 'GB业务分组',
    gb_ptz_type                  integer COMMENT 'GB云台类型',
    gb_position_type             integer COMMENT 'GB点位类型',
    gb_room_type                 integer COMMENT 'GB房间类型',
    gb_use_type                  integer COMMENT 'GB用途',
    gb_supply_light_type         integer COMMENT 'GB补光',
    gb_direction_type            integer COMMENT 'GB朝向',
    gb_resolution                character varying(255) COMMENT 'GB分辨率',
    gb_download_speed            character varying(255) COMMENT 'GB码流速率',
    gb_svc_space_support_mod     integer COMMENT 'GB空域SVC',
    gb_svc_time_support_mode     integer COMMENT 'GB时域SVC',
    record_plan_id               integer COMMENT '绑定的录像计划ID',
    data_type                    integer not null COMMENT '数据类型标识',
    data_device_id               integer not null COMMENT '数据来源设备主键',
    gps_speed                    double precision COMMENT 'GPS速度',
    gps_altitude                 double precision COMMENT 'GPS海拔',
    gps_direction                double precision COMMENT 'GPS方向',
    enable_broadcast             integer default 0 COMMENT '是否支持广播',
    index (data_type),
    index (data_device_id),
    constraint uk_wvp_unique_channel unique (gb_device_id)
);

-- 媒体服务器（如 ZLM）节点信息
drop table IF EXISTS wvp_media_server;
create table IF NOT EXISTS wvp_media_server
(
    id                  character varying(255) primary key COMMENT '媒体服务器ID',
    ip                  character varying(50) COMMENT '服务器IP',
    hook_ip             character varying(50) COMMENT 'hook回调IP',
    sdp_ip              character varying(50) COMMENT 'SDP中使用的IP',
    stream_ip           character varying(50) COMMENT '推流使用的IP',
    http_port           integer COMMENT 'HTTP端口',
    http_ssl_port       integer COMMENT 'HTTPS端口',
    rtmp_port           integer COMMENT 'RTMP端口',
    rtmp_ssl_port       integer COMMENT 'RTMPS端口',
    rtp_proxy_port      integer COMMENT 'RTP代理端口',
    rtsp_port           integer COMMENT 'RTSP端口',
    rtsp_ssl_port       integer COMMENT 'RTSPS端口',
    flv_port            integer COMMENT 'FLV端口',
    flv_ssl_port        integer COMMENT 'FLV HTTPS端口',
    mp4_port            integer COMMENT 'MP4点播端口',
    mp4_ssl_port        integer COMMENT 'MP4 HTTPS端口',
    ws_flv_port         integer COMMENT 'WS-FLV端口',
    ws_flv_ssl_port     integer COMMENT 'WS-FLV HTTPS端口',
    jtt_proxy_port      integer COMMENT 'JT/T代理端口',
    auto_config         bool                  default false COMMENT '是否自动配置',
    secret              character varying(50) COMMENT 'ZLM校验密钥',
    type                character varying(50) default 'zlm' COMMENT '节点类型',
    rtp_enable          bool                  default false COMMENT '是否开启RTP',
    rtp_port_range      character varying(50) COMMENT 'RTP端口范围',
    send_rtp_port_range character varying(50) COMMENT '发送RTP端口范围',
    record_assist_port  integer COMMENT '录像辅助端口',
    default_server      bool                  default false COMMENT '是否默认节点',
    create_time         character varying(50) COMMENT '创建时间',
    update_time         character varying(50) COMMENT '更新时间',
    hook_alive_interval integer COMMENT 'hook心跳间隔',
    record_path         character varying(255) COMMENT '录像目录',
    record_day          integer               default 7 COMMENT '录像保留天数',
    transcode_suffix    character varying(255) COMMENT '转码指令后缀',
    server_id           character varying(50) COMMENT '对应信令服务器ID',
    constraint uk_media_server_unique_ip_http_port unique (ip, http_port, server_id)
);

-- 上级国标平台注册信息
drop table IF EXISTS wvp_platform;
create table IF NOT EXISTS wvp_platform
(
    id                    serial primary key COMMENT '主键ID',
    enable                bool default false COMMENT '是否启用该平台注册',
    name                  character varying(255) COMMENT '平台名称',
    server_gb_id          character varying(50) COMMENT '上级平台国标编码',
    server_gb_domain      character varying(50) COMMENT '上级平台域编码',
    server_ip             character varying(50) COMMENT '上级平台IP',
    server_port           integer COMMENT '上级平台注册端口',
    device_gb_id          character varying(50) COMMENT '本平台向上注册的国标编码',
    device_ip             character varying(50) COMMENT '本平台信令IP',
    device_port           character varying(50) COMMENT '本平台信令端口',
    username              character varying(255) COMMENT '注册用户名',
    password              character varying(50) COMMENT '注册密码',
    expires               character varying(50) COMMENT '注册有效期',
    keep_timeout          character varying(50) COMMENT '心跳超时时间',
    transport             character varying(50) COMMENT '传输协议（UDP/TCP）',
    civil_code            character varying(50) COMMENT '行政区划代码',
    manufacturer          character varying(255) COMMENT '厂商',
    model                 character varying(255) COMMENT '型号',
    address               character varying(255) COMMENT '地址',
    character_set         character varying(50) COMMENT '字符集',
    ptz                   bool default false COMMENT '是否支持PTZ',
    rtcp                  bool default false COMMENT '是否开启RTCP',
    status                bool default false COMMENT '注册状态',
    catalog_group         integer COMMENT '目录分组方式',
    register_way          integer COMMENT '注册方式',
    secrecy               integer COMMENT '保密级别',
    create_time           character varying(50) COMMENT '创建时间',
    update_time           character varying(50) COMMENT '更新时间',
    as_message_channel    bool default false COMMENT '是否作为消息通道',
    catalog_with_platform integer default 1 COMMENT '是否推送平台目录',
    catalog_with_group    integer default 1 COMMENT '是否推送分组目录',
    catalog_with_region   integer default 1 COMMENT '是否推送区域目录',
    auto_push_channel     bool default true COMMENT '是否自动推送通道',
    send_stream_ip        character varying(50) COMMENT '推流时使用的IP',
    server_id             character varying(50) COMMENT '对应信令服务器ID',
    constraint uk_platform_unique_server_gb_id unique (server_gb_id)
);

-- 国标平台下发的通道映射关系
drop table IF EXISTS wvp_platform_channel;
create table IF NOT EXISTS wvp_platform_channel
(
    id                           serial primary key COMMENT '主键ID',
    platform_id                  integer COMMENT '平台ID',
    device_channel_id            integer COMMENT '本地通道表主键',
    custom_device_id             character varying(50) COMMENT '自定义国标编码',
    custom_name                  character varying(255) COMMENT '自定义名称',
    custom_manufacturer          character varying(50) COMMENT '自定义厂商',
    custom_model                 character varying(50) COMMENT '自定义型号',
    custom_owner                 character varying(50) COMMENT '自定义归属',
    custom_civil_code            character varying(50) COMMENT '自定义行政区划',
    custom_block                 character varying(50) COMMENT '自定义区域',
    custom_address               character varying(50) COMMENT '自定义地址',
    custom_parental              integer COMMENT '自定义父/子标识',
    custom_parent_id             character varying(50) COMMENT '自定义父节点',
    custom_safety_way            integer COMMENT '自定义安全防范',
    custom_register_way          integer COMMENT '自定义注册方式',
    custom_cert_num              character varying(50) COMMENT '自定义证书编号',
    custom_certifiable           integer COMMENT '自定义可认证标志',
    custom_err_code              integer COMMENT '自定义错误码',
    custom_end_time              character varying(50) COMMENT '自定义截止时间',
    custom_secrecy               integer COMMENT '自定义保密级别',
    custom_ip_address            character varying(50) COMMENT '自定义IP',
    custom_port                  integer COMMENT '自定义端口',
    custom_password              character varying(255) COMMENT '自定义密码',
    custom_status                character varying(50) COMMENT '自定义状态',
    custom_longitude             double precision COMMENT '自定义经度',
    custom_latitude              double precision COMMENT '自定义纬度',
    custom_ptz_type              integer COMMENT '自定义云台类型',
    custom_position_type         integer COMMENT '自定义点位类型',
    custom_room_type             integer COMMENT '自定义房间类型',
    custom_use_type              integer COMMENT '自定义用途',
    custom_supply_light_type     integer COMMENT '自定义补光',
    custom_direction_type        integer COMMENT '自定义朝向',
    custom_resolution            character varying(255) COMMENT '自定义分辨率',
    custom_business_group_id     character varying(255) COMMENT '自定义业务分组',
    custom_download_speed        character varying(255) COMMENT '自定义码流速率',
    custom_svc_space_support_mod integer COMMENT '自定义空域SVC',
    custom_svc_time_support_mode integer COMMENT '自定义时域SVC',
    constraint uk_platform_gb_channel_platform_id_catalog_id_device_channel_id unique (platform_id, device_channel_id),
    constraint uk_platform_gb_channel_device_id unique (custom_device_id)
);

-- 平台与分组（行政区划/组织）关系
drop table IF EXISTS wvp_platform_group;
create table IF NOT EXISTS wvp_platform_group
(
    id          serial primary key COMMENT '主键ID',
    platform_id integer COMMENT '平台ID',
    group_id    integer COMMENT '分组ID',
    constraint uk_wvp_platform_group_platform_id_group_id unique (platform_id, group_id)
);

-- 平台与区域关系
drop table IF EXISTS wvp_platform_region;
create table IF NOT EXISTS wvp_platform_region
(
    id          serial primary key COMMENT '主键ID',
    platform_id integer COMMENT '平台ID',
    region_id   integer COMMENT '区域ID',
    constraint uk_wvp_platform_region_platform_id_group_id unique (platform_id, region_id)
);

-- 拉流代理/转推配置
drop table IF EXISTS wvp_stream_proxy;
create table IF NOT EXISTS wvp_stream_proxy
(
    id                         serial primary key COMMENT '主键ID',
    type                       character varying(50) COMMENT '代理类型（拉流/推流）',
    app                        character varying(255) COMMENT '应用名',
    stream                     character varying(255) COMMENT '流ID',
    src_url                    character varying(255) COMMENT '源地址',
    timeout                    integer COMMENT '拉流超时时间',
    ffmpeg_cmd_key             character varying(255) COMMENT 'FFmpeg命令模板键',
    rtsp_type                  character varying(50) COMMENT 'RTSP拉流方式',
    media_server_id            character varying(50) COMMENT '指定媒体服务器ID',
    enable_audio               bool default false COMMENT '是否启用音频',
    enable_mp4                 bool default false COMMENT '是否录制MP4',
    pulling                    bool default false COMMENT '当前是否在拉流',
    enable                     bool default false COMMENT '是否启用该代理',
    create_time                character varying(50) COMMENT '创建时间',
    name                       character varying(255) COMMENT '代理名称',
    update_time                character varying(50) COMMENT '更新时间',
    stream_key                 character varying(255) COMMENT '唯一流标识',
    server_id                  character varying(50) COMMENT '信令服务器ID',
    enable_disable_none_reader bool default false COMMENT '是否无人观看时自动停流',
    relates_media_server_id    character varying(50) COMMENT '关联的媒体服务器ID',
    constraint uk_stream_proxy_app_stream unique (app, stream)
);

-- 推流会话记录
drop table IF EXISTS wvp_stream_push;
create table IF NOT EXISTS wvp_stream_push
(
    id                 serial primary key COMMENT '主键ID',
    app                character varying(255) COMMENT '应用名',
    stream             character varying(255) COMMENT '流ID',
    create_time        character varying(50) COMMENT '创建时间',
    media_server_id    character varying(50) COMMENT '推流所在媒体服务器',
    server_id          character varying(50) COMMENT '信令服务器ID',
    push_time          character varying(50) COMMENT '推流开始时间',
    status             bool default false COMMENT '推流状态',
    update_time        character varying(50) COMMENT '更新时间',
    pushing            bool default false COMMENT '是否正在推流',
    self               bool default false COMMENT '是否本地发起',
    start_offline_push bool default true COMMENT '是否离线后自动重推',
    constraint uk_stream_push_app_stream unique (app, stream)
);

-- 云端录像记录
drop table IF EXISTS wvp_cloud_record;
create table IF NOT EXISTS wvp_cloud_record
(
    id              serial primary key COMMENT '主键ID',
    app             character varying(255) COMMENT '应用名',
    stream          character varying(255) COMMENT '流ID',
    call_id         character varying(255) COMMENT '会话ID',
    start_time      bigint COMMENT '录像开始时间',
    end_time        bigint COMMENT '录像结束时间',
    media_server_id character varying(50) COMMENT '媒体服务器ID',
    server_id       character varying(50) COMMENT '信令服务器ID',
    file_name       character varying(255) COMMENT '文件名',
    folder          character varying(500) COMMENT '目录',
    file_path       character varying(500) COMMENT '完整路径',
    collect         bool default false COMMENT '是否收藏',
    file_size       bigint COMMENT '文件大小',
    time_len        double precision COMMENT '时长'
);

-- 平台用户信息
drop table IF EXISTS wvp_user;
create table IF NOT EXISTS wvp_user
(
    id          serial primary key COMMENT '主键ID',
    username    character varying(255) COMMENT '用户名',
    password    character varying(255) COMMENT '密码（MD5）',
    role_id     integer COMMENT '角色ID',
    create_time character varying(50) COMMENT '创建时间',
    update_time character varying(50) COMMENT '更新时间',
    push_key    character varying(50) COMMENT '推送密钥',
    constraint uk_user_username unique (username)
);

-- 用户角色信息
drop table IF EXISTS wvp_user_role;
create table IF NOT EXISTS wvp_user_role
(
    id          serial primary key COMMENT '主键ID',
    name        character varying(50) COMMENT '角色名称',
    authority   character varying(50) COMMENT '权限标识',
    create_time character varying(50) COMMENT '创建时间',
    update_time character varying(50) COMMENT '更新时间'
);


drop table IF EXISTS wvp_user_api_key;
create table IF NOT EXISTS wvp_user_api_key
(
    id          serial primary key COMMENT '主键ID',
    user_id     bigint COMMENT '关联用户ID',
    app         character varying(255) COMMENT '应用标识',
    api_key     text COMMENT 'API Key',
    expired_at  bigint COMMENT '过期时间戳',
    remark      character varying(255) COMMENT '备注',
    enable      bool default true COMMENT '是否启用',
    create_time character varying(50) COMMENT '创建时间',
    update_time character varying(50) COMMENT '更新时间'
);


/*初始数据*/
-- 初始化管理员账号，账号admin 密码admin（MD5加密后）
INSERT INTO wvp_user
VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3', 1, '2021-04-13 14:14:57', '2021-04-13 14:14:57',
        '3e80d1762a324d5b0ff636e0bd16f1e3');
-- 初始化管理员角色
INSERT INTO wvp_user_role
VALUES (1, 'admin', '0', '2021-04-13 14:14:57', '2021-04-13 14:14:57');

-- 通用分组表，存储行业或组织结构
drop table IF EXISTS wvp_common_group;
create table IF NOT EXISTS wvp_common_group
(
    id               serial primary key COMMENT '主键ID',
    device_id        varchar(50)  NOT NULL COMMENT '分组对应的平台或设备ID',
    name             varchar(255) NOT NULL COMMENT '分组名称',
    parent_id        int COMMENT '父级分组ID',
    parent_device_id varchar(50) DEFAULT NULL COMMENT '父级分组对应的设备ID',
    business_group   varchar(50)  NOT NULL COMMENT '业务分组编码',
    create_time      varchar(50)  NOT NULL COMMENT '创建时间',
    update_time      varchar(50)  NOT NULL COMMENT '更新时间',
    civil_code       varchar(50) default null COMMENT '行政区划代码',
    alias            varchar(255) default null COMMENT '别名',
    constraint uk_common_group_device_platform unique (device_id)
);

-- 通用行政区域表
drop table IF EXISTS wvp_common_region;
create table IF NOT EXISTS wvp_common_region
(
    id               serial primary key COMMENT '主键ID',
    device_id        varchar(50)  NOT NULL COMMENT '区域对应的平台或设备ID',
    name             varchar(255) NOT NULL COMMENT '区域名称',
    parent_id        int COMMENT '父级区域ID',
    parent_device_id varchar(50) DEFAULT NULL COMMENT '父级区域的设备ID',
    create_time      varchar(50)  NOT NULL COMMENT '创建时间',
    update_time      varchar(50)  NOT NULL COMMENT '更新时间',
    constraint uk_common_region_device_id unique (device_id)
);

-- 录像计划基础信息
drop table IF EXISTS wvp_record_plan;
create table IF NOT EXISTS wvp_record_plan
(
    id              serial primary key COMMENT '主键ID',
    snap            bool default false COMMENT '是否抓图计划',
    name            varchar(255) NOT NULL COMMENT '计划名称',
    create_time     character varying(50) COMMENT '创建时间',
    update_time     character varying(50) COMMENT '更新时间'
);

-- 录像计划条目表
drop table IF EXISTS wvp_record_plan_item;
create table IF NOT EXISTS wvp_record_plan_item
(
    id              serial primary key COMMENT '主键ID',
    start           int COMMENT '开始时间（分钟）',
    stop            int COMMENT '结束时间（分钟）',
    week_day        int COMMENT '星期（0-6）',
    plan_id         int COMMENT '所属录像计划ID',
    create_time     character varying(50) COMMENT '创建时间',
    update_time     character varying(50) COMMENT '更新时间'
);

-- 交通部 JT/T 1076 终端信息
drop table IF EXISTS wvp_jt_terminal;
create table IF NOT EXISTS wvp_jt_terminal (
                                 id serial primary key COMMENT '主键ID',
                                 phone_number character varying(50) COMMENT '终端SIM卡号',
                                 terminal_id character varying(50) COMMENT '终端设备ID',
                                 province_id character varying(50) COMMENT '所在省份ID',
                                 province_text character varying(100) COMMENT '所在省份名称',
                                 city_id character varying(50) COMMENT '所在城市ID',
                                 city_text character varying(100) COMMENT '所在城市名称',
                                 maker_id character varying(50) COMMENT '厂商ID',
                                 model character varying(50) COMMENT '终端型号',
                                 plate_color character varying(50) COMMENT '车牌颜色',
                                 plate_no character varying(50) COMMENT '车牌号码',
                                 longitude double precision COMMENT '经度',
                                 latitude double precision COMMENT '纬度',
                                 status bool default false COMMENT '在线状态',
                                 register_time character varying(50) default null COMMENT '注册时间',
                                 update_time character varying(50) not null COMMENT '更新时间',
                                 create_time character varying(50) not null COMMENT '创建时间',
                                 geo_coord_sys character varying(50) COMMENT '坐标系',
                                 media_server_id character varying(50) default 'auto' COMMENT '媒体服务器ID',
                                 sdp_ip character varying(50) COMMENT 'SDP IP',
                                 constraint uk_jt_device_id_device_id unique (id, phone_number)
);

-- 交通部 JT/T 1076 通道信息
drop table IF EXISTS wvp_jt_channel;
create table IF NOT EXISTS wvp_jt_channel (
                               id serial primary key COMMENT '主键ID',
                               terminal_db_id integer COMMENT '所属终端记录ID',
                               channel_id integer COMMENT '通道号',
                               has_audio bool default false COMMENT '是否有音频',
                               name character varying(255) COMMENT '通道名称',
                               update_time character varying(50) not null COMMENT '更新时间',
                               create_time character varying(50) not null COMMENT '创建时间',
                               constraint uk_jt_channel_id_device_id unique (terminal_db_id, channel_id)
);
