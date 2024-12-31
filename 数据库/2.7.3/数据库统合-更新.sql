/*
* WVP RPC 调用
*/
alter table wvp_device add server_id character varying(50);
alter table wvp_media_server add server_id character varying(50);
alter table wvp_stream_proxy add server_id character varying(50);
alter table wvp_cloud_record add server_id character varying(50);

update wvp_device set server_id = "你服务的ID";
update wvp_media_server set server_id = "你服务的ID";
update wvp_stream_proxy set server_id = "你服务的ID";
update wvp_cloud_record set server_id = "你服务的ID";