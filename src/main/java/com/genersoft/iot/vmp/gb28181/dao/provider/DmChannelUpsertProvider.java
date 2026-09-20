package com.genersoft.iot.vmp.gb28181.dao.provider;

import java.util.Map;

/**
 * 达梦(DM8) upsert 实现，使用 MERGE 语法。
 * 注意：达梦 MERGE 的 USING 需为投影子查询，此处用 select ... from dual union all 拼接。
 */
public class DmChannelUpsertProvider {

    public String batchUpsert(Map<String, Object> params) {
        return "<script>" +
                "MERGE INTO wvp_device_channel t " +
                "USING ( " +
                " <foreach collection='channels' item='item' separator=' UNION ALL '> " +
                " SELECT " +
                " #{item.deviceId} AS device_id, #{item.dataType} AS data_type, #{item.dataDeviceId} AS data_device_id, " +
                " #{item.name} AS name, #{item.manufacturer} AS manufacturer, #{item.model} AS model, " +
                " #{item.owner} AS owner, #{item.civilCode} AS civil_code, #{item.block} AS block, " +
                " #{item.address} AS address, #{item.parental} AS parental, #{item.parentId} AS parent_id, " +
                " #{item.safetyWay} AS safety_way, #{item.registerWay} AS register_way, #{item.certNum} AS cert_num, " +
                " #{item.certifiable} AS certifiable, #{item.errCode} AS err_code, #{item.endTime} AS end_time, " +
                " #{item.secrecy} AS secrecy, #{item.ipAddress} AS ip_address, #{item.port} AS port, " +
                " #{item.password} AS password, #{item.status} AS status, #{item.longitude} AS longitude, " +
                " #{item.latitude} AS latitude, #{item.ptzType} AS ptz_type, #{item.positionType} AS position_type, " +
                " #{item.roomType} AS room_type, #{item.useType} AS use_type, #{item.supplyLightType} AS supply_light_type, " +
                " #{item.directionType} AS direction_type, #{item.resolution} AS resolution, " +
                " #{item.businessGroupId} AS business_group_id, #{item.downloadSpeed} AS download_speed, " +
                " #{item.svcSpaceSupportMod} AS svc_space_support_mod, #{item.svcTimeSupportMode} AS svc_time_support_mode, " +
                " #{item.createTime} AS create_time, #{item.updateTime} AS update_time, #{item.subCount} AS sub_count, " +
                " #{item.streamId} AS stream_id, #{item.hasAudio} AS has_audio, #{item.gpsTime} AS gps_time, " +
                " #{item.streamIdentification} AS stream_identification, #{item.channelType} AS channel_type " +
                " FROM DUAL " +
                " </foreach> " +
                " ) s ON (t.data_device_id = s.data_device_id AND t.device_id = s.device_id) " +
                " WHEN MATCHED THEN UPDATE SET " +
                " t.name = s.name, t.manufacturer = s.manufacturer, t.model = s.model, t.owner = s.owner, " +
                " t.civil_code = s.civil_code, t.block = s.block, t.address = s.address, " +
                " t.parental = s.parental, t.parent_id = s.parent_id, t.safety_way = s.safety_way, " +
                " t.register_way = s.register_way, t.cert_num = s.cert_num, t.certifiable = s.certifiable, " +
                " t.err_code = s.err_code, t.end_time = s.end_time, t.secrecy = s.secrecy, " +
                " t.ip_address = s.ip_address, t.port = s.port, t.password = s.password, " +
                " t.status = s.status, t.longitude = s.longitude, t.latitude = s.latitude, " +
                " t.ptz_type = s.ptz_type, t.position_type = s.position_type, t.room_type = s.room_type, " +
                " t.use_type = s.use_type, t.supply_light_type = s.supply_light_type, " +
                " t.direction_type = s.direction_type, t.resolution = s.resolution, " +
                " t.business_group_id = s.business_group_id, t.download_speed = s.download_speed, " +
                " t.svc_space_support_mod = s.svc_space_support_mod, " +
                " t.svc_time_support_mode = s.svc_time_support_mode, " +
                " t.update_time = s.update_time, t.sub_count = s.sub_count, " +
                " t.stream_id = s.stream_id, t.has_audio = s.has_audio, " +
                " t.gps_time = s.gps_time, t.stream_identification = s.stream_identification, " +
                " t.channel_type = s.channel_type " +
                " WHEN NOT MATCHED THEN INSERT " +
                " (data_device_id, device_id, data_type, name, manufacturer, model, owner, civil_code, block, " +
                " address, parental, parent_id, safety_way, register_way, cert_num, certifiable, err_code, end_time, secrecy, " +
                " ip_address, port, password, status, longitude, latitude, ptz_type, position_type, room_type, use_type, " +
                " supply_light_type, direction_type, resolution, business_group_id, download_speed, svc_space_support_mod, " +
                " svc_time_support_mode, create_time, update_time, sub_count, stream_id, has_audio, gps_time, stream_identification, channel_type) " +
                " VALUES " +
                " (s.data_device_id, s.device_id, s.data_type, s.name, s.manufacturer, s.model, s.owner, s.civil_code, s.block, " +
                " s.address, s.parental, s.parent_id, s.safety_way, s.register_way, s.cert_num, s.certifiable, s.err_code, s.end_time, s.secrecy, " +
                " s.ip_address, s.port, s.password, s.status, s.longitude, s.latitude, s.ptz_type, s.position_type, s.room_type, s.use_type, " +
                " s.supply_light_type, s.direction_type, s.resolution, s.business_group_id, s.download_speed, s.svc_space_support_mod, " +
                " s.svc_time_support_mode, s.create_time, s.update_time, s.sub_count, s.stream_id, s.has_audio, s.gps_time, s.stream_identification, s.channel_type) " +
                "</script>";
    }
}
