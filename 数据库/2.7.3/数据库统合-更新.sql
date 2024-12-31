/*
* WVP RPC 调用
*/
alter table wvp_device add server_id character varying(50);
alter table wvp_media_server add server_id character varying(50);
alter table wvp_stream_proxy add server_id character varying(50);