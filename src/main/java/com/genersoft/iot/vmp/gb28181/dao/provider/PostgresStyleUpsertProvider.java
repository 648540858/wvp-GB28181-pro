package com.genersoft.iot.vmp.gb28181.dao.provider;

import java.util.Map;

public class PostgresStyleUpsertProvider {

    public String batchUpsert(Map<String, Object> params) {
        return "<script>" +
                "INSERT INTO wvp_device_channel (" +
                "device_id, data_type, data_device_id, name, manufacturer, model, owner, civil_code, block, " +
                "address, parental, parent_id, safety_way, register_way, cert_num, certifiable, err_code, end_time, secrecy, " +
                "ip_address, port, password, status, longitude, latitude, ptz_type, position_type, room_type, use_type, " +
                "supply_light_type, direction_type, resolution, business_group_id, download_speed, svc_space_support_mod, " +
                "svc_time_support_mode, create_time, update_time, sub_count, stream_id, has_audio, gps_time, stream_identification, channel_type" +
                ") VALUES " +
                "<foreach collection='channels' item='item' separator=','>" +
                "(#{item.deviceId}, #{item.dataType}, #{item.dataDeviceId}, #{item.name}, #{item.manufacturer}, #{item.model}, #{item.owner}, #{item.civilCode}, #{item.block}, " +
                "#{item.address}, #{item.parental}, #{item.parentId}, #{item.safetyWay}, #{item.registerWay}, #{item.certNum}, #{item.certifiable}, #{item.errCode}, #{item.endTime}, #{item.secrecy}, " +
                "#{item.ipAddress}, #{item.port}, #{item.password}, #{item.status}, #{item.longitude}, #{item.latitude}, #{item.ptzType}, #{item.positionType}, #{item.roomType}, #{item.useType}, " +
                "#{item.supplyLightType}, #{item.directionType}, #{item.resolution}, #{item.businessGroupId}, #{item.downloadSpeed}, #{item.svcSpaceSupportMod}," +
                " #{item.svcTimeSupportMode}, #{item.createTime}, #{item.updateTime}, #{item.subCount}, #{item.streamId}, #{item.hasAudio}, #{item.gpsTime}, #{item.streamIdentification}, #{item.channelType})" +
                "</foreach>" +
                "ON CONFLICT (data_device_id, device_id) DO UPDATE SET " +
                "name=EXCLUDED.name, manufacturer=EXCLUDED.manufacturer, model=EXCLUDED.model, owner=EXCLUDED.owner, " +
                "civil_code=EXCLUDED.civil_code, block=EXCLUDED.block, address=EXCLUDED.address, " +
                "parental=EXCLUDED.parental, parent_id=EXCLUDED.parent_id, safety_way=EXCLUDED.safety_way, " +
                "register_way=EXCLUDED.register_way, cert_num=EXCLUDED.cert_num, certifiable=EXCLUDED.certifiable, " +
                "err_code=EXCLUDED.err_code, end_time=EXCLUDED.end_time, secrecy=EXCLUDED.secrecy, " +
                "ip_address=EXCLUDED.ip_address, port=EXCLUDED.port, password=EXCLUDED.password, " +
                "status=EXCLUDED.status, longitude=EXCLUDED.longitude, latitude=EXCLUDED.latitude, " +
                "ptz_type=EXCLUDED.ptz_type, position_type=EXCLUDED.position_type, room_type=EXCLUDED.room_type, " +
                "use_type=EXCLUDED.use_type, supply_light_type=EXCLUDED.supply_light_type, " +
                "direction_type=EXCLUDED.direction_type, resolution=EXCLUDED.resolution, " +
                "business_group_id=EXCLUDED.business_group_id, download_speed=EXCLUDED.download_speed, " +
                "svc_space_support_mod=EXCLUDED.svc_space_support_mod, " +
                "svc_time_support_mode=EXCLUDED.svc_time_support_mode, " +
                "update_time=EXCLUDED.update_time, sub_count=EXCLUDED.sub_count, " +
                "stream_id=EXCLUDED.stream_id, has_audio=EXCLUDED.has_audio, " +
                "gps_time=EXCLUDED.gps_time, stream_identification=EXCLUDED.stream_identification, " +
                "channel_type=EXCLUDED.channel_type" +
                "</script>";
    }
}
