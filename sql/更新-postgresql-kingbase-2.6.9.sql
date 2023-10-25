alter table wvp_device_channel
    rename "stream_id" to stream_id ;

alter table wvp_platform
    add auto_push_channel bool default false;

alter table wvp_stream_proxy
    add stream_key character varying(255);

create table wvp_cloud_record (
      id serial primary key,
      app character varying(255),
      stream character varying(255),
      call_id character varying(255),
      start_time int8,
      end_time int8,
      media_server_id character varying(50),
      file_name character varying(255),
      folder character varying(255),
      file_path character varying(255),
      collect bool default false,
      file_size int8,
      time_len int8,
      constraint uk_stream_push_app_stream_path unique (app, stream, file_path)
);

alter table wvp_media_server
    add record_path character varying(255);

alter table wvp_media_server
    add record_date integer default 7;


