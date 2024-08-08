package com.genersoft.iot.vmp.gb28181.dao.provider;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Group;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ChannelProvider {

    public String getBaseSelectSql(){
        return "select\n" +
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
                "    gb_civil_code,\n" +
                "    coalesce(gb_block, block) as gb_block,\n" +
                "    coalesce(gb_address, address) as gb_address,\n" +
                "    coalesce(gb_parental, parental) as gb_parental,\n" +
                "    gb_parent_id,\n" +
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
                "    gb_business_group_id,\n" +
                "    coalesce(gb_download_speed, download_speed) as gb_download_speed,\n" +
                "    coalesce(gb_svc_space_support_mod, svc_space_support_mod) as gb_svc_space_support_mod,\n" +
                "    coalesce(gb_svc_time_support_mode,svc_time_support_mode) as gb_svc_time_support_mode\n" +
                " from wvp_device_channel\n"
                ;
    }


    public String queryByDeviceId(Map<String, Object> params ){
        return getBaseSelectSql() + " where gb_device_id = #{gbDeviceId} or device_id = #{gbDeviceId}";
    }

    public String queryById(Map<String, Object> params ){
        return getBaseSelectSql() + " where id = #{gbId}";
    }

    public String queryByStreamPushId(Map<String, Object> params ){
        return getBaseSelectSql() + " where stream_push_id = #{streamPushId}";
    }

    public String queryByStreamProxyId(Map<String, Object> params ){
        return getBaseSelectSql() + " where stream_proxy_id = #{streamProxyId}";
    }


    public String queryList(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append(" where 1 = 1 ");
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
            sqlBuild.append(" AND gb_civil_code is not null");
        }
        if (params.get("hasCivilCode") != null && !(Boolean)params.get("hasCivilCode")) {
            sqlBuild.append(" AND gb_civil_code is null");
        }
        if (params.get("hasGroup") != null && (Boolean)params.get("hasGroup")) {
            sqlBuild.append(" AND gb_business_group_id is not null");
        }
        if (params.get("hasGroup") != null && !(Boolean)params.get("hasGroup")) {
            sqlBuild.append(" AND gb_business_group_id is null");
        }
        return sqlBuild.toString();
    }

    public String queryInListByStatus(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append("where gb_status=#{status} and id in ( ");

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
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append("where id in ( ");

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
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append("where id in ( ");

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
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append("where ");
        if (params.get("civilCode") != null) {
            sqlBuild.append(" gb_civil_code = #{civilCode} ");
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
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append("where gb_civil_code = #{civilCode} ");
        return sqlBuild.toString() ;
    }

    public String queryByBusinessGroup(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append("where gb_business_group_id = #{businessGroup} ");
        return sqlBuild.toString() ;
    }

    public String queryByParentId(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append("where gb_parent_id = #{parentId} ");
        return sqlBuild.toString() ;
    }

    public String queryByGroupList(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());

        sqlBuild.append(" where gb_parent_id in ( ");
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
}
