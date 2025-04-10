package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储设备信息
 */
@Mapper
@Repository
public interface DeviceMapper {

    @Select("SELECT " +
            "id, " +
            "device_id, " +
            "coalesce(custom_name, name) as name, " +
            "password, " +
            "manufacturer, " +
            "model, " +
            "firmware, " +
            "transport," +
            "stream_mode," +
            "ip," +
            "sdp_ip," +
            "local_ip," +
            "port," +
            "host_address," +
            "expires," +
            "register_time," +
            "keepalive_time," +
            "create_time," +
            "update_time," +
            "charset," +
            "subscribe_cycle_for_catalog," +
            "subscribe_cycle_for_mobile_position," +
            "mobile_position_submission_interval," +
            "subscribe_cycle_for_alarm," +
            "ssrc_check," +
            "as_message_channel," +
            "geo_coord_sys," +
            "on_line," +
            "server_id,"+
            "media_server_id," +
            "broadcast_push_after_ack," +
            "(SELECT count(0) FROM wvp_device_channel dc WHERE dc.data_type = 1 and dc.data_device_id= de.id) as channel_count "+
            " FROM wvp_device de WHERE de.device_id = #{deviceId}")
    Device getDeviceByDeviceId( @Param("deviceId") String deviceId);

    @Insert("INSERT INTO wvp_device (" +
                "device_id, " +
                "name, " +
                "manufacturer, " +
                "model, " +
                "firmware, " +
                "transport," +
                "stream_mode," +
                "media_server_id," +
                "ip," +
                "sdp_ip," +
                "local_ip," +
                "port," +
                "host_address," +
                "expires," +
                "register_time," +
                "keepalive_time," +
                "heart_beat_interval," +
                "heart_beat_count," +
                "position_capability," +
                "create_time," +
                "update_time," +
                "charset," +
                "subscribe_cycle_for_catalog," +
                "subscribe_cycle_for_mobile_position,"+
                "mobile_position_submission_interval,"+
                "subscribe_cycle_for_alarm,"+
                "ssrc_check,"+
                "as_message_channel,"+
                "broadcast_push_after_ack,"+
                "geo_coord_sys,"+
                "server_id,"+
                "on_line"+
            ") VALUES (" +
                "#{deviceId}," +
                "#{name}," +
                "#{manufacturer}," +
                "#{model}," +
                "#{firmware}," +
                "#{transport}," +
                "#{streamMode}," +
                "#{mediaServerId}," +
                "#{ip}," +
                "#{sdpIp}," +
                "#{localIp}," +
                "#{port}," +
                "#{hostAddress}," +
                "#{expires}," +
                "#{registerTime}," +
                "#{keepaliveTime}," +
                "#{heartBeatInterval}," +
                "#{heartBeatCount}," +
                "#{positionCapability}," +
                "#{createTime}," +
                "#{updateTime}," +
                "#{charset}," +
                "#{subscribeCycleForCatalog}," +
                "#{subscribeCycleForMobilePosition}," +
                "#{mobilePositionSubmissionInterval}," +
                "#{subscribeCycleForAlarm}," +
                "#{ssrcCheck}," +
                "#{asMessageChannel}," +
                "#{broadcastPushAfterAck}," +
                "#{geoCoordSys}," +
                "#{serverId}," +
                "#{onLine}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(Device device);

    @Update(value = {" <script>" +
                "UPDATE wvp_device " +
                "SET update_time=#{updateTime}" +
                "<if test=\"name != null\">, name=#{name}</if>" +
                "<if test=\"manufacturer != null\">, manufacturer=#{manufacturer}</if>" +
                "<if test=\"model != null\">, model=#{model}</if>" +
                "<if test=\"firmware != null\">, firmware=#{firmware}</if>" +
                "<if test=\"transport != null\">, transport=#{transport}</if>" +
                "<if test=\"ip != null\">, ip=#{ip}</if>" +
                "<if test=\"localIp != null\">, local_ip=#{localIp}</if>" +
                "<if test=\"port != null\">, port=#{port}</if>" +
                "<if test=\"hostAddress != null\">, host_address=#{hostAddress}</if>" +
                "<if test=\"onLine != null\">, on_line=#{onLine}</if>" +
                "<if test=\"registerTime != null\">, register_time=#{registerTime}</if>" +
                "<if test=\"keepaliveTime != null\">, keepalive_time=#{keepaliveTime}</if>" +
                "<if test=\"heartBeatInterval != null\">, heart_beat_interval=#{heartBeatInterval}</if>" +
                "<if test=\"positionCapability != null\">, position_capability=#{positionCapability}</if>" +
                "<if test=\"heartBeatCount != null\">, heart_beat_count=#{heartBeatCount}</if>" +
                "<if test=\"expires != null\">, expires=#{expires}</if>" +
                "<if test=\"serverId != null\">, server_id=#{serverId}</if>" +
                "WHERE device_id=#{deviceId}"+
            " </script>"})
    int update(Device device);

    @Select(
            " <script>" +
            "SELECT " +
            "id, " +
            "device_id, " +
            "coalesce(custom_name, name) as name, " +
            "password, " +
            "manufacturer, " +
            "model, " +
            "firmware, " +
            "transport," +
            "stream_mode," +
            "ip,"+
            "sdp_ip,"+
            "local_ip,"+
            "port,"+
            "host_address,"+
            "expires,"+
            "register_time,"+
            "keepalive_time,"+
            "create_time,"+
            "update_time,"+
            "charset,"+
            "subscribe_cycle_for_catalog,"+
            "subscribe_cycle_for_mobile_position,"+
            "mobile_position_submission_interval,"+
            "subscribe_cycle_for_alarm,"+
            "ssrc_check,"+
            "as_message_channel,"+
            "broadcast_push_after_ack,"+
            "geo_coord_sys,"+
            "on_line,"+
            "media_server_id,"+
            "(SELECT count(0) FROM wvp_device_channel dc WHERE dc.data_type = #{dataType} and dc.data_device_id= de.id) as channel_count " +
            "FROM wvp_device de" +
            "<if test='online != null'> where de.on_line=${online}</if>"+
            " order by de.create_time desc "+
            " </script>"
    )
    List<Device> getDevices(@Param("dataType") Integer dataType, @Param("online") Boolean online);

    @Delete("DELETE FROM wvp_device WHERE device_id=#{deviceId}")
    int del(String deviceId);

    @Select("SELECT " +
            "id, " +
            "device_id, " +
            "coalesce(custom_name, name) as name, " +
            "password, " +
            "manufacturer, " +
            "model, " +
            "firmware, " +
            "transport," +
            "stream_mode," +
            "ip," +
            "sdp_ip,"+
            "local_ip,"+
            "port,"+
            "host_address,"+
            "expires,"+
            "register_time,"+
            "keepalive_time,"+
            "create_time,"+
            "update_time,"+
            "charset,"+
            "subscribe_cycle_for_catalog,"+
            "subscribe_cycle_for_mobile_position,"+
            "mobile_position_submission_interval,"+
            "subscribe_cycle_for_alarm,"+
            "ssrc_check,"+
            "as_message_channel,"+
            "broadcast_push_after_ack,"+
            "geo_coord_sys,"+
            "server_id,"+
            "on_line"+
            " FROM wvp_device WHERE on_line = true")
    List<Device> getOnlineDevices();
    @Select("SELECT " +
            "id, " +
            "device_id, " +
            "coalesce(custom_name, name) as name, " +
            "password, " +
            "manufacturer, " +
            "model, " +
            "firmware, " +
            "transport," +
            "stream_mode," +
            "ip," +
            "sdp_ip,"+
            "local_ip,"+
            "port,"+
            "host_address,"+
            "expires,"+
            "register_time,"+
            "keepalive_time,"+
            "create_time,"+
            "update_time,"+
            "charset,"+
            "subscribe_cycle_for_catalog,"+
            "subscribe_cycle_for_mobile_position,"+
            "mobile_position_submission_interval,"+
            "subscribe_cycle_for_alarm,"+
            "ssrc_check,"+
            "as_message_channel,"+
            "broadcast_push_after_ack,"+
            "geo_coord_sys,"+
            "server_id,"+
            "on_line"+
            " FROM wvp_device WHERE on_line = true and server_id = #{serverId}")
    List<Device> getOnlineDevicesByServerId(@Param("serverId") String serverId);

    @Select("SELECT " +
            "id,"+
            "device_id,"+
            "coalesce(custom_name,name)as name,"+
            "password,"+
            "manufacturer,"+
            "model,"+
            "firmware,"+
            "transport,"+
            "stream_mode,"+
            "ip,"+
            "sdp_ip,"+
            "local_ip,"+
            "port,"+
            "host_address,"+
            "expires,"+
            "register_time,"+
            "keepalive_time,"+
            "create_time,"+
            "update_time,"+
            "charset,"+
            "subscribe_cycle_for_catalog,"+
            "subscribe_cycle_for_mobile_position,"+
            "mobile_position_submission_interval,"+
            "subscribe_cycle_for_alarm,"+
            "ssrc_check,"+
            "as_message_channel,"+
            "broadcast_push_after_ack,"+
            "geo_coord_sys,"+
            "on_line"+
            " FROM wvp_device WHERE ip = #{host} AND port=#{port}")
    Device getDeviceByHostAndPort(@Param("host") String host, @Param("port") int port);

    @Update(value = {" <script>" +
            "UPDATE wvp_device " +
            "SET update_time=#{updateTime}, custom_name=#{name} , password=#{password}, stream_mode=#{streamMode}" +
            ", ip=#{ip}, sdp_ip=#{sdpIp}, port=#{port}, charset=#{charset}" +
            ", ssrc_check=#{ssrcCheck}, as_message_channel=#{asMessageChannel}" +
            ", broadcast_push_after_ack=#{broadcastPushAfterAck}, geo_coord_sys=#{geoCoordSys}, media_server_id=#{mediaServerId}" +
            " WHERE id=#{id}"+
            " </script>"})
    void updateCustom(Device device);

    @Insert("INSERT INTO wvp_device (" +
            "device_id,"+
            "custom_name,"+
            "password,"+
            "sdp_ip,"+
            "create_time,"+
            "update_time,"+
            "charset,"+
            "ssrc_check,"+
            "as_message_channel,"+
            "broadcast_push_after_ack,"+
            "geo_coord_sys,"+
            "on_line,"+
            "stream_mode," +
            "server_id," +
            "media_server_id"+
            ") VALUES (" +
            "#{deviceId}," +
            "#{name}," +
            "#{password}," +
            "#{sdpIp}," +
            "#{createTime}," +
            "#{updateTime}," +
            "#{charset}," +
            "#{ssrcCheck}," +
            "#{asMessageChannel}," +
            "#{broadcastPushAfterAck}," +
            "#{geoCoordSys}," +
            "#{onLine}," +
            "#{streamMode}," +
            "#{serverId}," +
            "#{mediaServerId}" +
            ")")
    void addCustomDevice(Device device);

    @Select("select * FROM wvp_device")
    List<Device> getAll();

    @Select("select * FROM wvp_device where  as_message_channel = true")
    List<Device> queryDeviceWithAsMessageChannel();

    @Select(" <script>" +
            "SELECT " +
            "id, " +
            "device_id, " +
            "coalesce(custom_name, name) as name, " +
            "password, " +
            "manufacturer, " +
            "model, " +
            "firmware, " +
            "transport," +
            "stream_mode," +
            "ip,"+
            "sdp_ip,"+
            "local_ip,"+
            "port,"+
            "host_address,"+
            "expires,"+
            "register_time,"+
            "keepalive_time,"+
            "create_time,"+
            "update_time,"+
            "charset,"+
            "subscribe_cycle_for_catalog,"+
            "subscribe_cycle_for_mobile_position,"+
            "mobile_position_submission_interval,"+
            "subscribe_cycle_for_alarm,"+
            "ssrc_check,"+
            "as_message_channel,"+
            "broadcast_push_after_ack,"+
            "geo_coord_sys,"+
            "on_line,"+
            "media_server_id,"+
            "server_id,"+
            "(SELECT count(0) FROM wvp_device_channel dc WHERE dc.data_type = #{dataType} and dc.data_device_id= de.id) as channel_count " +
            " FROM wvp_device de" +
            " where 1 = 1 "+
            " <if test='status != null'> AND de.on_line=${status}</if>"+
            " <if test='query != null'> AND (" +
            " coalesce(custom_name, name) LIKE concat('%',#{query},'%') escape '/' " +
            " OR device_id LIKE concat('%',#{query},'%') escape '/' " +
            " OR ip LIKE concat('%',#{query},'%') escape '/')" +
            "</if> " +
            " order by create_time desc, device_id " +
            " </script>")
    List<Device> getDeviceList(@Param("dataType") Integer dataType, @Param("query") String query, @Param("status") Boolean status);

    @Select("select * from wvp_device_channel where id = #{id}")
    DeviceChannel getRawChannel(@Param("id") int id);

    @Select("select * from wvp_device where id = #{id}")
    Device query(@Param("id") Integer id);

    @Select("select wd.* from wvp_device wd left join wvp_device_channel wdc on wdc.data_type = #{dataType} and wd.id = wdc.data_device_id  where wdc.id = #{channelId}")
    Device queryByChannelId(@Param("dataType") Integer dataType, @Param("channelId") Integer channelId);

    @Select("select wd.* from wvp_device wd left join wvp_device_channel wdc on wdc.data_type = #{dataType} and wd.id = wdc.data_device_id  where wdc.device_id = #{channelDeviceId}")
    Device getDeviceBySourceChannelDeviceId(@Param("dataType") Integer dataType, @Param("channelDeviceId") String channelDeviceId);

    @Update(value = {" <script>" +
            " UPDATE wvp_device " +
            "  SET subscribe_cycle_for_catalog=#{subscribeCycleForCatalog}" +
            " WHERE id=#{id}"+
            " </script>"})
    void updateSubscribeCatalog(Device device);

    @Update(value = {" <script>" +
            "UPDATE wvp_device " +
            "SET subscribe_cycle_for_mobile_position=#{subscribeCycleForMobilePosition}, mobile_position_submission_interval=#{mobilePositionSubmissionInterval}" +
            " WHERE id=#{id}"+
            " </script>"})
    void updateSubscribeMobilePosition(Device device);
}
