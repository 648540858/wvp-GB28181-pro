alter table wvp_media_server
    add transcode_suffix character varying(255);

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

create table wvp_user_api_key (
                                  id serial primary key ,
                                  user_id bigint,
                                  app character varying(255) ,
                                  api_key text,
                                  expired_at bigint,
                                  remark character varying(255),
                                  enable bool default true,
                                  create_time character varying(50),
                                  update_time character varying(50)
);