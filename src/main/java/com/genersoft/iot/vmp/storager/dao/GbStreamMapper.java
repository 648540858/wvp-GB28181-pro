package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface GbStreamMapper {

    @Insert("INSERT INTO gb_stream (app, stream, gbId, name, " +
            "longitude, latitude, streamType, mediaServerId, status) VALUES" +
            "('${app}', '${stream}', '${gbId}', '${name}', " +
            "'${longitude}', '${latitude}', '${streamType}', " +
            "'${mediaServerId}', ${status})")
    int add(GbStream gbStream);

    @Update("UPDATE gb_stream " +
            "SET app=#{app}," +
            "stream=#{stream}," +
            "gbId=#{gbId}," +
            "name=#{name}," +
            "streamType=#{streamType}," +
            "longitude=#{longitude}, " +
            "latitude=#{latitude}," +
            "mediaServerId=#{mediaServerId}," +
            "status=${status} " +
            "WHERE app=#{app} AND stream=#{stream}")
    int update(GbStream gbStream);

    @Delete("DELETE FROM gb_stream WHERE app=#{app} AND stream=#{stream}")
    int del(String app, String stream);

    @Select("SELECT gs.*, pgs.platformId FROM gb_stream gs LEFT JOIN  platform_gb_stream pgs ON gs.app = pgs.app AND gs.stream = pgs.stream")
    List<GbStream> selectAll();

    @Select("SELECT * FROM gb_stream WHERE app=#{app} AND stream=#{stream}")
    StreamProxyItem selectOne(String app, String stream);

    @Select("SELECT * FROM gb_stream WHERE gbId=#{gbId}")
    List<GbStream> selectByGBId(String gbId);

    @Select("SELECT gs.*, pgs.platformId FROM gb_stream gs " +
            "LEFT JOIN platform_gb_stream pgs ON gs.app = pgs.app AND gs.stream = pgs.stream " +
            "WHERE gs.gbId = '${gbId}' AND pgs.platformId = '${platformId}'")
    List<GbStream> queryStreamInPlatform(String platformId, String gbId);

    @Select("SELECT gs.*, pgs.platformId FROM gb_stream gs " +
            "LEFT JOIN platform_gb_stream pgs ON gs.app = pgs.app AND gs.stream = pgs.stream " +
            "WHERE pgs.platformId = '${platformId}'")
    List<GbStream> queryGbStreamListInPlatform(String platformId);

    @Update("UPDATE gb_stream " +
            "SET status=${status} " +
            "WHERE app=#{app} AND stream=#{stream}")
    int setStatus(String app, String stream, boolean status);

    @Select("SELECT gs.*, pgs.platformId FROM gb_stream gs LEFT JOIN  platform_gb_stream pgs ON gs.app = pgs.app AND gs.stream = pgs.stream WHERE mediaServerId=#{mediaServerId} ")
    List<GbStream> selectAllByMediaServerId(String mediaServerId);
}
