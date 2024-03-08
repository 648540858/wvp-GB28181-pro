package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.storager.dao.dto.ChannelSourceInfo;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储上级平台
 */
@Mapper
@Repository
public interface ParentPlatformMapper {

    @Insert("INSERT INTO wvp_platform (enable, name, server_gb_id, server_gb_domain, server_ip, server_port,device_gb_id,device_ip,"+
            "device_port,username,password,expires,keep_timeout,transport,character_set,ptz,rtcp,as_message_channel,auto_push_channel,"+
            "status,start_offline_push,catalog_id,administrative_division,catalog_group,create_time,update_time) " +
            "            VALUES (#{enable}, #{name}, #{serverGBId}, #{serverGBDomain}, #{serverIP}, #{serverPort}, #{deviceGBId}, #{deviceIp}, " +
            "            #{devicePort}, #{username}, #{password}, #{expires}, #{keepTimeout}, #{transport}, #{characterSet}, #{ptz}, #{rtcp}, #{asMessageChannel}, #{autoPushChannel}, " +
            "            #{status},  #{startOfflinePush}, #{catalogId}, #{administrativeDivision}, #{catalogGroup}, #{createTime}, #{updateTime})")
    int addParentPlatform(ParentPlatform parentPlatform);

    @Update("UPDATE wvp_platform " +
            "SET enable=#{enable}, " +
            "name=#{name}," +
            "device_gb_id=#{deviceGBId}," +
            "server_gb_id=#{serverGBId}, " +
            "server_gb_domain=#{serverGBDomain}, " +
            "server_ip=#{serverIP}," +
            "server_port=#{serverPort}, " +
            "device_ip=#{deviceIp}, " +
            "device_port=#{devicePort}, " +
            "username=#{username}, " +
            "password=#{password}, " +
            "expires=#{expires}, " +
            "keep_timeout=#{keepTimeout}, " +
            "transport=#{transport}, " +
            "character_set=#{characterSet}, " +
            "ptz=#{ptz}, " +
            "rtcp=#{rtcp}, " +
            "as_message_channel=#{asMessageChannel}, " +
            "auto_push_channel=#{autoPushChannel}, " +
            "status=#{status}, " +
            "start_offline_push=#{startOfflinePush}, " +
            "catalog_group=#{catalogGroup}, " +
            "administrative_division=#{administrativeDivision}, " +
            "create_time=#{createTime}, " +
            "update_time=#{updateTime}, " +
            "catalog_id=#{catalogId} " +
            "WHERE id=#{id}")
    int updateParentPlatform(ParentPlatform parentPlatform);

    @Delete("DELETE FROM wvp_platform WHERE server_gb_id=#{serverGBId}")
    int delParentPlatform(ParentPlatform parentPlatform);

    @Select("SELECT *, ((SELECT count(0)\n" +
            "              FROM wvp_platform_gb_channel pc\n" +
            "              WHERE pc.platform_id = pp.server_gb_id)\n" +
            "              +\n" +
            "              (SELECT count(0)\n" +
            "              FROM wvp_platform_gb_stream pgs\n" +
            "              WHERE pgs.platform_id = pp.server_gb_id)\n" +
            "              +\n" +
            "              (SELECT count(0)\n" +
            "              FROM wvp_platform_catalog pgc\n" +
            "              WHERE pgc.platform_id = pp.server_gb_id)) as channel_count\n" +
            "FROM wvp_platform pp ")
    List<ParentPlatform> getParentPlatformList();

    @Select("SELECT * FROM wvp_platform WHERE enable=#{enable} ")
    List<ParentPlatform> getEnableParentPlatformList(boolean enable);

    @Select("SELECT * FROM wvp_platform WHERE enable=true and as_message_channel=true")
    List<ParentPlatform> queryEnablePlatformListWithAsMessageChannel();

    @Select("SELECT * FROM wvp_platform WHERE server_gb_id=#{platformGbId}")
    ParentPlatform getParentPlatByServerGBId(String platformGbId);

    @Select("SELECT * FROM wvp_platform WHERE id=#{id}")
    ParentPlatform getParentPlatById(int id);

    @Update("UPDATE wvp_platform SET status=false" )
    int outlineForAllParentPlatform();

    @Update("UPDATE wvp_platform SET status=#{online} WHERE server_gb_id=#{platformGbID}" )
    int updateParentPlatformStatus(@Param("platformGbID") String platformGbID, @Param("online") boolean online);

    @Update(value = {" <script>" +
            "UPDATE wvp_platform " +
            "SET catalog_id=#{catalogId}, update_time=#{updateTime}" +
            "WHERE server_gb_id=#{platformId}"+
            "</script>"})
    int setDefaultCatalog(@Param("platformId") String platformId, @Param("catalogId") String catalogId, @Param("updateTime") String updateTime);

    @Select("select 'channel' as name, count(pgc.platform_id) count from wvp_platform_gb_channel pgc left join wvp_device_channel dc on dc.id = pgc.device_channel_id where  pgc.platform_id=#{platform_id} and dc.channel_id =#{gbId} " +
            "union " +
            "select 'stream' as name, count(pgs.platform_id) count from wvp_platform_gb_stream pgs left join wvp_gb_stream gs on pgs.gb_stream_id = gs.gb_stream_id where  pgs.platform_id=#{platform_id} and gs.gb_id =#{gbId}")
    List<ChannelSourceInfo> getChannelSource(@Param("platform_id") String platform_id, @Param("gbId") String gbId);
}
