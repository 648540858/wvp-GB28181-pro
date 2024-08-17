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
            " civil_code,manufacturer,model,address,register_way,secrecy) " +
            " VALUES (#{enable}, #{name}, #{serverGBId}, #{serverGBDomain}, #{serverIp}, #{serverPort}, #{deviceGBId}, #{deviceIp}, " +
            " #{devicePort}, #{username}, #{password}, #{expires}, #{keepTimeout}, #{transport}, #{characterSet}, #{ptz}, #{rtcp}, #{status}, #{catalogGroup},#{updateTime}," +
            " #{createTime}, #{asMessageChannel}, #{sendStreamIp}, #{autoPushChannel}, #{catalogWithPlatform}, #{catalogWithGroup},#{catalogWithRegion}, " +
            " #{civilCode}, #{manufacturer}, #{model}, #{address}, #{registerWay}, #{secrecy})")
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
            " secrecy=#{secrecy} " +
            "WHERE id=#{id}")
    int update(Platform parentPlatform);

    @Delete("DELETE FROM wvp_platform WHERE server_gb_id=#{serverGBId}")
    int delParentPlatform(Platform parentPlatform);

    @Select(" SELECT pp.*, " +
            " (SELECT count(0) FROM wvp_platform_gb_channel pc WHERE pc.platform_id = pp.id  ) as channel_count" +
            " FROM wvp_platform pp "
    )
    List<Platform> queryList();

    @Select("SELECT * FROM wvp_platform WHERE enable=#{enable} ")
    List<Platform> getEnableParentPlatformList(boolean enable);

    @Select("SELECT * FROM wvp_platform WHERE enable=true and as_message_channel=true")
    List<Platform> queryEnablePlatformListWithAsMessageChannel();

    @Select("SELECT * FROM wvp_platform WHERE server_gb_id=#{platformGbId}")
    Platform getParentPlatByServerGBId(String platformGbId);

    @Select("SELECT * FROM wvp_platform WHERE id=#{id}")
    Platform query(int id);

    @Update("UPDATE wvp_platform SET status=#{online} WHERE server_gb_id=#{platformGbID}" )
    int updateStatus(@Param("platformGbID") String platformGbID, @Param("online") boolean online);

    @Select("SELECT * FROM wvp_platform WHERE enable=true")
    List<Platform> queryEnablePlatformList();

}
