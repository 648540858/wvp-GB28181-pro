package com.genersoft.iot.vmp.gb28181.dao.provider;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;

public class DeviceChannelProvider {

    public String getBaseSelectSql(){
        return "SELECT " +
                " dc.id,\n" +
                " dc.data_device_id,\n" +
                " dc.create_time,\n" +
                " dc.update_time,\n" +
                " dc.sub_count,\n" +
                " dc.stream_id,\n" +
                " dc.has_audio,\n" +
                " dc.gps_time,\n" +
                " dc.stream_identification,\n" +
                " dc.channel_type,\n" +
                " coalesce(dc.gb_device_id, dc.device_id) as device_id,\n" +
                " coalesce(dc.gb_name, dc.name) as name,\n" +
                " coalesce(dc.gb_manufacturer, dc.manufacturer) as manufacturer,\n" +
                " coalesce(dc.gb_model, dc.model) as model,\n" +
                " coalesce(dc.gb_owner, dc.owner) as owner,\n" +
                " coalesce(dc.gb_civil_code, dc.civil_code) as civil_code,\n" +
                " coalesce(dc.gb_block, dc.block) as block,\n" +
                " coalesce(dc.gb_address, dc.address) as address,\n" +
                " coalesce(dc.gb_parental, dc.parental) as parental,\n" +
                " coalesce(dc.gb_parent_id, dc.parent_id) as parent_id,\n" +
                " coalesce(dc.gb_safety_way, dc.safety_way) as safety_way,\n" +
                " coalesce(dc.gb_register_way, dc.register_way) as register_way,\n" +
                " coalesce(dc.gb_cert_num, dc.cert_num) as cert_num,\n" +
                " coalesce(dc.gb_certifiable, dc.certifiable) as certifiable,\n" +
                " coalesce(dc.gb_err_code, dc.err_code) as err_code,\n" +
                " coalesce(dc.gb_end_time, dc.end_time) as end_time,\n" +
                " coalesce(dc.gb_secrecy, dc.secrecy) as secrecy,\n" +
                " coalesce(dc.gb_ip_address, dc.ip_address) as ip_address,\n" +
                " coalesce(dc.gb_port, dc.port) as port,\n" +
                " coalesce(dc.gb_password, dc.password) as password,\n" +
                " coalesce(dc.gb_status, dc.status) as status,\n" +
                " coalesce(dc.gb_longitude, dc.longitude) as longitude,\n" +
                " coalesce(dc.gb_latitude, dc.latitude) as latitude,\n" +
                " coalesce(dc.gb_ptz_type, dc.ptz_type) as ptz_type,\n" +
                " coalesce(dc.gb_position_type, dc.position_type) as position_type,\n" +
                " coalesce(dc.gb_room_type, dc.room_type) as room_type,\n" +
                " coalesce(dc.gb_use_type, dc.use_type) as use_type,\n" +
                " coalesce(dc.gb_supply_light_type, dc.supply_light_type) as supply_light_type,\n" +
                " coalesce(dc.gb_direction_type, dc.direction_type) as direction_type,\n" +
                " coalesce(dc.gb_resolution, dc.resolution) as resolution,\n" +
                " coalesce(dc.gb_business_group_id, dc.business_group_id) as business_group_id,\n" +
                " coalesce(dc.gb_download_speed, dc.download_speed) as download_speed,\n" +
                " coalesce(dc.gb_svc_space_support_mod, dc.svc_space_support_mod) as svc_space_support_mod,\n" +
                " coalesce(dc.gb_svc_time_support_mode,dc.svc_time_support_mode) as svc_time_support_mode\n" +
                " from " +
                " wvp_device_channel dc "
                ;
    }
    public String queryChannels(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append(" where data_type = " + ChannelDataType.GB28181.value + " and dc.data_device_id = #{dataDeviceId} ");
        if (params.get("businessGroupId") != null ) {
            sqlBuild.append(" AND coalesce(dc.gb_business_group_id, dc.business_group_id)=#{businessGroupId} AND coalesce(dc.gb_parent_id, dc.parent_id) is null");
        }else if (params.get("parentChannelId") != null ) {
            sqlBuild.append(" AND coalesce(dc.gb_parent_id, dc.parent_id)=#{parentChannelId}");
        }
        if (params.get("civilCode") != null ) {
            sqlBuild.append(" AND (coalesce(dc.gb_civil_code, dc.civil_code) = #{civilCode} " +
                    "OR (LENGTH(coalesce(dc.gb_device_id, dc.device_id))=LENGTH(#{civilCode}) + 2) AND coalesce(dc.gb_device_id, dc.device_id) LIKE concat(#{civilCode},'%'))");
        }
        if (params.get("query") != null && !ObjectUtils.isEmpty(params.get("query"))) {
            sqlBuild.append(" AND (coalesce(dc.gb_device_id, dc.device_id) LIKE concat('%',#{query},'%') escape '/'" +
                    " OR coalesce(dc.gb_name, dc.name) LIKE concat('%',#{query},'%') escape '/')")
            ;
        }
        if (params.get("online") != null && (Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(gb_status, status) = 'ON'");
        }
        if (params.get("online") != null && !(Boolean)params.get("online")) {
            sqlBuild.append(" AND coalesce(gb_status, status) = 'OFF'");
        }
        if (params.get("hasSubChannel") != null && (Boolean)params.get("hasSubChannel")) {
            sqlBuild.append(" AND dc.sub_count > 0");
        }
        if (params.get("hasSubChannel") != null && !(Boolean)params.get("hasSubChannel")) {
            sqlBuild.append(" AND dc.sub_count = 0");
        }
        List<String> channelIds = (List<String>)params.get("channelIds");
        if (channelIds != null && !channelIds.isEmpty()) {
            sqlBuild.append(" AND dc.device_id in (");
            boolean first = true;
            for (String id : channelIds) {
                if (!first) {
                    sqlBuild.append(",");
                }
                sqlBuild.append(id);
                first = false;
            }
            sqlBuild.append(" )");
        }
        sqlBuild.append("ORDER BY device_id");
        return sqlBuild.toString();
    }


    public String queryChannelsByDeviceDbId(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append(" where data_type = " + ChannelDataType.GB28181.value + " and dc.data_device_id = #{dataDeviceId}");
        return sqlBuild.toString();
    }

    public String queryAllChannels(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append(" where data_type = " + ChannelDataType.GB28181.value + " and dc.data_device_id = #{dataDeviceId}");
        return sqlBuild.toString();
    }

    public String getOne(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append(" where dc.id=#{id}");
        return sqlBuild.toString();
    }

    public String getOneByDeviceId(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append(" where data_type = " + ChannelDataType.GB28181.value + " and dc.data_device_id=#{dataDeviceId} and coalesce(dc.gb_device_id, dc.device_id) = #{channelId}");
        return sqlBuild.toString();
    }



    public String queryByDeviceId(Map<String, Object> params ){
        return getBaseSelectSql() + " where data_type = " + ChannelDataType.GB28181.value + " and channel_type = 0 and coalesce(gb_device_id, device_id) = #{gbDeviceId}";
    }

    public String queryById(Map<String, Object> params ){
        return getBaseSelectSql() + " where data_type = " + ChannelDataType.GB28181.value + " and channel_type = 0 and id = #{gbId}";
    }


    public String queryList(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append(" where channel_type = 0 and data_type = " + ChannelDataType.GB28181.value);
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
}
