package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.common.CommonGbChannel;
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
            "device_port,username,password,expires,keep_timeout,transport,character_set,ptz,rtcp,as_message_channel,auto_push_channel," +
            "share_all_channel,share_group,share_region,"+
            "status,start_offline_push,catalog_id,administrative_division,catalog_group,create_time,update_time) " +
            "            VALUES (#{enable}, #{name}, #{serverGBId}, #{serverGBDomain}, #{serverIP}, #{serverPort}, #{deviceGBId}, #{deviceIp}, " +
            "            #{devicePort}, #{username}, #{password}, #{expires}, #{keepTimeout}, #{transport}, #{characterSet}, #{ptz}, " +
            "            #{rtcp}, #{asMessageChannel}, #{autoPushChannel}, #{shareAllChannel}, #{shareGroup}, #{shareRegion}, " +
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
            "share_all_channel=#{shareAllChannel}, " +
            "share_group=#{shareGroup}, " +
            "share_region=#{shareRegion}, " +
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

    @Select("<script>" +
            "SELECT * " +
            "<if test='shareAllChannel == false'> " +
            "(SELECT count(0) as channel_count FROM wvp_common_channel_platform wccp WHERE wccp.platform_id = pp.id) " +
            "</if>" +
            "<if test='shareAllChannel == true'> " +
            "(SELECT count(0) as channel_count FROM wvp_common_channel ) " +
            "</if>" +
            "FROM wvp_platform pp " +
            "</script>")
    List<ParentPlatform> getParentPlatformList();

    @Select("SELECT * FROM wvp_platform WHERE enable=#{enable} ")
    List<ParentPlatform> getEnableParentPlatformList(boolean enable);

    @Select("SELECT * FROM wvp_platform WHERE enable=true and as_message_channel=true")
    List<ParentPlatform> queryEnablePlatformListWithAsMessageChannel();

    @Select("SELECT * FROM wvp_platform WHERE server_gb_id=#{platformGbId}")
    ParentPlatform getParentPlatByServerGBId(String platformGbId);

    @Select("SELECT * FROM wvp_platform WHERE id=#{id}")
    ParentPlatform getParentPlatById(int id);

    @Update("UPDATE wvp_platform SET status=#{online} WHERE server_gb_id=#{platformGbID}" )
    int updateParentPlatformStatus(@Param("platformGbID") String platformGbID, @Param("online") boolean online);


    @Select("SELECT * FROM wvp_platform WHERE share_all_channel=true")
    List<ParentPlatform> queryAllWithShareAll();


    @Select("<script>" +
            "select wp.* " +
            " from wvp_platform wp\n" +
            "         left join wvp_common_channel_platform wccp on wp.id = wccp.platform_id\n" +
            " where wp.share_all_channel = true " +
            "<if test='(channelList != null and channelList.size != 0) and (platformIdList != null and platformIdList.size != 0) '> " +
            " or (wccp.common_gb_channel_id in " +
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' >#{item.commonGbId}</foreach>" +
            " and wccp.platform_id in " +
            " <foreach collection='platformIdList'  item='item'  open='(' separator=',' close=')' >#{item}</foreach>)" +
            "</if>" +
            "<if test='(channelList != null and channelList.size != 0) and (platformIdList == null or platformIdList.size == 0) '> " +
            " or wccp.common_gb_channel_id in " +
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' >#{item.commonGbId}</foreach>" +
            "</if>" +
            "<if test='(channelList == null or channelList.size == 0) and (platformIdList != null and platformIdList.size != 0) '> " +
            " or wccp.platform_id in " +
            " <foreach collection='platformIdList'  item='item'  open='(' separator=',' close=')' >#{item}</foreach>" +
            "</if>" +
            "</script>")
    List<ParentPlatform> querySharePlatform(@Param("channelList") List<CommonGbChannel> channelList,
                                            @Param("platformIdList") List<Integer> platformIdList);
}
