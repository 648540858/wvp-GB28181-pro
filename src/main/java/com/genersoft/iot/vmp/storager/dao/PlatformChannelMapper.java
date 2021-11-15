package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface PlatformChannelMapper {

    /**
     * 查询列表里已经关联的
     */
    @Select("<script> "+
            "SELECT deviceAndChannelId FROM platform_gb_channel WHERE platformId='${platformId}' AND deviceAndChannelId in" +
            "<foreach collection='deviceAndChannelIds' open='(' item='id_' separator=',' close=')'> '${id_}'</foreach> ORDER BY deviceAndChannelId ASC" +
            "</script>")
    List<String> findChannelRelatedPlatform(String platformId, List<String> deviceAndChannelIds);

    @Insert("<script> "+
            "INSERT INTO platform_gb_channel (channelId, deviceId, platformId, deviceAndChannelId) VALUES" +
            "<foreach collection='channelReducesToAdd'  item='item' separator=','>" +
            " ('${item.channelId}','${item.deviceId}', '${platformId}', '${item.deviceId}_${item.channelId}' )" +
            "</foreach>" +
            "</script>")
    int addChannels(String platformId, List<ChannelReduce> channelReducesToAdd);


    @Delete("<script> "+
            "DELETE FROM platform_gb_channel WHERE platformId='${platformId}' AND deviceAndChannelId in" +
            "<foreach collection='channelReducesToDel'  item='item'  open='(' separator=',' close=')' > '${item.deviceId}_${item.channelId}'</foreach>" +
            "</script>")
    int delChannelForGB(String platformId, List<ChannelReduce> channelReducesToDel);

    @Delete("<script> "+
            "DELETE FROM platform_gb_channel WHERE deviceId='${deviceId}' " +
            "</script>")
    int delChannelForDeviceId(String deviceId);

    @Delete("<script> "+
            "DELETE FROM platform_gb_channel WHERE platformId='${platformId}'"  +
            "</script>")
    int cleanChannelForGB(String platformId);


    @Select("SELECT * FROM device_channel WHERE deviceId = (SELECT deviceId FROM platform_gb_channel WHERE " +
            "platformId='${platformId}' AND channelId='${channelId}' ) AND channelId='${channelId}'")
    DeviceChannel queryChannelInParentPlatform(String platformId, String channelId);

    @Select("SELECT * FROM device WHERE deviceId = (SELECT deviceId FROM platform_gb_channel WHERE platformId='${platformId}' AND channelId='${channelId}')")
    Device queryVideoDeviceByPlatformIdAndChannelId(String platformId, String channelId);
}
