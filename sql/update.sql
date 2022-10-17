alter table wvp.media_server
    drop column streamNoneReaderDelayMS;

alter table stream_proxy
    add enable_disable_none_reader bit(1) default null;
