-- auto-generated definition
create schema wvp collate utf8_bin;

create table device
(
    deviceId           varchar(255) not null
        primary key,
    name               varchar(255) null,
    manufacturer       varchar(255) null,
    model              varchar(255) null,
    firmware           varchar(255) null,
    transport          varchar(255) null,
    streamMode         varchar(255) null,
    online             varchar(255) null,
    registerTimeMillis int          null,
    ip                 varchar(255) not null,
    port               int          not null,
    hostAddress        varchar(255) not null
);

create table device_channel
(
    channelId   varchar(255) not null,
    name        varchar(255) null,
    manufacture varchar(255) null,
    model       varchar(255) null,
    owner       varchar(255) null,
    civilCode   varchar(255) null,
    block       varchar(255) null,
    address     varchar(255) null,
    parentId    varchar(255) null,
    safetyWay   int          null,
    registerWay int          null,
    certNum     varchar(255) null,
    certifiable int          null,
    errCode     int          null,
    endTime     varchar(255) null,
    secrecy     varchar(255) null,
    ipAddress   varchar(255) null,
    port        int          null,
    password    varchar(255) null,
    PTZType     int          null,
    status      int          null,
    longitude   double       null,
    latitude    double       null,
    streamId    varchar(255) null,
    deviceId    varchar(255) not null,
    parental    varchar(255) null,
    hasAudio    tinyint(1)   null,
    primary key (channelId, deviceId)
);

create table device_mobile_position
(
    deviceId       varchar(255) not null,
    deviceName     varchar(255) null,
    time           varchar(255) not null,
    longitude      double       not null,
    latitude       double       not null,
    altitude       double       null,
    speed          double       null,
    direction      double       null,
    reportSource   varchar(255) null,
    geodeticSystem varchar(255) null,
    cnLng          varchar(255) null,
    cnLat          varchar(255) null,
    primary key (deviceId, time)
);

create table gb_stream
(
    app        varchar(255) not null,
    stream     varchar(255) not null,
    gbId       varchar(255) not null,
    name       varchar(255) null,
    longitude  double       null,
    latitude   double       null,
    streamType varchar(255) null,
    status     int          null,
    primary key (app, stream, gbId)
);

create table parent_platform
(
    enable         int          null,
    name           varchar(255) null,
    serverGBId     varchar(255) not null
        primary key,
    serverGBDomain varchar(255) null,
    serverIP       varchar(255) null,
    serverPort     int          null,
    deviceGBId     varchar(255) not null,
    deviceIp       varchar(255) null,
    devicePort     varchar(255) null,
    username       varchar(255) null,
    password       varchar(255) null,
    expires        varchar(255) null,
    keepTimeout    varchar(255) null,
    transport      varchar(255) null,
    characterSet   varchar(255) null,
    ptz            int          null,
    rtcp           int          null,
    status         tinyint(1)   null
);

create table platform_gb_channel
(
    channelId          varchar(255) not null,
    deviceId           varchar(255) not null,
    platformId         varchar(255) not null,
    deviceAndChannelId varchar(255) not null,
    primary key (deviceAndChannelId, platformId)
);

create table platform_gb_stream
(
    platformId varchar(255) not null,
    app        varchar(255) not null,
    stream     varchar(255) not null,
    primary key (platformId, app, stream)
);

create table stream_proxy
(
    type           varchar(255) not null,
    app            varchar(255) not null,
    stream         varchar(255) not null,
    url            varchar(255) null,
    src_url        varchar(255) null,
    dst_url        blob         null,
    timeout_ms     int          null,
    ffmpeg_cmd_key varchar(255) null,
    rtp_type       varchar(255) null,
    enable_hls     tinyint(1)   null,
    enable_mp4     tinyint(1)   null,
    enable         tinyint(1)   not null,
    primary key (app, stream)
);

create table stream_push
(
    app              varchar(255) not null,
    stream           varchar(255) not null,
    totalReaderCount varchar(255) null,
    originType       int          null,
    originTypeStr    varchar(255) null,
    createStamp      int          null,
    aliveSecond      int          null,
    primary key (app, stream)
);

create table user
(
    id          int auto_increment
        primary key,
    username    varchar(255) not null,
    password    varchar(255) not null,
    roleId      int          not null,
    create_time varchar(255) not null
);

insert into user (username, password, roleId, create_time) values ('admin', '21232f297a57a5a743894a0e4a801fc3', '0', '2021-04-13 14:14:57');