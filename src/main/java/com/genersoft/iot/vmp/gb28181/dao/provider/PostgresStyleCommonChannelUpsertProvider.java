package com.genersoft.iot.vmp.gb28181.dao.provider;

import java.util.Map;

public class PostgresStyleCommonChannelUpsertProvider {

    public String batchUpsert(Map<String, Object> params) {
        return "<script>" +
                "INSERT INTO wvp_device_channel (" +
                "gb_device_id, data_type, data_device_id, create_time, update_time, " +
                "gb_name, gb_manufacturer, gb_model, gb_owner, gb_civil_code, gb_block, gb_address, " +
                "gb_parental, gb_parent_id, gb_safety_way, gb_register_way, gb_cert_num, gb_certifiable, " +
                "gb_err_code, gb_end_time, gb_secrecy, gb_ip_address, gb_port, gb_password, gb_status, " +
                "gb_longitude, gb_latitude, gb_ptz_type, gb_position_type, gb_room_type, gb_use_type, " +
                "gb_supply_light_type, gb_direction_type, gb_resolution, gb_business_group_id, " +
                "gb_download_speed, gb_svc_space_support_mod, gb_svc_time_support_mode, enable_broadcast" +
                ") VALUES " +
                "<foreach collection='channels' item='item' separator=','>" +
                "(#{item.gbDeviceId}, #{item.dataType}, #{item.dataDeviceId}, #{item.createTime}, #{item.updateTime}," +
                "#{item.gbName}, #{item.gbManufacturer}, #{item.gbModel}, #{item.gbOwner}, #{item.gbCivilCode}, #{item.gbBlock}, #{item.gbAddress}," +
                "#{item.gbParental}, #{item.gbParentId}, #{item.gbSafetyWay}, #{item.gbRegisterWay}, #{item.gbCertNum}, #{item.gbCertifiable}," +
                "#{item.gbErrCode}, #{item.gbEndTime}, #{item.gbSecrecy}, #{item.gbIpAddress}, #{item.gbPort}, #{item.gbPassword}, #{item.gbStatus}," +
                "#{item.gbLongitude}, #{item.gbLatitude}, #{item.gbPtzType}, #{item.gbPositionType}, #{item.gbRoomType}, #{item.gbUseType}," +
                "#{item.gbSupplyLightType}, #{item.gbDirectionType}, #{item.gbResolution}, #{item.gbBusinessGroupId}," +
                "#{item.gbDownloadSpeed}, #{item.gbSvcSpaceSupportMod}, #{item.gbSvcTimeSupportMode}, #{item.enableBroadcast})" +
                "</foreach>" +
                "ON CONFLICT (gb_device_id) DO UPDATE SET " +
                "gb_name=EXCLUDED.gb_name, gb_manufacturer=EXCLUDED.gb_manufacturer, gb_model=EXCLUDED.gb_model, " +
                "gb_owner=EXCLUDED.gb_owner, gb_civil_code=EXCLUDED.gb_civil_code, gb_block=EXCLUDED.gb_block, " +
                "gb_address=EXCLUDED.gb_address, gb_parental=EXCLUDED.gb_parental, gb_parent_id=EXCLUDED.gb_parent_id, " +
                "gb_safety_way=EXCLUDED.gb_safety_way, gb_register_way=EXCLUDED.gb_register_way, " +
                "gb_cert_num=EXCLUDED.gb_cert_num, gb_certifiable=EXCLUDED.gb_certifiable, " +
                "gb_err_code=EXCLUDED.gb_err_code, gb_end_time=EXCLUDED.gb_end_time, gb_secrecy=EXCLUDED.gb_secrecy, " +
                "gb_ip_address=EXCLUDED.gb_ip_address, gb_port=EXCLUDED.gb_port, gb_password=EXCLUDED.gb_password, " +
                "gb_status=EXCLUDED.gb_status, gb_longitude=EXCLUDED.gb_longitude, gb_latitude=EXCLUDED.gb_latitude, " +
                "gb_ptz_type=EXCLUDED.gb_ptz_type, gb_position_type=EXCLUDED.gb_position_type, " +
                "gb_room_type=EXCLUDED.gb_room_type, gb_use_type=EXCLUDED.gb_use_type, " +
                "gb_supply_light_type=EXCLUDED.gb_supply_light_type, gb_direction_type=EXCLUDED.gb_direction_type, " +
                "gb_resolution=EXCLUDED.gb_resolution, gb_business_group_id=EXCLUDED.gb_business_group_id, " +
                "gb_download_speed=EXCLUDED.gb_download_speed, " +
                "gb_svc_space_support_mod=EXCLUDED.gb_svc_space_support_mod, " +
                "gb_svc_time_support_mode=EXCLUDED.gb_svc_time_support_mode, " +
                "enable_broadcast=EXCLUDED.enable_broadcast, update_time=EXCLUDED.update_time" +
                "</script>";
    }
}
