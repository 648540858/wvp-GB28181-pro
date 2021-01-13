package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.vmanager.platform.bean.ChannelReduce;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface PatformChannelMapper {

    /**
     * 查询列表里已经关联的
     */
    @Select("<script> "+
            "SELECT deviceAndChannelId FROM platform_gb_channel WHERE platformId='${platformId}' AND deviceAndChannelId in" +
            "<foreach collection='deviceAndChannelIds' open='(' item='id_' separator=',' close=')'> '${id_}'</foreach>" +
            "</script>")
    List<String> findChannelRelatedPlatform(String platformId, List<String> deviceAndChannelIds);

    @Insert("<script> "+
            "INSERT INTO platform_gb_channel (channelId, deviceId, platformId, deviceAndChannelId) VALUES" +
            "<foreach collection='channelReducesToAdd'  item='item' separator=','> ('${item.channelId}','${item.deviceId}', '${platformId}', '${item.deviceId}_${item.channelId}' )</foreach>" +
            "</script>")
    int addChannels(String platformId, List<ChannelReduce> channelReducesToAdd);


    @Delete("<script> "+
            "DELETE FROM platform_gb_channel WHERE deviceAndChannelId in" +
            "<foreach collection='channelReducesToDel'  item='item'  open='(' separator=',' close=')' > '${item.deviceId}_${item.channelId}'</foreach>" +
            "</script>")
    int delChannelForGB(String platformId, List<ChannelReduce> channelReducesToDel);
}
