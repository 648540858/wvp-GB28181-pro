package com.genersoft.iot.vmp.gb28181.dao.provider;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ChannelProvider {

    public final static String BASE_SQL = "select\n" +
            "    id as gb_id,\n" +
            "    device_db_id as gb_device_db_id,\n" +
            "    stream_push_id,\n" +
            "    stream_proxy_id,\n" +
            "    create_time,\n" +
            "    update_time,\n" +
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
    
    private final static String BASE_SQL_FOR_PLATFORM =
            "select\n" +
            "    wdc.id as gb_id,\n" +
            "    wdc.device_db_id as gb_device_db_id,\n" +
            "    wdc.stream_push_id,\n" +
            "    wdc.stream_proxy_id,\n" +
            "    wdc.create_time,\n" +
            "    wdc.update_time,\n" +
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

    public String queryByDeviceId(Map<String, Object> params ){
        return BASE_SQL + " where channel_type = 0 and coalesce(gb_device_id, device_id) = #{gbDeviceId}";
    }

    public String queryById(Map<String, Object> params ){
        return BASE_SQL + " where channel_type = 0 and id = #{gbId}";
    }

    public String queryByStreamPushId(Map<String, Object> params ){
        return BASE_SQL + " where channel_type = 0 and stream_push_id = #{streamPushId}";
    }

    public String queryByStreamProxyId(Map<String, Object> params ){
        return BASE_SQL + " where channel_type = 0 and stream_proxy_id = #{streamProxyId}";
    }


    public String queryList(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append(" where channel_type = 0 ");
        if (params.get("query") != null) {
            sqlBuild.append(" AND (coalesce(gb_device_id, device_id) LIKE concat('%',#{query},'%')" +
                    " OR coalesce(gb_name, name) LIKE concat('%',#{query},'%') )")
            ;
        }
        if (params.get("online") != null && (Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(gb_status, status) = 'ON'");
        }
        if (params.get("online") != null && !(Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(gb_status, status) = 'OFF'");
        }
        if (params.get("hasCivilCode") != null && (Boolean)params.get("hasCivilCode")) {
            sqlBuild.append(" AND coalesce(gb_civil_code, civil_code) is not null");
        }
        if (params.get("hasCivilCode") != null && !(Boolean)params.get("hasCivilCode")) {
            sqlBuild.append(" AND coalesce(gb_civil_code, civil_code) is null");
        }
        if (params.get("hasGroup") != null && (Boolean)params.get("hasGroup")) {
            sqlBuild.append(" AND coalesce(gb_parent_id, parent_id) is not null");
        }
        if (params.get("hasGroup") != null && !(Boolean)params.get("hasGroup")) {
            sqlBuild.append(" AND coalesce(gb_parent_id, parent_id) is null");
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
        sqlBuild.append("where channel_type = 0 and device_db_id in ( ");

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

        sqlBuild.append(" where channel_type = 0 and stream_push_id in ( ");
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
}
