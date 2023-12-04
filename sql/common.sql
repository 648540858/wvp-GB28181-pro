CREATE TABLE `wvp_common_channel`
(
    `common_gb_id`                    bigint unsigned NOT NULL AUTO_INCREMENT,
    `common_gb_device_id`             varchar(50)  NOT NULL,
    `common_gb_name`      varchar(255) DEFAULT NULL,
    `common_gb_manufacturer`          varchar(255) DEFAULT NULL,
    `common_gb_model`                 varchar(255) DEFAULT NULL,
    `common_gb_owner`                 varchar(255) DEFAULT NULL,
    `common_gb_civilCode` varchar(50)  DEFAULT NULL,
    `common_gb_block`                 varchar(255) DEFAULT NULL,
    `common_gb_address`               varchar(255) DEFAULT NULL,
    `common_gb_parental`              integer,
    `common_gb_parent_id`             varchar(50)  DEFAULT NULL,
    `common_gb_safety_way`            integer,
    `common_gb_register_way`          integer,
    `common_gb_cert_num`              varchar(255) DEFAULT NULL,
    `common_gb_certifiable`           integer,
    `common_gb_err_code`              integer,
    `common_gb_end_time`              varchar(50)  DEFAULT NULL,
    `common_gb_secrecy`               integer,
    `common_gb_ip_address`            varchar(50)  DEFAULT NULL,
    `common_gb_port`                  integer,
    `common_gb_password`              varchar(50)  DEFAULT NULL,
    `common_gb_status`    bool         default false,
    `common_gb_longitude` double,
    `common_gb_latitude` double,
    `common_gb_ptz_type`              integer,
    `common_gb_position_type`         integer,
    `common_gb_room_type`             integer,
    `common_gb_use_type`              integer,
    `common_gb_supply_light_type`     integer,
    `common_gb_direction_type`        integer,
    `common_gb_resolution`            varchar(255) DEFAULT NULL,
    `common_gb_business_group_id`     varchar(255) DEFAULT NULL,
    `common_gb_download_speed`        varchar(255) DEFAULT NULL,
    `common_gb_svc_time_support_mode` integer,
    `type`                            varchar(255) NOT NULL,
    `update_time`          varchar(50) NOT NULL,
    `create_time`          varchar(50) NOT NULL,
    PRIMARY KEY (`common_gb_id`),
    UNIQUE KEY `common_gb_device_id` (`common_gb_device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `wvp_common_group`
(
    `common_group_id`           bigint unsigned NOT NULL AUTO_INCREMENT,
    `common_group_device_id`    varchar(50)  NOT NULL,
    `common_group_name`         varchar(255) NOT NULL,
    `common_group_parent_id`    varchar(50)  DEFAULT NULL,
    `common_group_top_id`       varchar(50)  DEFAULT NULL,
    `common_group_create_time`  varchar(50)  NOT NULL,
    `common_group_update_time`  varchar(50)  NOT NULL,
    PRIMARY KEY (`common_group_id`),
    UNIQUE KEY `common_group_device_id` (`common_group_device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `wvp_common_region`
(
    `common_region_id`        bigint unsigned NOT NULL AUTO_INCREMENT,
    `common_region_device_id` varchar(50)  NOT NULL,
    `common_region_name`      varchar(255) NOT NULL,
    `common_region_parent_id` varchar(50) DEFAULT NULL,
    `common_region_create_time` varchar(50) NOT NULL,
    `common_region_update_time` varchar(50) NOT NULL,
    PRIMARY KEY (`common_region_id`),
    UNIQUE KEY `common_region_device_id` (`common_region_device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `wvp_common_channel_platform`
(
    `id`                   bigint unsigned NOT NULL AUTO_INCREMENT,
    `platform_id`          bigint unsigned NOT NULL,
    `common_gb_channel_id` bigint unsigned NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `id` (`id`),
    UNIQUE KEY `uk_platform_id_common_gb_channel_id` (`platform_id`,`common_gb_channel_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


drop table wvp_platform_gb_stream;
drop table wvp_platform_gb_channel;
drop table wvp_platform_catalog;
drop table wvp_platform_gb_channel;
drop table wvp_resources_tree;




