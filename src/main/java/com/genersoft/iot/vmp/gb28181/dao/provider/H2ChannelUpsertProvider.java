package com.genersoft.iot.vmp.gb28181.dao.provider;

import java.util.Map;

public class H2ChannelUpsertProvider {

    public String batchUpsert(Map<String, Object> params) {
        return "<script>" +
                "MERGE INTO wvp_device_channel (" +
                "data_device_id, device_id, data_type, name, manufacturer, model, owner, civil_code, block, " +
                "address, parental, parent_id, safety_way, register_way, cert_num, certifiable, err_code, end_time, secrecy, " +
                "ip_address, port, password, status, longitude, latitude, ptz_type, position_type, room_type, use_type, " +
                "supply_light_type, direction_type, resolution, business_group_id, download_speed, svc_space_support_mod, " +
                "svc_time_support_mode, create_time, update_time, sub_count, stream_id, has_audio, gps_time, stream_identification, channel_type" +
                ") KEY (data_device_id, device_id) VALUES " +
                "<foreach collection='channels' item='item' separator=','>" +
                "(#{item.dataDeviceId}, #{item.deviceId}, #{item.dataType}, #{item.name}, #{item.manufacturer}, #{item.model}, #{item.owner}, #{item.civilCode}, #{item.block}, " +
                "#{item.address}, #{item.parental}, #{item.parentId}, #{item.safetyWay}, #{item.registerWay}, #{item.certNum}, #{item.certifiable}, #{item.errCode}, #{item.endTime}, #{item.secrecy}, " +
                "#{item.ipAddress}, #{item.port}, #{item.password}, #{item.status}, #{item.longitude}, #{item.latitude}, #{item.ptzType}, #{item.positionType}, #{item.roomType}, #{item.useType}, " +
                "#{item.supplyLightType}, #{item.directionType}, #{item.resolution}, #{item.businessGroupId}, #{item.downloadSpeed}, #{item.svcSpaceSupportMod}," +
                " #{item.svcTimeSupportMode}, #{item.createTime}, #{item.updateTime}, #{item.subCount}, #{item.streamId}, #{item.hasAudio}, #{item.gpsTime}, #{item.streamIdentification}, #{item.channelType})" +
                "</foreach>" +
                "</script>";
    }
}
