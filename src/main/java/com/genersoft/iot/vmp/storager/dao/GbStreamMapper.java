package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface GbStreamMapper {

    @Insert("INSERT INTO wvp_gb_stream (app, stream, gb_id, name, " +
            "longitude, latitude, stream_type,media_server_id,create_time) VALUES" +
            "(#{app}, #{stream}, #{gbId}, #{name}, " +
            "#{longitude}, #{latitude}, #{streamType}, " +
            "#{mediaServerId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "gbStreamId", keyColumn = "gbStreamId")
    int add(GbStream gbStream);

    @Update("UPDATE wvp_gb_stream " +
            "SET app=#{app}," +
            "stream=#{stream}," +
            "gb_id=#{gbId}," +
            "name=#{name}," +
            "stream_type=#{streamType}," +
            "longitude=#{longitude}, " +
            "latitude=#{latitude}," +
            "media_server_id=#{mediaServerId}" +
            "WHERE app=#{app} AND stream=#{stream}")
    int updateByAppAndStream(GbStream gbStream);

    @Update("UPDATE wvp_gb_stream " +
            "SET app=#{app}," +
            "stream=#{stream}," +
            "gb_id=#{gbId}," +
            "name=#{name}," +
            "stream_type=#{streamType}," +
            "longitude=#{longitude}, " +
            "latitude=#{latitude}," +
            "media_server_id=#{mediaServerId}" +
            "WHERE gb_stream_id=#{gbStreamId}")
    int update(GbStream gbStream);

    @Delete("DELETE FROM wvp_gb_stream WHERE app=#{app} AND stream=#{stream}")
    int del(@Param("app") String app, @Param("stream") String stream);

    @Select("<script> "+
            "SELECT gs.* FROM wvp_gb_stream gs " +
            "WHERE " +
            "1=1 " +
            " <if test='catalogId != null'> AND gs.gb_stream_id in" +
            "(select pgs.gb_stream_id from wvp_platform_gb_stream pgs where pgs.platform_id = #{platformId} and pgs.catalog_id=#{catalogId})</if> " +
            " <if test='catalogId == null'> AND gs.gb_stream_id not in" +
            "(select pgs.gb_stream_id from wvp_platform_gb_stream pgs where pgs.platform_id = #{platformId}) </if> " +
            " <if test='query != null'> AND (gs.app LIKE concat('%',#{query},'%') OR gs.stream LIKE concat('%',#{query},'%') OR gs.gb_id LIKE concat('%',#{query},'%') OR gs.name LIKE concat('%',#{query},'%'))</if> " +
            " <if test='mediaServerId != null' > AND gs.media_server_id=#{mediaServerId} </if>" +
            " order by gs.gb_stream_id asc " +
            "</script>")
    List<GbStream> selectAll(@Param("platformId") String platformId, @Param("catalogId") String catalogId, @Param("query") String query, @Param("mediaServerId") String mediaServerId);

    @Select("SELECT * FROM wvp_gb_stream WHERE app=#{app} AND stream=#{stream}")
    GbStream selectOne(@Param("app") String app, @Param("stream") String stream);

    @Select("SELECT * FROM wvp_gb_stream WHERE gb_id=#{gbId}")
    List<GbStream> selectByGBId(String gbId);

    @Select("SELECT gs.*, pgs.platform_id as platform_id, pgs.catalog_id as catalog_id FROM wvp_gb_stream gs " +
            "LEFT JOIN wvp_platform_gb_stream pgs ON gs.gb_stream_id = pgs.gb_stream_id " +
            "WHERE gs.gb_id = #{gbId} AND pgs.platform_id = #{platformId}")
    GbStream queryStreamInPlatform(@Param("platformId") String platformId, @Param("gbId") String gbId);

    @Select("<script> "+
            "select gt.gb_id as channel_id, gt.name, 'wvp-pro' as manufacture,  st.status, gt.longitude, gt.latitude, pc.id as parent_id," +
            "       '1' as register_way, pc.civil_code, 'live' as model, 'wvp-pro' as owner, '0' as parental,'0' as secrecy" +
            " from wvp_gb_stream gt " +
            " left join (" +
            "    select " +
            " <if test='usPushingAsStatus != true'> sp.status as status, </if>" +
            " <if test='usPushingAsStatus == true'> sp.push_ing as status, </if>" +
            "sp.app, sp.stream from wvp_stream_push sp" +
            "    union all" +
            "    select spxy.status, spxy.app, spxy.stream from wvp_stream_proxy spxy" +
            " ) st on st.app = gt.app and st.stream = gt.stream" +
            " left join wvp_platform_gb_stream pgs on gt.gb_stream_id = pgs.gb_stream_id" +
            " left join wvp_platform_catalog pc on pgs.catalog_id = pc.id and pgs.platform_id = pc.platform_id" +
            " where pgs.platform_id=#{platformId}" +
            "</script>")
    List<DeviceChannel> queryGbStreamListInPlatform(String platformId, @Param("usPushingAsStatus") boolean usPushingAsStatus);


    @Select("SELECT gs.* FROM wvp_gb_stream gs left join wvp_platform_gb_stream pgs " +
            "ON gs.gb_stream_id = pgs.gb_stream_id WHERE pgs.gb_stream_id is NULL")
    List<GbStream> queryStreamNotInPlatform();

    @Delete("DELETE FROM wvp_gb_stream WHERE stream_type=#{type} AND gb_id=NULL AND media_server_id=#{mediaServerId}")
    void deleteWithoutGBId(@Param("type") String type, @Param("mediaServerId") String mediaServerId);

    @Delete("<script> "+
            "DELETE FROM wvp_gb_stream where " +
            "<foreach collection='streamProxyItemList' item='item' separator='or'>" +
            "(app=#{item.app} and stream=#{item.stream}) " +
            "</foreach>" +
            "</script>")
    void batchDel(List<StreamProxyItem> streamProxyItemList);

    @Delete("<script> "+
            "DELETE FROM wvp_gb_stream where " +
            "<foreach collection='gbStreams' item='item' separator='or'>" +
            "(app=#{item.app} and stream=#{item.stream}) " +
            "</foreach>" +
            "</script>")
    void batchDelForGbStream(List<GbStream> gbStreams);

    @Insert("<script> " +
            "INSERT into wvp_gb_stream " +
            "(app, stream, gb_id, name, " +
            "longitude, latitude, stream_type,media_server_id,create_time)" +
            "values " +
            "<foreach collection='subList' index='index' item='item' separator=','> " +
            "(#{item.app}, #{item.stream}, #{item.gbId}, #{item.name}, " +
            "#{item.longitude}, #{item.latitude}, #{item.streamType}, " +
            "#{item.mediaServerId}, #{item.createTime}) "+
            "</foreach> " +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "gbStreamId", keyColumn = "gb_stream_id")
    void batchAdd(@Param("subList") List<StreamPushItem> subList);

    @Update({"<script>" +
            "<foreach collection='gpsMsgInfos' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_gb_stream" +
            " SET longitude=#{item.lng}, latitude=#{item.lat} " +
            "WHERE gb_id=#{item.id}"+
            "</foreach>" +
            "</script>"})
    int updateStreamGPS(List<GPSMsgInfo> gpsMsgInfos);

    @Select("<script> "+
                   "SELECT * FROM wvp_gb_stream where " +
                   "<foreach collection='streamPushItems' item='item' separator='or'>" +
                   "(app=#{item.app} and stream=#{item.stream}) " +
                   "</foreach>" +
                   "</script>")
    List<GbStream> selectAllForAppAndStream(List<StreamPushItem> streamPushItems);

    @Update("UPDATE wvp_gb_stream " +
            "SET media_server_id=#{mediaServerId}" +
            "WHERE app=#{app} AND stream=#{stream}")
    void updateMediaServer(String app, String stream, String mediaServerId);

    @Update("<script> "+
                " <foreach collection='list' item='item' index='index' separator=';'>"+
                    "UPDATE wvp_gb_stream " +
                    " SET name=#{item.name},"+
                    " gb_id=#{item.gb_id}"+
                    " WHERE app=#{item.app} and stream=#{item.stream}"+
                "</foreach>"+
            "</script>")
    int updateGbIdOrName(List<StreamPushItem> streamPushItemForUpdate);

    @Select("SELECT status FROM wvp_stream_proxy WHERE app=#{app} AND stream=#{stream}")
    Boolean selectStatusForProxy(@Param("app") String app, @Param("stream") String stream);

    @Select("SELECT status FROM wvp_stream_push WHERE app=#{app} AND stream=#{stream}")
    Boolean selectStatusForPush(@Param("app") String app, @Param("stream") String stream);

}
