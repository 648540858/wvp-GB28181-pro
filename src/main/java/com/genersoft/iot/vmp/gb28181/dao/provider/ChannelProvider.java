package com.genersoft.iot.vmp.gb28181.dao.provider;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.web.custom.bean.CameraGroup;
import com.genersoft.iot.vmp.web.custom.bean.Point;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ChannelProvider {

    public final static String BASE_SQL = "select\n" +
            "    id as gb_id,\n" +
            "    data_type,\n" +
            "    data_device_id,\n" +
            "    create_time,\n" +
            "    update_time,\n" +
            "    stream_id,\n" +
            "    record_plan_id,\n" +
            "    enable_broadcast,\n" +
            "    map_level,\n" +
            "    coalesce(gb_device_id, device_id) as gb_device_id,\n" +
            "    coalesce(gb_name, name) as gb_name,\n" +
            "    coalesce(gb_manufacturer, manufacturer) as gb_manufacturer,\n" +
            "    coalesce(gb_model, model) as gb_model,\n" +
            "    coalesce(gb_owner, owner) as gb_owner,\n" +
            "    coalesce(gb_civil_code, civil_code) as gb_civil_code,\n" +
            "    coalesce(gb_block, block) as gb_block,\n" +
            "    coalesce(gb_address, address) as gb_address,\n" +
            "    coalesce(gb_parental, parental) as gb_parental,\n" +
            "    coalesce(gb_parent_id, parent_id) as gb_parent_id,\n" +
            "    coalesce(gb_safety_way, safety_way) as gb_safety_way,\n" +
            "    coalesce(gb_register_way, register_way) as gb_register_way,\n" +
            "    coalesce(gb_cert_num, cert_num) as gb_cert_num,\n" +
            "    coalesce(gb_certifiable, certifiable) as gb_certifiable,\n" +
            "    coalesce(gb_err_code, err_code) as gb_err_code,\n" +
            "    coalesce(gb_end_time, end_time) as gb_end_time,\n" +
            "    coalesce(gb_secrecy, secrecy) as gb_secrecy,\n" +
            "    coalesce(gb_ip_address, ip_address) as gb_ip_address,\n" +
            "    coalesce(gb_port, port) as gb_port,\n" +
            "    coalesce(gb_password, password) as gb_password,\n" +
            "    coalesce(gb_status, status) as gb_status,\n" +
            "    coalesce(gb_longitude, longitude) as gb_longitude,\n" +
            "    coalesce(gb_latitude, latitude) as gb_latitude,\n" +
            "    coalesce(gb_ptz_type, ptz_type) as gb_ptz_type,\n" +
            "    coalesce(gb_position_type, position_type) as gb_position_type,\n" +
            "    coalesce(gb_room_type, room_type) as gb_room_type,\n" +
            "    coalesce(gb_use_type, use_type) as gb_use_type,\n" +
            "    coalesce(gb_supply_light_type, supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(gb_direction_type, direction_type) as gb_direction_type,\n" +
            "    coalesce(gb_resolution, resolution) as gb_resolution,\n" +
            "    coalesce(gb_business_group_id, business_group_id) as gb_business_group_id,\n" +
            "    coalesce(gb_download_speed, download_speed) as gb_download_speed,\n" +
            "    coalesce(gb_svc_space_support_mod, svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(gb_svc_time_support_mode,svc_time_support_mode) as gb_svc_time_support_mode\n" +
            " from wvp_device_channel\n"
            ;

    public final static String BASE_SQL_TABLE_NAME = "select\n" +
            "    wdc.id as gb_id,\n" +
            "    wdc.data_type,\n" +
            "    wdc.data_device_id,\n" +
            "    wdc.create_time,\n" +
            "    wdc.update_time,\n" +
            "    wdc.stream_id,\n" +
            "    wdc.record_plan_id,\n" +
            "    wdc.enable_broadcast,\n" +
            "    coalesce(wdc.gb_device_id,  wdc.device_id) as gb_device_id,\n" +
            "    coalesce(wdc.gb_name,  wdc.name) as gb_name,\n" +
            "    coalesce(wdc.gb_manufacturer,  wdc.manufacturer) as gb_manufacturer,\n" +
            "    coalesce(wdc.gb_model,  wdc.model) as gb_model,\n" +
            "    coalesce(wdc.gb_owner,  wdc.owner) as gb_owner,\n" +
            "    coalesce(wdc.gb_civil_code,  wdc.civil_code) as gb_civil_code,\n" +
            "    coalesce(wdc.gb_block,  wdc.block) as gb_block,\n" +
            "    coalesce(wdc.gb_address,  wdc.address) as gb_address,\n" +
            "    coalesce(wdc.gb_parental,  wdc.parental) as gb_parental,\n" +
            "    coalesce(wdc.gb_parent_id,  wdc.parent_id) as gb_parent_id,\n" +
            "    coalesce(wdc.gb_safety_way,  wdc.safety_way) as gb_safety_way,\n" +
            "    coalesce(wdc.gb_register_way,  wdc.register_way) as gb_register_way,\n" +
            "    coalesce(wdc.gb_cert_num,  wdc.cert_num) as gb_cert_num,\n" +
            "    coalesce(wdc.gb_certifiable,  wdc.certifiable) as gb_certifiable,\n" +
            "    coalesce(wdc.gb_err_code,  wdc.err_code) as gb_err_code,\n" +
            "    coalesce(wdc.gb_end_time,  wdc.end_time) as gb_end_time,\n" +
            "    coalesce(wdc.gb_secrecy,  wdc.secrecy) as gb_secrecy,\n" +
            "    coalesce(wdc.gb_ip_address,  wdc.ip_address) as gb_ip_address,\n" +
            "    coalesce(wdc.gb_port,  wdc.port) as gb_port,\n" +
            "    coalesce(wdc.gb_password,  wdc.password) as gb_password,\n" +
            "    coalesce(wdc.gb_status,  wdc.status) as gb_status,\n" +
            "    coalesce(wdc.gb_longitude,  wdc.longitude) as gb_longitude,\n" +
            "    coalesce(wdc.gb_latitude,  wdc.latitude) as gb_latitude,\n" +
            "    coalesce(wdc.gb_ptz_type,  wdc.ptz_type) as gb_ptz_type,\n" +
            "    coalesce(wdc.gb_position_type,  wdc.position_type) as gb_position_type,\n" +
            "    coalesce(wdc.gb_room_type,  wdc.room_type) as gb_room_type,\n" +
            "    coalesce(wdc.gb_use_type,  wdc.use_type) as gb_use_type,\n" +
            "    coalesce(wdc.gb_supply_light_type,  wdc.supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(wdc.gb_direction_type,  wdc.direction_type) as gb_direction_type,\n" +
            "    coalesce(wdc.gb_resolution,  wdc.resolution) as gb_resolution,\n" +
            "    coalesce(wdc.gb_business_group_id,  wdc.business_group_id) as gb_business_group_id,\n" +
            "    coalesce(wdc.gb_download_speed,  wdc.download_speed) as gb_download_speed,\n" +
            "    coalesce(wdc.gb_svc_space_support_mod,  wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(wdc.gb_svc_time_support_mode,  wdc.svc_time_support_mode) as gb_svc_time_support_mode\n" +
            " from wvp_device_channel wdc\n"
            ;

    private final static String BASE_SQL_FOR_PLATFORM =
            "select\n" +
            "    wdc.id as gb_id,\n" +
            "    wdc.data_type,\n" +
            "    wdc.data_device_id,\n" +
            "    wdc.create_time,\n" +
            "    wdc.update_time,\n" +
            "    wdc.enable_broadcast,\n" +
            "    coalesce(wpgc.custom_device_id, wdc.gb_device_id, wdc.device_id) as gb_device_id,\n" +
            "    coalesce(wpgc.custom_name, wdc.gb_name, wdc.name) as gb_name,\n" +
            "    coalesce(wpgc.custom_manufacturer, wdc.gb_manufacturer, wdc.manufacturer) as gb_manufacturer,\n" +
            "    coalesce(wpgc.custom_model, wdc.gb_model, wdc.model) as gb_model,\n" +
            "    coalesce(wpgc.custom_owner, wdc.gb_owner, wdc.owner) as gb_owner,\n" +
            "    coalesce(wpgc.custom_civil_code, wdc.gb_civil_code, wdc.civil_code) as gb_civil_code,\n" +
            "    coalesce(wpgc.custom_block, wdc.gb_block, wdc.block) as gb_block,\n" +
            "    coalesce(wpgc.custom_address, wdc.gb_address, wdc.address) as gb_address,\n" +
            "    coalesce(wpgc.custom_parental, wdc.gb_parental, wdc.parental) as gb_parental,\n" +
            "    coalesce(wpgc.custom_parent_id, wdc.gb_parent_id, wdc.parent_id) as gb_parent_id,\n" +
            "    coalesce(wpgc.custom_safety_way, wdc.gb_safety_way, wdc.safety_way) as gb_safety_way,\n" +
            "    coalesce(wpgc.custom_register_way, wdc.gb_register_way, wdc.register_way) as gb_register_way,\n" +
            "    coalesce(wpgc.custom_cert_num, wdc.gb_cert_num, wdc.cert_num) as gb_cert_num,\n" +
            "    coalesce(wpgc.custom_certifiable, wdc.gb_certifiable, wdc.certifiable) as gb_certifiable,\n" +
            "    coalesce(wpgc.custom_err_code, wdc.gb_err_code, wdc.err_code) as gb_err_code,\n" +
            "    coalesce(wpgc.custom_end_time, wdc.gb_end_time, wdc.end_time) as gb_end_time,\n" +
            "    coalesce(wpgc.custom_secrecy, wdc.gb_secrecy, wdc.secrecy) as gb_secrecy,\n" +
            "    coalesce(wpgc.custom_ip_address, wdc.gb_ip_address, wdc.ip_address) as gb_ip_address,\n" +
            "    coalesce(wpgc.custom_port, wdc.gb_port, wdc.port) as gb_port,\n" +
            "    coalesce(wpgc.custom_password, wdc.gb_password, wdc.password) as gb_password,\n" +
            "    coalesce(wpgc.custom_status, wdc.gb_status, wdc.status) as gb_status,\n" +
            "    coalesce(wpgc.custom_longitude, wdc.gb_longitude, wdc.longitude) as gb_longitude,\n" +
            "    coalesce(wpgc.custom_latitude, wdc.gb_latitude, wdc.latitude) as gb_latitude,\n" +
            "    coalesce(wpgc.custom_ptz_type, wdc.gb_ptz_type, wdc.ptz_type) as gb_ptz_type,\n" +
            "    coalesce(wpgc.custom_position_type, wdc.gb_position_type, wdc.position_type) as gb_position_type,\n" +
            "    coalesce(wpgc.custom_room_type, wdc.gb_room_type, wdc.room_type) as gb_room_type,\n" +
            "    coalesce(wpgc.custom_use_type, wdc.gb_use_type, wdc.use_type) as gb_use_type,\n" +
            "    coalesce(wpgc.custom_supply_light_type, wdc.gb_supply_light_type, wdc.supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(wpgc.custom_direction_type, wdc.gb_direction_type, wdc.direction_type) as gb_direction_type,\n" +
            "    coalesce(wpgc.custom_resolution, wdc.gb_resolution, wdc.resolution) as gb_resolution,\n" +
            "    coalesce(wpgc.custom_business_group_id, wdc.gb_business_group_id, wdc.business_group_id) as gb_business_group_id,\n" +
            "    coalesce(wpgc.custom_download_speed, wdc.gb_download_speed, wdc.download_speed) as gb_download_speed,\n" +
            "    coalesce(wpgc.custom_svc_space_support_mod, wdc.gb_svc_space_support_mod, wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(wpgc.custom_svc_time_support_mode, wdc.gb_svc_time_support_mode, wdc.svc_time_support_mode) as gb_svc_time_support_mode\n" +
            "    from wvp_device_channel wdc" +
                    " left join wvp_platform_channel wpgc on wdc.id = wpgc.device_channel_id"
            ;

    private final static String BASE_SQL_FOR_CAMERA_DEVICE =
            "select\n" +
                    "    wdc.id as gb_id,\n" +
                    "    wdc.data_type,\n" +
                    "    wdc.data_device_id,\n" +
                    "    wdc.create_time,\n" +
                    "    wdc.update_time,\n" +
                    "    wdc.stream_id,\n" +
                    "    wdc.record_plan_id,\n" +
                    "    wdc.enable_broadcast,\n" +
                    "    wd.device_id as deviceCode,\n" +
                    "    wcg.alias as groupAlias,\n" +
                    "    wcg2.alias as topGroupGAlias,\n" +
                    "    coalesce(wdc.gb_device_id,  wdc.device_id) as gb_device_id,\n" +
                    "    coalesce(wdc.gb_name,  wdc.name) as gb_name,\n" +
                    "    coalesce(wdc.gb_manufacturer,  wdc.manufacturer) as gb_manufacturer,\n" +
                    "    coalesce(wdc.gb_model,  wdc.model) as gb_model,\n" +
                    "    coalesce(wdc.gb_owner,  wdc.owner) as gb_owner,\n" +
                    "    coalesce(wdc.gb_civil_code,  wdc.civil_code) as gb_civil_code,\n" +
                    "    coalesce(wdc.gb_block,  wdc.block) as gb_block,\n" +
                    "    coalesce(wdc.gb_address,  wdc.address) as gb_address,\n" +
                    "    coalesce(wdc.gb_parental,  wdc.parental) as gb_parental,\n" +
                    "    coalesce(wdc.gb_parent_id,  wdc.parent_id) as gb_parent_id,\n" +
                    "    coalesce(wdc.gb_safety_way,  wdc.safety_way) as gb_safety_way,\n" +
                    "    coalesce(wdc.gb_register_way,  wdc.register_way) as gb_register_way,\n" +
                    "    coalesce(wdc.gb_cert_num,  wdc.cert_num) as gb_cert_num,\n" +
                    "    coalesce(wdc.gb_certifiable,  wdc.certifiable) as gb_certifiable,\n" +
                    "    coalesce(wdc.gb_err_code,  wdc.err_code) as gb_err_code,\n" +
                    "    coalesce(wdc.gb_end_time,  wdc.end_time) as gb_end_time,\n" +
                    "    coalesce(wdc.gb_secrecy,  wdc.secrecy) as gb_secrecy,\n" +
                    "    coalesce(wdc.gb_ip_address,  wdc.ip_address) as gb_ip_address,\n" +
                    "    coalesce(wdc.gb_port,  wdc.port) as gb_port,\n" +
                    "    coalesce(wdc.gb_password,  wdc.password) as gb_password,\n" +
                    "    coalesce(wdc.gb_status,  wdc.status) as gb_status,\n" +
                    "    coalesce(wdc.gb_longitude,  wdc.longitude) as gb_longitude,\n" +
                    "    coalesce(wdc.gb_latitude,  wdc.latitude) as gb_latitude,\n" +
                    "    coalesce(wdc.gb_ptz_type,  wdc.ptz_type) as gb_ptz_type,\n" +
                    "    coalesce(wdc.gb_position_type,  wdc.position_type) as gb_position_type,\n" +
                    "    coalesce(wdc.gb_room_type,  wdc.room_type) as gb_room_type,\n" +
                    "    coalesce(wdc.gb_use_type,  wdc.use_type) as gb_use_type,\n" +
                    "    coalesce(wdc.gb_supply_light_type,  wdc.supply_light_type) as gb_supply_light_type,\n" +
                    "    coalesce(wdc.gb_direction_type,  wdc.direction_type) as gb_direction_type,\n" +
                    "    coalesce(wdc.gb_resolution,  wdc.resolution) as gb_resolution,\n" +
                    "    coalesce(wdc.gb_business_group_id,  wdc.business_group_id) as gb_business_group_id,\n" +
                    "    coalesce(wdc.gb_download_speed,  wdc.download_speed) as gb_download_speed,\n" +
                    "    coalesce(wdc.gb_svc_space_support_mod,  wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
                    "    coalesce(wdc.gb_svc_time_support_mode,  wdc.svc_time_support_mode) as gb_svc_time_support_mode\n" +
                    " from wvp_device_channel wdc\n" +
                    " left join wvp_device wd on wdc.data_type = 1 AND wd.id = wdc.data_device_id" +
                    " left join wvp_common_group wcg on wcg.device_id = coalesce(wdc.gb_parent_id,  wdc.parent_id)" +
                    " left join wvp_common_group wcg2 on wcg2.device_id = wcg.business_group"
            ;

    public String queryByDeviceId(Map<String, Object> params ){
        return BASE_SQL + " where channel_type = 0 and coalesce(gb_device_id, device_id) = #{gbDeviceId}";
    }

    public String queryById(Map<String, Object> params ){
        return BASE_SQL + " where channel_type = 0 and id = #{gbId}";
    }

    public String queryByDataId(Map<String, Object> params ){
        return BASE_SQL + " where channel_type = 0 and data_type = #{dataType} and data_device_id = #{dataDeviceId}";
    }

    public String queryListByCivilCode(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append(" where channel_type = 0 ");
        if (params.get("query") != null) {
            sqlBuild.append(" AND (coalesce(gb_device_id, device_id) LIKE concat('%',#{query},'%') escape '/'" +
                    " OR coalesce(gb_name, name) LIKE concat('%',#{query},'%') escape '/' )")
            ;
        }
        if (params.get("online") != null && (Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(gb_status, status) = 'ON'");
        }
        if (params.get("online") != null && !(Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(gb_status, status) = 'OFF'");
        }
        if (params.get("civilCode") != null) {
            sqlBuild.append(" AND coalesce(gb_civil_code, civil_code) = #{civilCode}");
        }else {
            sqlBuild.append(" AND coalesce(gb_civil_code, civil_code) is null");
        }
        if (params.get("dataType") != null) {
            sqlBuild.append(" AND data_type = #{dataType}");
        }
        return sqlBuild.toString();
    }

    public String queryListByParentId(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append(" where channel_type = 0 ");
        if (params.get("query") != null) {
            sqlBuild.append(" AND (coalesce(gb_device_id, device_id) LIKE concat('%',#{query},'%') escape '/'" +
                    " OR coalesce(gb_name, name) LIKE concat('%',#{query},'%') escape '/' )")
            ;
        }
        if (params.get("online") != null && (Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(gb_status, status) = 'ON'");
        }
        if (params.get("online") != null && !(Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(gb_status, status) = 'OFF'");
        }
        if (params.get("groupDeviceId") != null) {
            sqlBuild.append(" AND coalesce(gb_parent_id, parent_id) = #{groupDeviceId}");
        }else {
            sqlBuild.append(" AND coalesce(gb_parent_id, parent_id) is null");
        }
        if (params.get("dataType") != null) {
            sqlBuild.append(" AND data_type = #{dataType}");
        }
        return sqlBuild.toString();
    }

    public String queryList(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append(" where channel_type = 0 ");
        if (params.get("query") != null) {
            sqlBuild.append(" AND (coalesce(gb_device_id, device_id) LIKE concat('%',#{query},'%') escape '/'" +
                    " OR coalesce(gb_name, name) LIKE concat('%',#{query},'%') escape '/' )")
            ;
        }
        if (params.get("online") != null && (Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(gb_status, status) = 'ON'");
        }
        if (params.get("online") != null && !(Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(gb_status, status) = 'OFF'");
        }
        if (params.get("hasRecordPlan") != null && (Boolean)params.get("hasRecordPlan")) {
            sqlBuild.append(" AND record_plan_id > 0");
        }
        if (params.get("dataType") != null) {
            sqlBuild.append(" AND data_type = #{dataType}");
        }
        if (params.get("civilCode") != null) {
            sqlBuild.append(" AND coalesce(gb_civil_code, civil_code) = #{civilCode}");
        }
        if (params.get("parentDeviceId") != null) {
            sqlBuild.append(" AND coalesce(gb_parent_id, parent_id) =  #{parentDeviceId}");
        }
        return sqlBuild.toString();
    }

    public String queryInListByStatus(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append("where channel_type = 0 and gb_status=#{status} and id in ( ");

        List<CommonGBChannel> commonGBChannelList = (List<CommonGBChannel>)params.get("commonGBChannelList");
        boolean first = true;
        for (CommonGBChannel channel : commonGBChannelList) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append(channel.getGbId());
            first = false;
        }
        sqlBuild.append(" )");
        return sqlBuild.toString() ;
    }

    public String queryByIds(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append("where channel_type = 0 and id in ( ");

        Collection<Integer> ids = (Collection<Integer>)params.get("ids");
        boolean first = true;
        for (Integer id : ids) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append(id);
            first = false;
        }
        sqlBuild.append(" )");
        return sqlBuild.toString() ;
    }

    public String queryByGbDeviceIds(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append("where channel_type = 0 and data_type = #{dataType} and data_device_id in ( ");

        Collection<Integer> ids = (Collection<Integer>)params.get("deviceIds");
        boolean first = true;
        for (Integer id : ids) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append(id);
            first = false;
        }
        sqlBuild.append(" )");
        return sqlBuild.toString() ;
    }

    public String queryByDeviceIds(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append("where channel_type = 0 and id in ( ");

        Collection<Integer> ids = (Collection<Integer>)params.get("deviceIds");
        boolean first = true;
        for (Integer id : ids) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append(id);
            first = false;
        }
        sqlBuild.append(" )");
        return sqlBuild.toString() ;
    }

    public String queryByIdsOrCivilCode(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append("where channel_type = 0 and ");
        if (params.get("civilCode") != null) {
            sqlBuild.append(" coalesce(gb_civil_code, civil_code) = #{civilCode} ");
            if (params.get("ids") != null) {
                sqlBuild.append(" OR ");
            }
        }
        if (params.get("ids") != null) {
            sqlBuild.append(" id in ( ");
            Collection<Integer> ids = (Collection<Integer>)params.get("ids");
            boolean first = true;
            for (Integer id : ids) {
                if (!first) {
                    sqlBuild.append(",");
                }
                sqlBuild.append(id);
                first = false;
            }
            sqlBuild.append(" )");
        }
        return sqlBuild.toString() ;
    }

    public String queryByCivilCode(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append("where channel_type = 0 and coalesce(gb_civil_code, civil_code) = #{civilCode} ");
        return sqlBuild.toString();
    }

    public String queryByBusinessGroup(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append("where channel_type = 0 and coalesce(gb_business_group_id, business_group_id) = #{businessGroup} ");
        return sqlBuild.toString() ;
    }

    public String queryByParentId(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append("where channel_type = 0 and gb_parent_id = #{parentId} ");
        return sqlBuild.toString() ;
    }

    public String queryByGroupList(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);

        sqlBuild.append(" where channel_type = 0 and gb_parent_id in ( ");
        Collection<Group> ids = (Collection<Group>)params.get("groupList");
        boolean first = true;
        for (Group group : ids) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append(group.getDeviceId());
            first = false;
        }
        sqlBuild.append(" )");

        return sqlBuild.toString() ;
    }

    public String queryListByStreamPushList(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);

        sqlBuild.append(" where channel_type = 0 and data_type = #{dataType} and data_device_id in ( ");
        Collection<StreamPush> ids = (Collection<StreamPush>)params.get("streamPushList");
        boolean first = true;
        for (StreamPush streamPush : ids) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append(streamPush.getId());
            first = false;
        }
        sqlBuild.append(" )");

        return sqlBuild.toString() ;
    }

    public String queryWithPlatform(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_PLATFORM);
        sqlBuild.append(" where wpgc.platform_id = #{platformId}");
        return sqlBuild.toString() ;
    }

    public String queryShareChannelByParentId(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_PLATFORM);
        sqlBuild.append(" where wpgc.platform_id = #{platformId} and coalesce(wpgc.custom_parent_id, wdc.gb_parent_id, wdc.parent_id) = #{parentId}");
        return sqlBuild.toString() ;
    }

    public String queryShareChannelByCivilCode(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_PLATFORM);
        sqlBuild.append(" where wpgc.platform_id = #{platformId} and coalesce(wpgc.custom_civil_code, wdc.gb_civil_code, wdc.civil_code) = #{civilCode}");
        return sqlBuild.toString() ;
    }

    public String queryListByCivilCodeForUnusual(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_TABLE_NAME);
        sqlBuild.append(" left join (select wcr.device_id from wvp_common_region wcr) temp on temp.device_id = coalesce(wdc.gb_civil_code, wdc.civil_code)" +
                " where coalesce(wdc.gb_civil_code, wdc.civil_code) is not null and temp.device_id is null ");
        sqlBuild.append(" AND wdc.channel_type = 0 ");
        if (params.get("query") != null) {
            sqlBuild.append(" AND (coalesce(wdc.gb_device_id, wdc.device_id) LIKE concat('%',#{query},'%') escape '/'" +
                    " OR coalesce(wdc.gb_name, wdc.name) LIKE concat('%',#{query},'%') escape '/' )")
            ;
        }
        if (params.get("online") != null && (Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(wdc.gb_status, wdc.status) = 'ON'");
        }
        if (params.get("online") != null && !(Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(wdc.gb_status, wdc.status) = 'OFF'");
        }
        if (params.get("dataType") != null) {
            sqlBuild.append(" AND wdc.data_type = #{dataType}");
        }
        return sqlBuild.toString();
    }

    public String queryListByParentForUnusual(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_TABLE_NAME);
        sqlBuild.append(" left join (select wcg.device_id from wvp_common_group wcg) temp on temp.device_id = coalesce(wdc.gb_parent_id, wdc.parent_id)" +
                " where coalesce(wdc.gb_parent_id, wdc.parent_id) is not null and temp.device_id is null ");
        sqlBuild.append(" AND wdc.channel_type = 0 ");
        if (params.get("query") != null) {
            sqlBuild.append(" AND (coalesce(wdc.gb_device_id, wdc.device_id) LIKE concat('%',#{query},'%') escape '/'" +
                    " OR coalesce(wdc.gb_name, wdc.name) LIKE concat('%',#{query},'%') escape '/' )")
            ;
        }
        if (params.get("online") != null && (Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(wdc.gb_status, wdc.status) = 'ON'");
        }
        if (params.get("online") != null && !(Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(wdc.gb_status, wdc.status) = 'OFF'");
        }
        if (params.get("dataType") != null) {
            sqlBuild.append(" AND wdc.data_type = #{dataType}");
        }
        return sqlBuild.toString();
    }

    public String queryCommonChannelByDeviceChannel(Map<String, Object> params ){
        return BASE_SQL +
                " where data_type=#{dataType} and data_device_id=#{dataDeviceId} AND device_id=#{deviceId}";
    }

    public String queryCameraChannelInBox(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_TABLE_NAME);
        sqlBuild.append(" where coalesce(wdc.gb_longitude, wdc.longitude) > #{minLon} " +
                "AND coalesce(wdc.gb_longitude, wdc.longitude) <= #{maxLon} " +
                "AND coalesce(wdc.gb_latitude,  wdc.latitude) > #{minLat} " +
                "AND coalesce(wdc.gb_latitude,  wdc.latitude) <= #{maxLat}");
        return sqlBuild.toString();
    }

    public String queryOldChanelListByChannels(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append(" where id in ( ");

        List<CommonGBChannel> channelList = (List<CommonGBChannel>)params.get("channelList");
        boolean first = true;
        for (CommonGBChannel channel : channelList) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append(channel.getGbId());
            first = false;
        }
        sqlBuild.append(" )");
        return sqlBuild.toString() ;
    }

    public String queryAllForUnusualCivilCode(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append("select wdc.id from wvp_device_channel wdc ");
        sqlBuild.append(" left join (select wcr.device_id from wvp_common_region wcr) temp on temp.device_id = coalesce(wdc.gb_civil_code, wdc.civil_code)" +
                " where coalesce(wdc.gb_civil_code, wdc.civil_code) is not null and temp.device_id is null ");
        sqlBuild.append(" AND wdc.channel_type = 0 ");
        return sqlBuild.toString();
    }

    public String queryAllForUnusualParent(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append("select wdc.id from wvp_device_channel wdc ");
        sqlBuild.append(" left join (select wcg.device_id from wvp_common_group wcg) temp on temp.device_id = coalesce(wdc.gb_parent_id, wdc.parent_id)" +
                " where coalesce(wdc.gb_parent_id, wdc.parent_id) is not null and temp.device_id is null ");
        sqlBuild.append(" AND wdc.channel_type = 0 ");
        return sqlBuild.toString();
    }

    public String queryOnlineListsByGbDeviceId(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_TABLE_NAME);
        sqlBuild.append(" where wdc.channel_type = 0 AND coalesce(wdc.gb_status, wdc.status) = 'ON' AND wdc.data_type = 1 AND data_device_id = #{deviceId}");
        return sqlBuild.toString();
    }

    public String queryListForSy(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_CAMERA_DEVICE);
        sqlBuild.append(" where wdc.channel_type = 0 AND wdc.data_type != 2 AND (wdc.gb_ptz_type is null ||  ( wdc.gb_ptz_type != 98 && wdc.gb_ptz_type != 99)) AND coalesce(wdc.gb_parent_id, wdc.parent_id) = #{groupDeviceId}");
        if (params.get("online") != null && (Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(wdc.gb_status, wdc.status) = 'ON'");
        }
        if (params.get("online") != null && !(Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(wdc.gb_status, wdc.status) = 'OFF'");
        }
        sqlBuild.append(" order by coalesce(wdc.gb_status, wdc.status) desc");

        return sqlBuild.toString();
    }

    public String queryListWithChildForSy(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_CAMERA_DEVICE);
        sqlBuild.append(" where wdc.channel_type = 0 AND wdc.data_type != 2 AND (wdc.gb_ptz_type is null ||  ( wdc.gb_ptz_type != 98 && wdc.gb_ptz_type != 99)) ");


        List<CameraGroup> groupList = (List<CameraGroup>)params.get("groupList");
        if (groupList != null && !groupList.isEmpty()) {
            sqlBuild.append(" AND coalesce(wdc.gb_parent_id, wdc.parent_id) in (");
            boolean first = true;
            for (CameraGroup group : groupList) {
                if (!first) {
                    sqlBuild.append(",");
                }
                sqlBuild.append("'" + group.getDeviceId() + "'");
                first = false;
            }
            sqlBuild.append(" )");
        }
        if (params.get("query") != null) {
            sqlBuild.append(" AND (coalesce(wdc.gb_device_id, wdc.device_id) LIKE concat('%',#{query},'%') escape '/'" +
                    " OR coalesce(wdc.gb_name, wdc.name) LIKE concat('%',#{query},'%') escape '/' )")
            ;
        }

        if (params.get("online") != null && (Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(wdc.gb_status, status) = 'ON'");
        }
        if (params.get("online") != null && !(Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(wdc.gb_status, status) = 'OFF'");
        }

        if (params.get("sortName") != null) {
            StringBuilder sqlBuildForSort = new StringBuilder();
            sqlBuildForSort.append("select * from ( ");
            sqlBuildForSort.append(sqlBuild);
            sqlBuildForSort.append(" ) as temp");
            String sortName = (String)params.get("sortName");
            switch (sortName) {
                case "gbId":
                    sqlBuildForSort.append(" order by gb_id ");
                    break;
                case "gbDeviceId":
                    sqlBuildForSort.append(" order by gb_device_id ");
                    break;
                case "gbName":
                    sqlBuildForSort.append(" order by gb_name ");
                    break;
                case "gbStatus":
                    sqlBuildForSort.append(" order by gb_status ");
                    break;
                case "createTime":
                    sqlBuildForSort.append(" order by create_time ");
                    break;
                case "updateTime":
                    sqlBuildForSort.append(" order by update_time ");
                    break;
                case "deviceCode":
                    sqlBuildForSort.append(" order by deviceCode ");
                    break;
            }

            if (params.get("order") != null && (Boolean)params.get("order")) {
                sqlBuildForSort.append(" ASC");
            }
            if (params.get("order") != null && !(Boolean)params.get("order")) {
                sqlBuildForSort.append(" DESC");
            }
            return sqlBuildForSort.toString();
        }else {
            return sqlBuild.toString();
        }
    }

    public String queryListInBox(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_CAMERA_DEVICE);
        sqlBuild.append(" where wdc.channel_type = 0 AND wdc.data_type != 2 AND (wdc.gb_ptz_type is null ||  ( wdc.gb_ptz_type != 98 && wdc.gb_ptz_type != 99)) " +
                " AND coalesce(wdc.gb_parent_id, wdc.parent_id) in (");

        sqlBuild.append(" ");
        List<CameraGroup> groupList = (List<CameraGroup>)params.get("groupList");
        boolean first = true;
        for (CameraGroup group : groupList) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append("'" + group.getDeviceId() + "'");
            first = false;
        }
        sqlBuild.append(" )");

        sqlBuild.append(" AND coalesce(wdc.gb_longitude, wdc.longitude) >= #{minLongitude} AND coalesce(wdc.gb_longitude, wdc.longitude) <= #{maxLongitude}");
        sqlBuild.append(" AND coalesce(wdc.gb_latitude,  wdc.latitude) >= #{minLatitude} AND coalesce(wdc.gb_latitude,  wdc.latitude) <= #{maxLatitude}");

        if (params.get("level") != null) {
            sqlBuild.append(" AND ( map_level <= #{level} || map_level is null )");
        }

        return sqlBuild.toString();
    }

    public String queryListInCircleForMysql(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_CAMERA_DEVICE);
        sqlBuild.append(" where wdc.channel_type = 0 AND wdc.data_type != 2 AND (wdc.gb_ptz_type is null ||  ( wdc.gb_ptz_type != 98 && wdc.gb_ptz_type != 99)) " +
                " AND coalesce(wdc.gb_parent_id, wdc.parent_id) in (");

        sqlBuild.append(" ");
        List<CameraGroup> groupList = (List<CameraGroup>)params.get("groupList");
        boolean first = true;
        for (CameraGroup group : groupList) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append("'" + group.getDeviceId() + "'");
            first = false;
        }
        sqlBuild.append(" )");

        String geomTextBuilder = "point(" + params.get("centerLongitude") + " " + params.get("centerLatitude") + ")";

        sqlBuild.append("AND ST_Distance_Sphere(point(coalesce(wdc.gb_longitude, wdc.longitude), coalesce(wdc.gb_latitude, wdc.latitude)), ST_GeomFromText('").append(geomTextBuilder).append("')) < #{radius}");

        if (params.get("level") != null) {
            sqlBuild.append(" AND ( map_level <= #{level} || map_level is null )");
        }

        return sqlBuild.toString();
    }

    public String queryListInCircleForKingBase(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_CAMERA_DEVICE);
        sqlBuild.append(" where wdc.channel_type = 0 AND wdc.data_type != 2 AND (wdc.gb_ptz_type is null ||  ( wdc.gb_ptz_type != 98 && wdc.gb_ptz_type != 99)) " +
                " AND coalesce(wdc.gb_parent_id, wdc.parent_id) in (");

        sqlBuild.append(" ");
        List<CameraGroup> groupList = (List<CameraGroup>)params.get("groupList");
        boolean first = true;
        for (CameraGroup group : groupList) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append("'" + group.getDeviceId() + "'");
            first = false;
        }
        sqlBuild.append(" )");

        String geomTextBuilder = "point(" + params.get("centerLongitude") + " " + params.get("centerLatitude") + ")";

        sqlBuild.append("AND ST_DistanceSphere(ST_MakePoint(coalesce(wdc.gb_longitude, wdc.longitude), coalesce(wdc.gb_latitude, wdc.latitude)), ST_GeomFromText('").append(geomTextBuilder).append("')) < #{radius}");

        if (params.get("level") != null) {
            sqlBuild.append(" AND ( map_level <= #{level} || map_level is null )");
        }

        return sqlBuild.toString();
    }

    public String queryListInPolygonForMysql(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_CAMERA_DEVICE);
        sqlBuild.append(" where wdc.channel_type = 0 AND wdc.data_type != 2 AND (wdc.gb_ptz_type is null ||  ( wdc.gb_ptz_type != 98 && wdc.gb_ptz_type != 99)) " +
                " AND coalesce(wdc.gb_parent_id, wdc.parent_id) in (");

        sqlBuild.append(" ");
        List<CameraGroup> groupList = (List<CameraGroup>)params.get("groupList");
        boolean first = true;
        for (CameraGroup group : groupList) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append("'" + group.getDeviceId() + "'");
            first = false;
        }
        sqlBuild.append(" )");

        StringBuilder geomTextBuilder = new StringBuilder();
        geomTextBuilder.append("POLYGON((");
        List<Point> pointList = (List<Point>)params.get("pointList");
        for (int i = 0; i < pointList.size(); i++) {
            if (i > 0) {
                geomTextBuilder.append(", ");
            }
            Point point = pointList.get(i);
            geomTextBuilder.append(point.getLng()).append(" ").append(point.getLat());
        }
        geomTextBuilder.append("))");
        sqlBuild.append("AND ST_Within(point(coalesce(wdc.gb_longitude, wdc.longitude), coalesce(wdc.gb_latitude, wdc.latitude)), ST_GeomFromText('").append(geomTextBuilder).append("'))");

        if (params.get("level") != null) {
            sqlBuild.append(" AND ( map_level <= #{level} || map_level is null )");
        }

        return sqlBuild.toString();
    }

    public String queryListInPolygonForKingBase(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_CAMERA_DEVICE);
        sqlBuild.append(" where wdc.channel_type = 0 AND wdc.data_type != 2 AND (wdc.gb_ptz_type is null ||  ( wdc.gb_ptz_type != 98 && wdc.gb_ptz_type != 99)) " +
                " AND coalesce(wdc.gb_parent_id, wdc.parent_id) in (");

        sqlBuild.append(" ");
        List<CameraGroup> groupList = (List<CameraGroup>)params.get("groupList");
        boolean first = true;
        for (CameraGroup group : groupList) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append("'" + group.getDeviceId() + "'");
            first = false;
        }
        sqlBuild.append(" )");

        StringBuilder geomTextBuilder = new StringBuilder();
        geomTextBuilder.append("POLYGON((");
        List<Point> pointList = (List<Point>)params.get("pointList");
        for (int i = 0; i < pointList.size(); i++) {
            if (i > 0) {
                geomTextBuilder.append(", ");
            }
            Point point = pointList.get(i);
            geomTextBuilder.append(point.getLng()).append(" ").append(point.getLat());
        }
        geomTextBuilder.append("))");
        sqlBuild.append("AND ST_Within(ST_MakePoint(coalesce(wdc.gb_longitude, wdc.longitude), coalesce(wdc.gb_latitude, wdc.latitude)), ST_GeomFromText('").append(geomTextBuilder).append("'))");

        if (params.get("level") != null) {
            sqlBuild.append(" AND ( map_level <= #{level} || map_level is null )");
        }

        return sqlBuild.toString();
    }

    public String queryGbChannelByChannelDeviceIdAndGbDeviceId(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_CAMERA_DEVICE);
        sqlBuild.append(" where coalesce(wdc.gb_device_id, wdc.device_id) = #{channelDeviceId}");
        if (params.get("gbDeviceId") != null) {
            sqlBuild.append(" AND wdc.data_type = 1 and wd.device_id = #{gbDeviceId}");
        }
        return sqlBuild.toString();
    }

    public String queryListByAddressAndDirectionType(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_CAMERA_DEVICE);
        sqlBuild.append(" where coalesce(wdc.gb_address, wdc.address) = #{address}");
        if (params.get("directionType") != null) {
            sqlBuild.append(" and coalesce(wdc.gb_direction_type,  wdc.direction_type) = #{directionType}");
        }
        return sqlBuild.toString();
    }


    public String queryListByDeviceIds(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append("<script> ");
        sqlBuild.append(BASE_SQL_FOR_CAMERA_DEVICE);
        sqlBuild.append(" where coalesce(wdc.gb_device_id,  wdc.device_id) in ");
        sqlBuild.append(" <foreach item='item' index='index' collection='deviceIds' open='(' separator=',' close=')'> #{item} </foreach>");
        sqlBuild.append(" </script>");
        return sqlBuild.toString() ;
    }

    public String queryCameraChannelByIds(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_CAMERA_DEVICE);
        sqlBuild.append(" where wdc.id in ( ");

        List<CommonGBChannel> channelList = (List<CommonGBChannel>)params.get("channelList");
        boolean first = true;
        for (CommonGBChannel channel : channelList) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append(channel.getGbId());
            first = false;
        }
        sqlBuild.append(" )");
        return sqlBuild.toString() ;
    }

    public String queryListForSyMobile(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_CAMERA_DEVICE);
        sqlBuild.append(" WHERE wdc.gb_ptz_type = 99 and wdc.channel_type = 0 AND wdc.data_type != 2 ");
        if (params.get("business") != null) {
            sqlBuild.append(" AND coalesce(gb_business_group_id, business_group_id) = #{business}");
        }
        return sqlBuild.toString();
    }

    public String queryMeetingChannelList(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL_FOR_CAMERA_DEVICE);
        sqlBuild.append(" WHERE wdc.channel_type = 0 AND wdc.data_type = 3 and wdc.gb_ptz_type = 98 and coalesce(wdc.gb_business_group_id, wdc.business_group_id) = #{business}");
        return sqlBuild.toString();
    }


    public String queryCameraChannelById(Map<String, Object> params ){
        return BASE_SQL_FOR_CAMERA_DEVICE + " where wdc.id = #{gbId}";
    }
}
