package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.PlatformCatalog;
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
            "SELECT deviceChannelId FROM platform_gb_channel WHERE platformId='${platformId}' AND deviceChannelId in" +
            "<foreach collection='channelReduces' open='(' item='item' separator=',' close=')'> '${item.id}'</foreach>" +
            "</script>")
    List<Integer> findChannelRelatedPlatform(String platformId, List<ChannelReduce> channelReduces);

    @Insert("<script> "+
            "INSERT INTO platform_gb_channel (platformId, deviceChannelId, catalogId) VALUES" +
            "<foreach collection='channelReducesToAdd'  item='item' separator=','>" +
            " ('${platformId}', '${item.id}' , '${item.catalogId}' )" +
            "</foreach>" +
            "</script>")
    int addChannels(String platformId, List<ChannelReduce> channelReducesToAdd);

    @Delete("<script> "+
            "DELETE FROM platform_gb_channel WHERE platformId='${platformId}' AND deviceChannelId in" +
            "<foreach collection='channelReducesToDel'  item='item'  open='(' separator=',' close=')' > '${item.id}'</foreach>" +
            "</script>")
    int delChannelForGB(String platformId, List<ChannelReduce> channelReducesToDel);

    @Delete("<script> "+
            "DELETE FROM platform_gb_channel WHERE deviceChannelId in " +
            "( select  temp.deviceChannelId from " +
            "(select pgc.deviceChannelId from platform_gb_channel pgc " +
            "left join device_channel dc on dc.id = pgc.deviceChannelId where dc.deviceId  =#{deviceId} " +
            ") temp)" +
            "</script>")
    int delChannelForDeviceId(String deviceId);

    @Delete("<script> "+
            "DELETE FROM platform_gb_channel WHERE platformId='${platformId}'"  +
            "</script>")
    int cleanChannelForGB(String platformId);

    @Select("SELECT dc.* FROM platform_gb_channel pgc left join device_channel dc on dc.id = pgc.deviceChannelId WHERE dc.channelId='${channelId}' and pgc.platformId='${platformId}'")
    List<DeviceChannel> queryChannelInParentPlatform(String platformId, String channelId);

    @Select(" select dc.channelId as id, dc.name as name, pgc.platformId as platformId, pgc.catalogId as parentId, 0 as childrenCount, 1 as type " +
            " from device_channel dc left join platform_gb_channel pgc on dc.id = pgc.deviceChannelId " +
            " where pgc.platformId=#{platformId} and pgc.catalogId=#{catalogId}")
    List<PlatformCatalog> queryChannelInParentPlatformAndCatalog(String platformId, String catalogId);

    @Select("select d.*\n" +
            "from platform_gb_channel pgc\n" +
            "         left join device_channel dc on dc.id = pgc.deviceChannelId\n" +
            "         left join device d on dc.deviceId = d.deviceId\n" +
            "where dc.channelId = #{channelId} and pgc.platformId=#{platformId}")
    List<Device> queryVideoDeviceByPlatformIdAndChannelId(String platformId, String channelId);

    @Delete("<script> "+
            "DELETE FROM platform_gb_channel WHERE catalogId=#{id}"  +
            "</script>")
    int delByCatalogId(String id);

    @Delete("<script> "+
           "DELETE FROM platform_gb_channel  WHERE catalogId=#{parentId} AND platformId=#{platformId} AND channelId=#{id}"  +
           "</script>")
    int delByCatalogIdAndChannelIdAndPlatformId(PlatformCatalog platformCatalog);

    @Select("<script> " +
            "SELECT " +
            "pp.* " +
            "FROM " +
            "parent_platform pp " +
            "left join platform_gb_channel pgc on " +
            "pp.serverGBId = pgc.platformId " +
            "left join device_channel dc on " +
            "dc.id = pgc.deviceChannelId " +
            "WHERE " +
            "dc.channelId = #{channelId} and pp.status = true " +
            "AND pp.serverGBId IN" +
            "<foreach collection='platforms'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</script> ")
    List<ParentPlatform> queryPlatFormListForGBWithGBId(String channelId, List<String> platforms);

    @Delete("<script> " +
           "DELETE FROM platform_gb_channel WHERE platformId=#{serverGBId}"  +
           "</script>")
    void delByPlatformId(String serverGBId);
}
