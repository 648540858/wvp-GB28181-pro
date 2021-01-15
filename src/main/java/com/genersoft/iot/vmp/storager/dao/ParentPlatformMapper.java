package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储上级平台
 */
@Mapper
@Repository
public interface ParentPlatformMapper {

    @Insert("INSERT INTO parent_platform (enable, name, serverGBId, serverGBDomain, serverIP, serverPort, deviceGBId, deviceIp,  " +
            "            devicePort, username, password, expires, keepTimeout, transport, characterSet, PTZEnable, rtcp, " +
            "            status) " +
            "            VALUES (${enable}, '${name}', '${serverGBId}', '${serverGBDomain}', '${serverIP}', ${serverPort}, '${deviceGBId}', '${deviceIp}', " +
            "            '${devicePort}', '${username}', '${password}', '${expires}', '${keepTimeout}', '${transport}', '${characterSet}', ${PTZEnable}, ${rtcp}, " +
            "            ${status})")
    int addParentPlatform(ParentPlatform parentPlatform);

    @Update("UPDATE parent_platform " +
            "SET enable=#{enable}, " +
            "name=#{name}," +
            "deviceGBId=#{deviceGBId}," +
            "serverGBDomain=#{serverGBDomain}, " +
            "serverIP=#{serverIP}," +
            "serverPort=#{serverPort}, " +
            "deviceIp=#{deviceIp}, " +
            "devicePort=#{devicePort}, " +
            "username=#{username}, " +
            "password=#{password}, " +
            "expires=#{expires}, " +
            "keepTimeout=#{keepTimeout}, " +
            "transport=#{transport}, " +
            "characterSet=#{characterSet}, " +
            "PTZEnable=#{PTZEnable}, " +
            "rtcp=#{rtcp}, " +
            "status=#{status} " +
            "WHERE serverGBId=#{serverGBId}")
    int updateParentPlatform(ParentPlatform parentPlatform);

    @Delete("DELETE FROM parent_platform WHERE serverGBId=#{serverGBId}")
    int delParentPlatform(ParentPlatform parentPlatform);

    @Select("SELECT *,( SELECT count(0) FROM platform_gb_channel pc WHERE pc.platformId = pp.serverGBId) as channelCount FROM parent_platform pp ")
    List<ParentPlatform> getParentPlatformList();

    @Select("SELECT * FROM parent_platform WHERE enable=#{enable}")
    List<ParentPlatform> getEnableParentPlatformList(boolean enable);

    @Select("SELECT * FROM parent_platform WHERE serverGBId=#{platformGbId}")
    ParentPlatform getParentPlatById(String platformGbId);

    @Update("UPDATE parent_platform SET status=false" )
    void outlineForAllParentPlatform();
}
