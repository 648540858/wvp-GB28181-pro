package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.Platform;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储上级平台
 */
@Mapper
@Repository
public interface PlatformMapper {

    @Insert("INSERT INTO wvp_platform (enable, name, server_gb_id, server_gb_domain, server_ip, server_port,device_gb_id,device_ip,"+
            " device_port,username,password,expires,keep_timeout,transport,character_set,ptz,rtcp,status,catalog_group, update_time," +
            " create_time, as_message_channel, send_stream_ip, auto_push_channel, catalog_with_platform,catalog_with_group,catalog_with_region, "+
            " civil_code,manufacturer,model,address,register_way,secrecy,server_id) " +
            " VALUES (#{enable}, #{name}, #{serverGBId}, #{serverGBDomain}, #{serverIp}, #{serverPort}, #{deviceGBId}, #{deviceIp}, " +
            " #{devicePort}, #{username}, #{password}, #{expires}, #{keepTimeout}, #{transport}, #{characterSet}, #{ptz}, #{rtcp}, #{status}, #{catalogGroup},#{updateTime}," +
            " #{createTime}, #{asMessageChannel}, #{sendStreamIp}, #{autoPushChannel}, #{catalogWithPlatform}, #{catalogWithGroup},#{catalogWithRegion}, " +
            " #{civilCode}, #{manufacturer}, #{model}, #{address}, #{registerWay}, #{secrecy}, #{serverId})")
    int add(Platform parentPlatform);

    @Update("UPDATE wvp_platform " +
            "SET update_time = #{updateTime}," +
            " enable=#{enable}, " +
            " name=#{name}," +
            " server_gb_id=#{serverGBId}, " +
            " server_gb_domain=#{serverGBDomain}, " +
            " server_ip=#{serverIp}," +
            " server_port=#{serverPort}, " +
            " device_gb_id=#{deviceGBId}," +
            " device_ip=#{deviceIp}, " +
            " device_port=#{devicePort}, " +
            " username=#{username}, " +
            " password=#{password}, " +
            " expires=#{expires}, " +
            " keep_timeout=#{keepTimeout}, " +
            " transport=#{transport}, " +
            " character_set=#{characterSet}, " +
            " ptz=#{ptz}, " +
            " rtcp=#{rtcp}, " +
            " status=#{status}, " +
            " catalog_group=#{catalogGroup}, " +
            " as_message_channel=#{asMessageChannel}, " +
            " send_stream_ip=#{sendStreamIp}, " +
            " auto_push_channel=#{autoPushChannel}, " +
            " catalog_with_platform=#{catalogWithPlatform}, " +
            " catalog_with_group=#{catalogWithGroup}, " +
            " catalog_with_region=#{catalogWithRegion}, " +
            " civil_code=#{civilCode}, " +
            " manufacturer=#{manufacturer}, " +
            " model=#{model}, " +
            " address=#{address}, " +
            " register_way=#{registerWay}, " +
            " server_id=#{serverId}, " +
            " secrecy=#{secrecy} " +
            "WHERE id=#{id}")
    int update(Platform parentPlatform);

    @Delete("DELETE FROM wvp_platform WHERE id=#{id}")
    int delete(@Param("id") Integer id);

    @Select(" <script>" +
            " SELECT pp.*, " +
            " ( (SELECT count(0) FROM wvp_platform_channel pc WHERE pc.platform_id = pp.id ) + " +
            "  (SELECT count(0) FROM wvp_platform_group pg WHERE pg.platform_id = pp.id ) * pp.catalog_with_group  + " +
            "  (SELECT count(0) FROM wvp_platform_region pr WHERE pr.platform_id = pp.id ) * pp.catalog_with_region + " +
            "  pp.catalog_with_platform " +
            "    ) as channel_count" +
            " FROM wvp_platform pp where 1=1 " +
            " <if test='query != null'> " +
            " AND (pp.name LIKE concat('%',#{query},'%') escape '/' OR pp.server_gb_id  LIKE concat('%',#{query},'%') escape '/' )</if> " +
            " order by pp.id desc"+
            " </script>")
    List<Platform> queryList(@Param("query") String query);

    @Select("SELECT * FROM wvp_platform WHERE server_id=#{serverId} and enable=#{enable} ")
    List<Platform> queryEnableParentPlatformList(@Param("serverId") String serverId, @Param("enable") boolean enable);

    @Select("SELECT * FROM wvp_platform WHERE enable=true and as_message_channel=true")
    List<Platform> queryEnablePlatformListWithAsMessageChannel();

    @Select("SELECT * FROM wvp_platform WHERE server_gb_id=#{platformGbId}")
    Platform getParentPlatByServerGBId(String platformGbId);

    @Select("SELECT * FROM wvp_platform WHERE id=#{id}")
    Platform query(int id);

    @Update("UPDATE wvp_platform SET status=#{online} WHERE server_gb_id=#{platformGbID}" )
    int updateStatus(@Param("platformGbID") String platformGbID, @Param("online") boolean online);

    @Select("SELECT server_id FROM wvp_platform WHERE enable=true and server_id != #{serverId} group by server_id")
    List<String> queryServerIdsWithEnableAndNotInServer(@Param("serverId") String serverId);

    @Select("SELECT * FROM wvp_platform WHERE server_id = #{serverId}")
    List<Platform> queryByServerId(@Param("serverId") String serverId);
}
