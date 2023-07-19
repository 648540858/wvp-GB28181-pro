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
            "SELECT device_channel_id from wvp_platform_gb_channel WHERE platform_id=#{platformId} AND device_channel_id in" +
            "<foreach collection='channelReduces' open='(' item='item' separator=',' close=')'> #{item.id}</foreach>" +
            "</script>")
    List<Integer> findChannelRelatedPlatform(String platformId, List<ChannelReduce> channelReduces);

    @Insert("<script> "+
            "INSERT INTO wvp_platform_gb_channel (platform_id, device_channel_id, catalog_id) VALUES" +
            "<foreach collection='channelReducesToAdd'  item='item' separator=','>" +
            " (#{platformId}, #{item.id} , #{item.catalogId} )" +
            "</foreach>" +
            "</script>")
    int addChannels(String platformId, List<ChannelReduce> channelReducesToAdd);

    @Delete("<script> "+
            "DELETE from wvp_platform_gb_channel WHERE platform_id=#{platformId} AND device_channel_id in" +
            "<foreach collection='channelReducesToDel'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            "</script>")
    int delChannelForGB(String platformId, List<ChannelReduce> channelReducesToDel);

    @Delete("<script> "+
            "DELETE from wvp_platform_gb_channel WHERE device_channel_id in " +
            "( select  temp.device_channel_id from " +
            "(select pgc.device_channel_id from wvp_platform_gb_channel pgc " +
            "left join wvp_device_channel dc on dc.id = pgc.device_channel_id where dc.device_id  =#{deviceId} " +
            ") temp)" +
            "</script>")
    int delChannelForDeviceId(String deviceId);

    @Delete("<script> "+
            "DELETE from wvp_platform_gb_channel WHERE platform_id=#{platformId}"  +
            "</script>")
    int cleanChannelForGB(String platformId);

    @Select("SELECT dc.* from wvp_platform_gb_channel pgc left join wvp_device_channel dc on dc.id = pgc.device_channel_id WHERE dc.channel_id=#{channelId} and pgc.platform_id=#{platformId}")
    List<DeviceChannel> queryChannelInParentPlatform(String platformId, String channelId);

    @Select("SELECT dc.* from wvp_platform_gb_channel pgc left join wvp_device_channel dc on dc.id = pgc.device_channel_id WHERE pgc.platform_id=#{platformId} and pgc.catalog_id=#{catalogId}")
    List<DeviceChannel> queryAllChannelInCatalog(String platformId, String catalogId);

    @Select(" select dc.channel_id as id, dc.name as name, pgc.platform_id as platform_id, pgc.catalog_id as parent_id, 0 as children_count, 1 as type " +
            " from wvp_device_channel dc left join wvp_platform_gb_channel pgc on dc.id = pgc.device_channel_id " +
            " where pgc.platform_id=#{platformId} and pgc.catalog_id=#{catalogId}")
    List<PlatformCatalog> queryChannelInParentPlatformAndCatalog(String platformId, String catalogId);

    @Select("select d.*\n" +
            "from wvp_platform_gb_channel pgc\n" +
            "         left join wvp_device_channel dc on dc.id = pgc.device_channel_id\n" +
            "         left join wvp_device d on dc.device_id = d.device_id\n" +
            "where dc.channel_id = #{channelId} and pgc.platform_id=#{platformId}")
    List<Device> queryVideoDeviceByPlatformIdAndChannelId(String platformId, String channelId);

    @Delete("<script> "+
            "DELETE from wvp_platform_gb_channel WHERE platform_id=#{platformId} and catalog_id=#{id}"  +
            "</script>")
    int delByCatalogId(String platformId, String id);

    @Delete("<script> "+
           "DELETE from wvp_platform_gb_channel  WHERE catalog_id=#{parentId} AND platform_id=#{platformId} AND channel_id=#{id}"  +
           "</script>")
    int delByCatalogIdAndChannelIdAndPlatformId(PlatformCatalog platformCatalog);

    @Select("<script> " +
            "SELECT " +
            "pp.* " +
            "FROM " +
            "wvp_platform pp " +
            "left join wvp_platform_gb_channel pgc on " +
            "pp.server_gb_id = pgc.platform_id " +
            "left join wvp_device_channel dc on " +
            "dc.id = pgc.device_channel_id " +
            "WHERE " +
            "dc.channel_id = #{channelId} and pp.status = true " +
            "AND pp.server_gb_id IN" +
            "<foreach collection='platforms' item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</script> ")
    List<ParentPlatform> queryPlatFormListForGBWithGBId(String channelId, List<String> platforms);

    @Delete("<script> " +
           "DELETE from wvp_platform_gb_channel WHERE platform_id=#{serverGBId}"  +
           "</script>")
    void delByPlatformId(String serverGBId);

    @Delete("<script> " +
            "DELETE from wvp_platform_gb_channel WHERE platform_id=#{platformId} and catalog_id=#{catalogId}"  +
            "</script>")
    int delChannelForGBByCatalogId(String platformId, String catalogId);

    @Select("select dc.channel_id dc.device_id,dc.name,d.manufacturer,d.model,d.firmware\n" +
            "from wvp_platform_gb_channel pgc\n" +
            "         left join wvp_device_channel dc on dc.id = pgc.device_channel_id\n" +
            "         left join wvp_device d on dc.device_id = d.device_id\n" +
            "where dc.channel_id = #{channelId} and pgc.platform_id=#{platformId}")
    List<Device> queryDeviceInfoByPlatformIdAndChannelId(String platformId, String channelId);

    @Select("SELECT pgc.platform_id from wvp_platform_gb_channel pgc left join wvp_device_channel dc on dc.id = pgc.device_channel_id WHERE dc.channel_id='${channelId}'")
    List<String> queryParentPlatformByChannelId(String channelId);
}
