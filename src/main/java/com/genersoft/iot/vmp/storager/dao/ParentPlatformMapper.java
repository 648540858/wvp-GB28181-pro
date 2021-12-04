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
            "            devicePort, username, password, expires, keepTimeout, transport, characterSet, ptz, rtcp, " +
            "            status, shareAllLiveStream) " +
            "            VALUES (${enable}, '${name}', '${serverGBId}', '${serverGBDomain}', '${serverIP}', ${serverPort}, '${deviceGBId}', '${deviceIp}', " +
            "            '${devicePort}', '${username}', '${password}', '${expires}', '${keepTimeout}', '${transport}', '${characterSet}', ${ptz}, ${rtcp}, " +
            "            ${status}, ${shareAllLiveStream})")
    int addParentPlatform(ParentPlatform parentPlatform);

    @Update("UPDATE parent_platform " +
            "SET enable=#{enable}, " +
            "name=#{name}," +
            "deviceGBId=#{deviceGBId}," +
            "serverGBId=#{serverGBId}, " +
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
            "ptz=#{ptz}, " +
            "rtcp=#{rtcp}, " +
            "status=#{status}, " +
            "shareAllLiveStream=#{shareAllLiveStream} " +
            "WHERE id=#{id}")
    int updateParentPlatform(ParentPlatform parentPlatform);

    @Delete("DELETE FROM parent_platform WHERE serverGBId=#{serverGBId}")
    int delParentPlatform(ParentPlatform parentPlatform);

    @Select("SELECT *, ((SELECT count(0)\n" +
            "              FROM platform_gb_channel pc\n" +
            "              WHERE pc.platformId = pp.serverGBId)\n" +
            "              +\n" +
            "              (SELECT count(0)\n" +
            "              FROM platform_gb_stream pgs\n" +
            "              WHERE pgs.platformId = pp.serverGBId)) as channelCount\n" +
            "FROM parent_platform pp ")
    List<ParentPlatform> getParentPlatformList();

    @Select("SELECT * FROM parent_platform WHERE enable=#{enable}")
    List<ParentPlatform> getEnableParentPlatformList(boolean enable);

    @Select("SELECT * FROM parent_platform WHERE serverGBId=#{platformGbId}")
    ParentPlatform getParentPlatByServerGBId(String platformGbId);

    @Select("SELECT * FROM parent_platform WHERE id=#{id}")
    ParentPlatform getParentPlatById(int id);

    @Update("UPDATE parent_platform SET status=false" )
    int outlineForAllParentPlatform();

    @Update("UPDATE parent_platform SET status=#{online} WHERE serverGBId=#{platformGbID}" )
    int updateParentPlatformStatus(String platformGbID, boolean online);

    @Select("SELECT * FROM parent_platform WHERE shareAllLiveStream=true")
    List<ParentPlatform> selectAllAhareAllLiveStream();
}
