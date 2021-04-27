-- auto-generated definition
create schema wvp collate utf8_bin;

create table device
(
    deviceId           varchar(50) not null
        primary key,
    name               varchar(50) null,
    manufacturer       varchar(50) null,
    model              varchar(50) null,
    firmware           varchar(50) null,
    transport          varchar(50) null,
    streamMode         varchar(50) null,
    online             varchar(50) null,
    registerTimeMillis int          null,
    ip                 varchar(50) not null,
    port               int          not null,
    hostAddress        varchar(50) not null
);

create table device_channel
(
    channelId   varchar(50) not null,
    name        varchar(50) null,
    manufacture varchar(50) null,
    model       varchar(50) null,
    owner       varchar(50) null,
    civilCode   varchar(50) null,
    block       varchar(50) null,
    address     varchar(50) null,
    parentId    varchar(50) null,
    safetyWay   int          null,
    registerWay int          null,
    certNum     varchar(50) null,
    certifiable int          null,
    errCode     int          null,
    endTime     varchar(50) null,
    secrecy     varchar(50) null,
    ipAddress   varchar(50) null,
    port        int          null,
    password    varchar(50) null,
    PTZType     int          null,
    status      int          null,
    longitude   double       null,
    latitude    double       null,
    streamId    varchar(50) null,
    deviceId    varchar(50) not null,
    parental    varchar(50) null,
    hasAudio    tinyint(1)   null,
    primary key (channelId, deviceId)
);

create table device_mobile_position
(
    deviceId       varchar(50) not null,
    deviceName     varchar(50) null,
    time           varchar(50) not null,
    longitude      double       not null,
    latitude       double       not null,
    altitude       double       null,
    speed          double       null,
    direction      double       null,
    reportSource   varchar(50) null,
    geodeticSystem varchar(50) null,
    cnLng          varchar(50) null,
    cnLat          varchar(50) null,
    primary key (deviceId, time)
);

create table gb_stream
(
    app        varchar(50) not null,
    stream     varchar(50) not null,
    gbId       varchar(50) not null,
    name       varchar(50) null,
    longitude  double       null,
    latitude   double       null,
    streamType varchar(50) null,
    status     int          null,
    primary key (app, stream, gbId)
);

create table parent_platform
(
    enable         int          null,
    name           varchar(50) null,
    serverGBId     varchar(50) not null
        primary key,
    serverGBDomain varchar(50) null,
    serverIP       varchar(50) null,
    serverPort     int          null,
    deviceGBId     varchar(50) not null,
    deviceIp       varchar(50) null,
    devicePort     varchar(50) null,
    username       varchar(50) null,
    password       varchar(50) null,
    expires        varchar(50) null,
    keepTimeout    varchar(50) null,
    transport      varchar(50) null,
    characterSet   varchar(50) null,
    ptz            int          null,
    rtcp           int          null,
    status         tinyint(1)   null
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
    platformId varchar(50) not null,
    app        varchar(50) not null,
    stream     varchar(50) not null,
    primary key (platformId, app, stream)
);

create table stream_proxy
(
    type           varchar(50) not null,
    app            varchar(50) not null,
    stream         varchar(50) not null,
    url            varchar(50) null,
    src_url        varchar(50) null,
    dst_url        blob         null,
    timeout_ms     int          null,
    ffmpeg_cmd_key varchar(50) null,
    rtp_type       varchar(50) null,
    enable_hls     tinyint(1)   null,
    enable_mp4     tinyint(1)   null,
    enable         tinyint(1)   not null,
    primary key (app, stream)
);

create table stream_push
(
    app              varchar(50) not null,
    stream           varchar(50) not null,
    totalReaderCount varchar(50) null,
    originType       int          null,
    originTypeStr    varchar(50) null,
    createStamp      int          null,
    aliveSecond      int          null,
    primary key (app, stream)
);

create table user
(
    id          int auto_increment
        primary key,
    username    varchar(50) not null,
    password    varchar(50) not null,
    roleId      int          not null,
    create_time varchar(50) not null
);

insert into user (username, password, roleId, create_time) values ('admin', '21232f297a57a5a743894a0e4a801fc3', '0', '2021-04-13 14:14:57');