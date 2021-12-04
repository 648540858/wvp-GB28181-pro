-- auto-generated definition


CREATE DATABASE `wvp` /*!40100 DEFAULT CHARACTER SET utf8mb3 COLLATE utf8mb3_bin */;

use wvp;

create table device
(
    deviceId      varchar(50)  not null
        primary key,
    name          varchar(255) null,
    manufacturer  varchar(255) null,
    model         varchar(255) null,
    firmware      varchar(255) null,
    transport     varchar(50)  null,
    streamMode    varchar(50)  null,
    online        varchar(50)  null,
    registerTime  varchar(50)  null,
    keepaliveTime varchar(50)  null,
    ip            varchar(50)  not null,
    createTime    varchar(50)  not null,
    updateTime    varchar(50)  not null,
    port          int          not null,
    expires       int          not null,
    subscribeCycleForCatalog       int          not null,
    hostAddress   varchar(50)  not null,
    charset       varchar(50)  not null
);

create table device_channel
(
    channelId   varchar(50)  not null,
    name        varchar(255) null,
    manufacture varchar(50)  null,
    model       varchar(50)  null,
    owner       varchar(50)  null,
    civilCode   varchar(50)  null,
    block       varchar(50)  null,
    address     varchar(50)  null,
    parentId    varchar(50)  null,
    safetyWay   int          null,
    registerWay int          null,
    certNum     varchar(50)  null,
    certifiable int          null,
    errCode     int          null,
    endTime     varchar(50)  null,
    secrecy     varchar(50)  null,
    ipAddress   varchar(50)  null,
    port        int          null,
    password    varchar(255) null,
    PTZType     int          null,
    status      int          null,
    longitude   double       null,
    latitude    double       null,
    streamId    varchar(50)  null,
    deviceId    varchar(50)  not null,
    parental    varchar(50)  null,
    hasAudio    bit          null,
    createTime  varchar(50)  not null,
    updateTime  varchar(50)  not null,
    primary key (channelId, deviceId)
);

create table device_alarm
(
    id                int auto_increment
        primary key,
    deviceId          varchar(50) not null,
    channelId         varchar(50) not null,
    alarmPriority     varchar(50) not null,
    alarmMethod       varchar(50),
    alarmTime         varchar(50) not null,
    alarmDescription  varchar(255),
    longitude         double null,
    latitude          double null,
    alarmType         varchar(50)
);

create table log
(
    id                int auto_increment
        primary key,
    name        varchar(50)     not null,
    type        varchar(50)     not null,
    uri         varchar(200)    not null,
    address     varchar(50)     not null,
    result      varchar(50)     not null,
    timing      bigint          not null,
    username    varchar(50)     not null,
    createTime  varchar(50)     not null
);

create table device_mobile_position
(
    deviceId       varchar(50)  not null,
    channelId      varchar(50)  not null,
    deviceName     varchar(255) null,
    time           varchar(50)  not null,
    longitude      double       not null,
    latitude       double       not null,
    altitude       double       null,
    speed          double       null,
    direction      double       null,
    reportSource   varchar(50)  null,
    geodeticSystem varchar(50)  null,
    cnLng          varchar(50)  null,
    cnLat          varchar(50)  null,
    primary key (deviceId, time)
);

create table gb_stream
(
    app           varchar(255) not null,
    stream        varchar(255) not null,
    gbId          varchar(50)  not null,
    name          varchar(255) null,
    longitude     double       null,
    latitude      double       null,
    streamType    varchar(50)  null,
    mediaServerId varchar(50)  null,
    status        int          null,
    primary key (app, stream, gbId)
);

create table media_server
(
    id                      varchar(255) not null
        primary key,
    ip                      varchar(50)  not null,
    hookIp                  varchar(50)  not null,
    sdpIp                   varchar(50)  not null,
    streamIp                varchar(50)  not null,
    httpPort                int          not null,
    httpSSlPort             int          not null,
    rtmpPort                int          not null,
    rtmpSSlPort             int          not null,
    rtpProxyPort            int          not null,
    rtspPort                int          not null,
    rtspSSLPort             int          not null,
    autoConfig              int          not null,
    secret                  varchar(50)  not null,
    streamNoneReaderDelayMS int          not null,
    rtpEnable               int          not null,
    rtpPortRange            varchar(50)  not null,
    sendRtpPortRange        varchar(50)  not null,
    recordAssistPort        int          not null,
    defaultServer           int          not null,
    createTime              varchar(50)  not null,
    updateTime              varchar(50)  not null,
    constraint media_server_i
        unique (ip, httpPort)
);

create table parent_platform
(
    id             int auto_increment,
    enable         int          null,
    name           varchar(255) null,
    serverGBId     varchar(50)  not null,
    serverGBDomain varchar(50)  null,
    serverIP       varchar(50)  null,
    serverPort     int          null,
    deviceGBId     varchar(50)  not null,
    deviceIp       varchar(50)  null,
    devicePort     varchar(50)  null,
    username       varchar(255) null,
    password       varchar(50)  null,
    expires        varchar(50)  null,
    keepTimeout    varchar(50)  null,
    transport      varchar(50)  null,
    characterSet   varchar(50)  null,
    ptz            int          null,
    rtcp           int          null,
    status         bit          null,
    shareAllLiveStream         int          null,
    primary key (id, serverGBId)
);

create table platform_gb_channel
(
    channelId          varchar(50) not null,
    deviceId           varchar(50) not null,
    platformId         varchar(50) not null,
    deviceAndChannelId varchar(50) not null,
    primary key (deviceAndChannelId, platformId)
);

create table platform_gb_stream
(
    platformId varchar(50)  not null,
    app        varchar(255) not null,
    stream     varchar(255) not null,
    primary key (platformId, app, stream)
);

create table stream_proxy
(
    type           varchar(50)  not null,
    app            varchar(255) not null,
    stream         varchar(255) not null,
    url            varchar(255) null,
    src_url        varchar(255) null,
    dst_url        varchar(255) null,
    timeout_ms     int          null,
    ffmpeg_cmd_key varchar(255) null,
    rtp_type       varchar(50)  null,
    mediaServerId  varchar(50)  null,
    enable_hls     bit          null,
    enable_mp4     bit          null,
    enable         bit          not null,
    enable_remove_none_reader    bit          not null,
    createTime     varchar(50)  not null,
    primary key (app, stream)
);

create table stream_push
(
    app              varchar(255) not null,
    stream           varchar(255) not null,
    totalReaderCount varchar(50)  null,
    originType       int          null,
    originTypeStr    varchar(50)  null,
    createStamp      int          null,
    aliveSecond      int          null,
    mediaServerId    varchar(50)  null,
    primary key (app, stream)
);

create table user
(
    id          int auto_increment
        primary key,
    username    varchar(255) not null,
    password    varchar(255) not null,
    roleId      int          not null,
    createTime varchar(50)  not null,
    updateTime varchar(50)  not null
);

create unique index user_username_uindex
    on user (username);

insert into user (username, password, roleId, createTime, updateTime) values ('admin', '21232f297a57a5a743894a0e4a801fc3', '1', '2021-04-13 14:14:57', '2021-04-13 14:14:57');

create table role (
        id          int auto_increment
                    primary key,
        name        TEXT NOT NULL,
        authority   TEXT NOT NULL,
        createTime  varchar(50)  not null,
        updateTime  varchar(50)  not null
);
insert into role (id, name, authority, createTime, updateTime) values ('1', 'admin', '0', '2021-04-13 14:14:57', '2021-04-13 14:14:57');

