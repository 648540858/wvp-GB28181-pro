alter table wvp_media_server
    add transcode_suffix character varying(255);

alter table wvp_platform_catalog
    add civil_code_for_channel character varying(50);
