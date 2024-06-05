create table wvp_jt_device (
                               id serial primary key,
                               device_id character varying(50),
                               phone_number character varying(50) not null,
                               province_id character varying(50),
                               province_text character varying(100),
                               city_id character varying(50),
                               city_text character varying(100),
                               maker_id character varying(50),
                               device_model character varying(50),
                               plate_color character varying(50),
                               plate_no character varying(50),
                               authentication_code character varying(255),
                               longitude double precision,
                               latitude double precision,
                               status bool default false,
                               register_time character varying(50) not null,
                               update_time character varying(50) not null,
                               create_time character varying(50) not null,
                               constraint uk_jt_device_id_device_id unique (id, terminal_id)
);
create table wvp_jt_channel (
                                id serial primary key,
                                channel_id integer,
                                device_id integer,
                                name character varying(255),
                                update_time character varying(50) not null,
                                create_time character varying(50) not null,
                                constraint uk_jt_device_id_device_id unique (id, terminal_id)
);