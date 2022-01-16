package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface GbStreamMapper {

    @Insert("REPLACE INTO gb_stream (app, stream, gbId, name, " +
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

    @Select("SELECT gs.*, pgs.platformId AS platformId, pgs.catalogId AS catalogId  FROM gb_stream gs LEFT JOIN  platform_gb_stream pgs ON gs.app = pgs.app AND gs.stream = pgs.stream")
    List<GbStream> selectAll();

    @Select("SELECT * FROM gb_stream WHERE app=#{app} AND stream=#{stream}")
    StreamProxyItem selectOne(String app, String stream);

    @Select("SELECT * FROM gb_stream WHERE gbId=#{gbId}")
    List<GbStream> selectByGBId(String gbId);

    @Select("SELECT gs.*, pgs.platformId as platformId, pgs.catalogId as catalogId FROM gb_stream gs " +
            "LEFT JOIN platform_gb_stream pgs ON gs.app = pgs.app AND gs.stream = pgs.stream " +
            "WHERE gs.gbId = '${gbId}' AND pgs.platformId = '${platformId}'")
    GbStream queryStreamInPlatform(String platformId, String gbId);

    @Select("SELECT gs.*, pgs.platformId as platformId, pgs.catalogId as catalogId FROM gb_stream gs " +
            "LEFT JOIN platform_gb_stream pgs ON gs.app = pgs.app AND gs.stream = pgs.stream " +
            "WHERE pgs.platformId = '${platformId}'")
    List<GbStream> queryGbStreamListInPlatform(String platformId);


    @Select("SELECT gs.*, pgs.platformId as platformId, pgs.catalogId as catalogId FROM gb_stream gs  LEFT JOIN platform_gb_stream pgs " +
            "ON  gs.app = pgs.app and gs.stream = pgs.stream WHERE pgs.app is NULL and pgs.stream is NULL")
    List<GbStream> queryStreamNotInPlatform();

    @Update("UPDATE gb_stream " +
            "SET status=${status} " +
            "WHERE app=#{app} AND stream=#{stream}")
    int setStatus(String app, String stream, boolean status);

    @Update("UPDATE gb_stream " +
            "SET status=${status} " +
            "WHERE mediaServerId=#{mediaServerId} ")
    void updateStatusByMediaServerId(String mediaServerId, boolean status);

    @Delete("DELETE FROM gb_stream WHERE streamType=#{type} AND gbId=NULL AND mediaServerId=#{mediaServerId}")
    void deleteWithoutGBId(String type, String mediaServerId);

    @Delete("<script> "+
            "DELETE FROM gb_stream where " +
            "<foreach collection='streamProxyItemList' item='item' separator='or'>" +
            "(app=#{item.app} and stream=#{item.stream}) " +
            "</foreach>" +
            "</script>")
    void batchDel(List<StreamProxyItem> streamProxyItemList);

    @Insert("<script> " +
            "REPLACE into gb_stream " +
            "(app, stream, gbId, name, " +
            "longitude, latitude, streamType, mediaServerId, status)" +
            "values " +
            "<foreach collection='subList' index='index' item='item' separator=','> " +
            "('${item.app}', '${item.stream}', '${item.gbId}', '${item.name}', " +
            "'${item.longitude}', '${item.latitude}', '${item.streamType}', " +
            "'${item.mediaServerId}', ${item.status}) "+
            "</foreach> " +
            "</script>")
    void batchAdd(List<StreamPushItem> subList);

    @Update({"<script>" +
            "<foreach collection='gpsMsgInfos' item='item' separator=';'>" +
            " UPDATE" +
            " gb_stream" +
            " SET longitude=${item.lng}, latitude=${item.lat} " +
            "WHERE gbId=#{item.id}"+
            "</foreach>" +
            "</script>"})
    int updateStreamGPS(List<GPSMsgInfo> gpsMsgInfos);
}
