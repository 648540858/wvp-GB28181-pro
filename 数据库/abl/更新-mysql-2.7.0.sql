alter table wvp_media_server
    add  type character varying(50) default 'zlm';

alter table wvp_media_server
    add flv_port integer;
alter table wvp_media_server
    add flv_ssl_port integer;
alter table wvp_media_server
    add ws_flv_port integer;
alter table wvp_media_server
    add ws_flv_ssl_port integer;
