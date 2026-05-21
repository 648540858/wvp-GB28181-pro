package com.genersoft.iot.vmp.gb28181.dao.provider;

import java.util.Map;

public class MysqlChannelUpsertProvider {

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
                "ON DUPLICATE KEY UPDATE " +
                "name=VALUES(name), manufacturer=VALUES(manufacturer), model=VALUES(model), owner=VALUES(owner), " +
                "civil_code=VALUES(civil_code), block=VALUES(block), address=VALUES(address), " +
                "parental=VALUES(parental), parent_id=VALUES(parent_id), safety_way=VALUES(safety_way), " +
                "register_way=VALUES(register_way), cert_num=VALUES(cert_num), certifiable=VALUES(certifiable), " +
                "err_code=VALUES(err_code), end_time=VALUES(end_time), secrecy=VALUES(secrecy), " +
                "ip_address=VALUES(ip_address), port=VALUES(port), password=VALUES(password), " +
                "status=VALUES(status), longitude=VALUES(longitude), latitude=VALUES(latitude), " +
                "ptz_type=VALUES(ptz_type), position_type=VALUES(position_type), room_type=VALUES(room_type), " +
                "use_type=VALUES(use_type), supply_light_type=VALUES(supply_light_type), " +
                "direction_type=VALUES(direction_type), resolution=VALUES(resolution), " +
                "business_group_id=VALUES(business_group_id), download_speed=VALUES(download_speed), " +
                "svc_space_support_mod=VALUES(svc_space_support_mod), " +
                "svc_time_support_mode=VALUES(svc_time_support_mode), " +
                "update_time=VALUES(update_time), sub_count=VALUES(sub_count), " +
                "stream_id=VALUES(stream_id), has_audio=VALUES(has_audio), " +
                "gps_time=VALUES(gps_time), stream_identification=VALUES(stream_identification), " +
                "channel_type=VALUES(channel_type)" +
                "</script>";
    }
}
