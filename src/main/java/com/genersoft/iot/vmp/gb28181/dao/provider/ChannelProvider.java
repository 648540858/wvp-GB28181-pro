package com.genersoft.iot.vmp.gb28181.dao.provider;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Group;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ChannelProvider {

    public String getBaseSelectSql(){
        return "select\n" +
                "    wdc.id as gb_id,\n" +
                "    wdc.device_db_id as gb_device_db_id,\n" +
                "    wcg.id as group_channel_id,\n" +
                "    wdc.stream_push_id,\n" +
                "    wdc.stream_proxy_id,\n" +
                "    wdc.create_time,\n" +
                "    wdc.update_time,\n" +
                "    coalesce(wdc.gb_device_id, wdc.device_id) as gb_device_id,\n" +
                "    coalesce(wdc.gb_name, wdc.name) as gb_name,\n" +
                "    coalesce(wdc.gb_manufacturer, manufacturer) as gb_manufacturer,\n" +
                "    coalesce(wdc.gb_model, wdc.model) as gb_model,\n" +
                "    coalesce(wdc.gb_owner, wdc.owner) as gb_owner,\n" +
                "    gb_civil_code,\n" +
                "    coalesce(wdc.gb_block, wdc.block) as gb_block,\n" +
                "    coalesce(wdc.gb_address, wdc.address) as gb_address,\n" +
                "    coalesce(wdc.gb_parental, wdc.parental) as gb_parental,\n" +
                "    wcg.device_id as gb_parent_id,\n" +
                "    coalesce(wdc.gb_safety_way, wdc.safety_way) as gb_safety_way,\n" +
                "    coalesce(wdc.gb_register_way, wdc.register_way) as gb_register_way,\n" +
                "    coalesce(wdc.gb_cert_num, wdc.cert_num) as gb_cert_num,\n" +
                "    coalesce(wdc.gb_certifiable, wdc.certifiable) as gb_certifiable,\n" +
                "    coalesce(wdc.gb_err_code, wdc.err_code) as gb_err_code,\n" +
                "    coalesce(wdc.gb_end_time, wdc.end_time) as gb_end_time,\n" +
                "    coalesce(wdc.gb_secrecy, wdc.secrecy) as gb_secrecy,\n" +
                "    coalesce(wdc.gb_ip_address, wdc.ip_address) as gb_ip_address,\n" +
                "    coalesce(wdc.gb_port, port) wdc.as gb_port,\n" +
                "    coalesce(wdc.gb_password, wdc.password) as gb_password,\n" +
                "    coalesce(wdc.gb_status, wdc.status) as gb_status,\n" +
                "    coalesce(wdc.gb_longitude, wdc.longitude) as gb_longitude,\n" +
                "    coalesce(wdc.gb_latitude, wdc.latitude) as gb_latitude,\n" +
                "    coalesce(wdc.gb_ptz_type, wdc.ptz_type) as gb_ptz_type,\n" +
                "    coalesce(wdc.gb_position_type, wdc.position_type) as gb_position_type,\n" +
                "    coalesce(wdc.gb_room_type, wdc.room_type) as gb_room_type,\n" +
                "    coalesce(wdc.gb_use_type, wdc.use_type) as gb_use_type,\n" +
                "    coalesce(wdc.gb_supply_light_type, wdc.supply_light_type) as gb_supply_light_type,\n" +
                "    coalesce(wdc.gb_direction_type, wdc.direction_type) as gb_direction_type,\n" +
                "    coalesce(wdc.gb_resolution, wdc.resolution) as gb_resolution,\n" +
                "    wcg.business_group as gb_business_group_id,\n" +
                "    coalesce(wdc.gb_download_speed, wdc.download_speed) as gb_download_speed,\n" +
                "    coalesce(wdc.gb_svc_space_support_mod, wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
                "    coalesce(wdc.gb_svc_time_support_mode, wdc.svc_time_support_mode) as gb_svc_time_support_mode\n" +
                " from wvp_device_channel wdc\n" + 
                " left jon wvp_common_group_channel wcgc on wcgc.channel_id = wdc.id\n" + 
                " left jon wvp_common_group wcg on wcgc.group_id = wcg.id\n"
                ;
    }


    public String queryByDeviceId(Map<String, Object> params ){
        return getBaseSelectSql() + " where wdc.gb_device_id = #{gbDeviceId} or wdc.device_id = #{gbDeviceId}";
    }

    public String queryById(Map<String, Object> params ){
        return getBaseSelectSql() + " where wdc.id = #{gbId}";
    }

    public String queryByStreamPushId(Map<String, Object> params ){
        return getBaseSelectSql() + " where wdc.stream_push_id = #{streamPushId}";
    }

    public String queryByStreamProxyId(Map<String, Object> params ){
        return getBaseSelectSql() + " where wdc.stream_proxy_id = #{streamProxyId}";
    }


    public String queryList(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append(" where 1 = 1 ");
        if (params.get("query") != null) {
            sqlBuild.append(" AND (coalesce(wdc.gb_device_id, device_id) LIKE concat('%',#{query},'%')" +
                    " OR coalesce(wdc.gb_name, name) LIKE concat('%',#{query},'%') )")
            ;
        }
        if (params.get("online") != null && (Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(wdc.gb_status, status) = 'ON'");
        }
        if (params.get("online") != null && !(Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(wdc.gb_status, status) = 'OFF'");
        }
        if (params.get("hasCivilCode") != null && (Boolean)params.get("hasCivilCode")) {
            sqlBuild.append(" AND wdc.gb_civil_code is not null");
        }
        if (params.get("hasCivilCode") != null && !(Boolean)params.get("hasCivilCode")) {
            sqlBuild.append(" AND wdc.gb_civil_code is null");
        }
        return sqlBuild.toString();
    }

    public String queryInListByStatus(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append("where wdc.gb_status=#{status} and wdc.id in ( ");

        List<CommonGBChannel> commonGBChannelList = (List<CommonGBChannel>)params.get("ids");
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
        sqlBuild.append("where wdc.id in ( ");

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
        sqlBuild.append("where wdc.device_db_id in ( ");

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
            sqlBuild.append(" wdc.gb_civil_code = #{civilCode} ");
            if (params.get("ids") != null) {
                sqlBuild.append(" OR ");
            }
        }
        if (params.get("ids") != null) {
            sqlBuild.append(" wdc.id in ( ");
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
        sqlBuild.append("where wdc.gb_civil_code = #{civilCode} ");
        return sqlBuild.toString() ;
    }

    public String queryByGroupList(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());

        sqlBuild.append(" wcg.id in ( ");
        Collection<Group> ids = (Collection<Group>)params.get("groupList");
        boolean first = true;
        for (Group group : ids) {
            if (!first) {
                sqlBuild.append(",");
            }
            sqlBuild.append(group.getId());
            first = false;
        }
        sqlBuild.append(" )");

        return sqlBuild.toString() ;
    }
}
