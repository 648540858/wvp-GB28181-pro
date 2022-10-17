alter table wvp.media_server
    drop column streamNoneReaderDelayMS;

alter table stream_proxy
    add enable_disable_none_reader bit(1) default null;

alter table device
    add mediaServerId varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'auto';
