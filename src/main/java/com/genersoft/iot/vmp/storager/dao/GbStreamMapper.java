package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
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
            "longitude, latitude, streamType, mediaServerId, createTime) VALUES" +
            "('${app}', '${stream}', '${gbId}', '${name}', " +
            "'${longitude}', '${latitude}', '${streamType}', " +
            "'${mediaServerId}', '${createTime}')")
    @Options(useGeneratedKeys = true, keyProperty = "gbStreamId", keyColumn = "gbStreamId")
    int add(GbStream gbStream);

    @Update("UPDATE gb_stream " +
            "SET app=#{app}," +
            "stream=#{stream}," +
            "gbId=#{gbId}," +
            "name=#{name}," +
            "streamType=#{streamType}," +
            "longitude=#{longitude}, " +
            "latitude=#{latitude}," +
            "mediaServerId=#{mediaServerId}" +
            "WHERE app=#{app} AND stream=#{stream}")
    int updateByAppAndStream(GbStream gbStream);

    @Update("UPDATE gb_stream " +
            "SET app=#{app}," +
            "stream=#{stream}," +
            "gbId=#{gbId}," +
            "name=#{name}," +
            "streamType=#{streamType}," +
            "longitude=#{longitude}, " +
            "latitude=#{latitude}," +
            "mediaServerId=#{mediaServerId}" +
            "WHERE gbStreamId=#{gbStreamId}")
    int update(GbStream gbStream);

    @Delete("DELETE FROM gb_stream WHERE app=#{app} AND stream=#{stream}")
    int del(String app, String stream);

    @Select("<script> "+
            "SELECT gs.* FROM gb_stream gs " +
            "WHERE " +
            "1=1 " +
            " <if test='catalogId != null'> AND gs.gbStreamId in" +
            "(select pgs.gbStreamId from platform_gb_stream pgs where pgs.platformId = #{platformId} and pgs.catalogId=#{catalogId})</if> " +
            " <if test='catalogId == null'> AND gs.gbStreamId not in" +
            "(select pgs.gbStreamId from platform_gb_stream pgs where pgs.platformId = #{platformId}) </if> " +
            " <if test='query != null'> AND (gs.app LIKE '%${query}%' OR gs.stream LIKE '%${query}%' OR gs.gbId LIKE '%${query}%' OR gs.name LIKE '%${query}%')</if> " +
            " <if test='mediaServerId != null' > AND gs.mediaServerId=#{mediaServerId} </if>" +
            " order by gs.gbStreamId asc " +
            "</script>")
    List<GbStream> selectAll(String platformId, String catalogId, String query, String mediaServerId);

    @Select("SELECT * FROM gb_stream WHERE app=#{app} AND stream=#{stream}")
    GbStream selectOne(String app, String stream);

    @Select("SELECT * FROM gb_stream WHERE gbId=#{gbId}")
    List<GbStream> selectByGBId(String gbId);

    @Select("SELECT gs.*, pgs.platformId as platformId, pgs.catalogId as catalogId FROM gb_stream gs " +
            "LEFT JOIN platform_gb_stream pgs ON gs.gbStreamId = pgs.gbStreamId " +
            "WHERE gs.gbId = '${gbId}' AND pgs.platformId = '${platformId}'")
    GbStream queryStreamInPlatform(String platformId, String gbId);

    @Select("select gt.gbId as channelId, gt.name, 'wvp-pro' as manufacture,  st.status, gt.longitude, gt.latitude, pc.id as parentId," +
            "       '1' as registerWay, pc.civilCode, 'live' as model, 'wvp-pro' as owner, '0' as parental,'0' as secrecy" +
            " from gb_stream gt " +
            " left join (" +
            "    select sp.status, sp.app, sp.stream from stream_push sp" +
            "    union all" +
            "    select spxy.status, spxy.app, spxy.stream from stream_proxy spxy" +
            " ) st on st.app = gt.app and st.stream = gt.stream" +
            " left join platform_gb_stream pgs on  gt.gbStreamId = pgs.gbStreamId" +
            " left join platform_catalog pc on pgs.catalogId = pc.id and pgs.platformId = pc.platformId" +
            " where pgs.platformId=#{platformId}")
    List<DeviceChannel> queryGbStreamListInPlatform(String platformId);


    @Select("SELECT gs.* FROM gb_stream gs LEFT JOIN platform_gb_stream pgs " +
            "ON gs.gbStreamId = pgs.gbStreamId WHERE pgs.gbStreamId is NULL")
    List<GbStream> queryStreamNotInPlatform();

    @Delete("DELETE FROM gb_stream WHERE streamType=#{type} AND gbId=NULL AND mediaServerId=#{mediaServerId}")
    void deleteWithoutGBId(String type, String mediaServerId);

    @Delete("<script> "+
            "DELETE FROM gb_stream where " +
            "<foreach collection='streamProxyItemList' item='item' separator='or'>" +
            "(app=#{item.app} and stream=#{item.stream}) " +
            "</foreach>" +
            "</script>")
    void batchDel(List<StreamProxyItem> streamProxyItemList);

    @Delete("<script> "+
            "DELETE FROM gb_stream where " +
            "<foreach collection='gbStreams' item='item' separator='or'>" +
            "(app=#{item.app} and stream=#{item.stream}) " +
            "</foreach>" +
            "</script>")
    void batchDelForGbStream(List<GbStream> gbStreams);

    @Insert("<script> " +
            "INSERT IGNORE into gb_stream " +
            "(app, stream, gbId, name, " +
            "longitude, latitude, streamType, mediaServerId, createTime)" +
            "values " +
            "<foreach collection='subList' index='index' item='item' separator=','> " +
            "('${item.app}', '${item.stream}', '${item.gbId}', '${item.name}', " +
            "'${item.longitude}', '${item.latitude}', '${item.streamType}', " +
            "'${item.mediaServerId}', '${item.createTime}') "+
            "</foreach> " +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "gbStreamId", keyColumn = "gbStreamId")
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

    @Select("<script> "+
                   "SELECT * FROM gb_stream where " +
                   "<foreach collection='streamPushItems' item='item' separator='or'>" +
                   "(app=#{item.app} and stream=#{item.stream}) " +
                   "</foreach>" +
                   "</script>")
    List<GbStream> selectAllForAppAndStream(List<StreamPushItem> streamPushItems);
}
