package com.genersoft.iot.vmp.gb28181.dao.provider;

import java.util.Map;

public class H2CommonChannelUpsertProvider {

    public String batchUpsert(Map<String, Object> params) {
        return "<script>" +
                "MERGE INTO wvp_device_channel (" +
                "gb_device_id, data_type, data_device_id, create_time, update_time, " +
                "gb_name, gb_manufacturer, gb_model, gb_owner, gb_civil_code, gb_block, gb_address, " +
                "gb_parental, gb_parent_id, gb_safety_way, gb_register_way, gb_cert_num, gb_certifiable, " +
                "gb_err_code, gb_end_time, gb_secrecy, gb_ip_address, gb_port, gb_password, gb_status, " +
                "gb_longitude, gb_latitude, gb_ptz_type, gb_position_type, gb_room_type, gb_use_type, " +
                "gb_supply_light_type, gb_direction_type, gb_resolution, gb_business_group_id, " +
                "gb_download_speed, gb_svc_space_support_mod, gb_svc_time_support_mode, enable_broadcast" +
                ") KEY (gb_device_id) VALUES " +
                "<foreach collection='channels' item='item' separator=','>" +
                "(#{item.gbDeviceId}, #{item.dataType}, #{item.dataDeviceId}, #{item.createTime}, #{item.updateTime}," +
                "#{item.gbName}, #{item.gbManufacturer}, #{item.gbModel}, #{item.gbOwner}, #{item.gbCivilCode}, #{item.gbBlock}, #{item.gbAddress}," +
                "#{item.gbParental}, #{item.gbParentId}, #{item.gbSafetyWay}, #{item.gbRegisterWay}, #{item.gbCertNum}, #{item.gbCertifiable}," +
                "#{item.gbErrCode}, #{item.gbEndTime}, #{item.gbSecrecy}, #{item.gbIpAddress}, #{item.gbPort}, #{item.gbPassword}, #{item.gbStatus}," +
                "#{item.gbLongitude}, #{item.gbLatitude}, #{item.gbPtzType}, #{item.gbPositionType}, #{item.gbRoomType}, #{item.gbUseType}," +
                "#{item.gbSupplyLightType}, #{item.gbDirectionType}, #{item.gbResolution}, #{item.gbBusinessGroupId}," +
                "#{item.gbDownloadSpeed}, #{item.gbSvcSpaceSupportMod}, #{item.gbSvcTimeSupportMode}, #{item.enableBroadcast})" +
                "</foreach>" +
                "</script>";
    }
}
