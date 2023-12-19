package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPush;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.StreamPushItemFromRedis;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface StreamPushMapper {

    @Insert("INSERT INTO wvp_stream_push (app, stream, total_reader_count, " +
            "push_time, alive_second, media_server_id, update_time, create_time, push_ing, self) VALUES" +
            "(#{app}, #{stream}, #{totalReaderCount}, " +
            "#{pushTime}, #{aliveSecond}, #{mediaServerId} , #{updateTime} , #{createTime}, " +
            "#{pushIng}, #{self} )")
    int add(StreamPush streamPushItem);


    @Update(value = {" <script>" +
            "UPDATE wvp_stream_push " +
            "SET update_time=#{updateTime}" +
            "<if test=\"mediaServerId != null\">, media_server_id=#{mediaServerId}</if>" +
            "<if test=\"totalReaderCount != null\">, total_reader_count=#{totalReaderCount}</if>" +
            "<if test=\"pushTime != null\">, push_time=#{pushTime}</if>" +
            "<if test=\"aliveSecond != null\">, alive_second=#{aliveSecond}</if>" +
            "<if test=\"pushIng != null\">, push_ing=#{pushIng}</if>" +
            "<if test=\"self != null\">, self=#{self}</if>" +
            "WHERE app=#{app} AND stream=#{stream}"+
            " </script>"})
    int update(StreamPush streamPushItem);

    @Delete("DELETE FROM wvp_stream_push WHERE app=#{app} AND stream=#{stream}")
    int del(String app, String stream);

    @Delete("<script> "+
            "DELETE sp FROM wvp_stream_push sp where " +
            "<foreach collection='streamPushItems' item='item' separator='or'>" +
            "(sp.app=#{item.app} and sp.stream=#{item.stream} and sp.gb_id is null) " +
            "</foreach>" +
            "</script>")
    int delAllWithoutGBId(List<StreamPush> streamPushItems);

    @Delete("<script> "+
            "DELETE FROM wvp_stream_push where " +
            "<foreach collection='streamPushItems' item='item' separator='or'>" +
            "(app=#{item.app} and stream=#{item.stream}) " +
            "</foreach>" +
            "</script>")
    int delAll(List<StreamPush> streamPushItems);

    @Delete("<script> "+
            "DELETE FROM wvp_stream_push where " +
            "<foreach collection='gbStreams' item='item' separator='or'>" +
            "(app=#{item.app} and stream=#{item.stream}) " +
            "</foreach>" +
            "</script>")
    int delAllForGbStream(List<GbStream> gbStreams);


    @Select(value = {" <script>" +
            "SELECT " +
            "* " +
            "from " +
            "wvp_stream_push " +
            "WHERE " +
            "1=1 " +
            " <if test='query != null'> AND (app LIKE concat('%',#{query},'%') OR stream LIKE concat('%',#{query},'%') OR gb_id LIKE concat('%',#{query},'%') OR name LIKE concat('%',#{query},'%'))</if> " +
            " <if test='pushing == true' > AND (gb_id is null OR push_ing=1)</if>" +
            " <if test='pushing == false' > AND (push_ing is null OR push_ing=0) </if>" +
            " <if test='mediaServerId != null' > AND media_server_id=#{mediaServerId} </if>" +
            "order by create_time desc" +
            " </script>"})
    List<StreamPush> selectAllForList(@Param("query") String query, @Param("pushing") Boolean pushing, @Param("mediaServerId") String mediaServerId);

    @Select("SELECT * from wvp_stream_push order by create_time desc")
    List<StreamPush> selectAll();

    @Select("SELECT * from wvp_stream_push WHERE app=#{app} AND stream=#{stream}")
    StreamPush selectOne(@Param("app") String app, @Param("stream") String stream);

    @Insert("<script>"  +
            "Insert INTO wvp_stream_push (app, stream, total_reader_count, " +
            "create_time, alive_second, media_server_id, status, push_ing) " +
            "VALUES <foreach collection='streamPushItems' item='item' index='index' separator=','>" +
            "( #{item.app}, #{item.stream}, #{item.totalReaderCount}, " +
            "#{item.createTime}, #{item.aliveSecond}, #{item.mediaServerId}, #{item.status} ," +
            " #{item.pushIng} )" +
            " </foreach>" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int addAll(List<StreamPush> streamPushItems);

    @Delete("DELETE FROM wvp_stream_push")
    void clear();

    @Delete(
            "delete" +
            " from wvp_stream_push " +
            " where media_server_id = #{mediaServerId}  and common_gb_channel_id = 0"
            )
    void deleteWithoutGBId(String mediaServerId);

    @Select("SELECT * FROM wvp_stream_push WHERE media_server_id=#{mediaServerId}")
    List<StreamPush> selectAllByMediaServerId(String mediaServerId);

    @Select("SELECT sp.* FROM wvp_stream_push sp WHERE sp.media_server_id=#{mediaServerId} and sp.gb_id is null")
    List<StreamPush> selectAllByMediaServerIdWithOutGbID(String mediaServerId);

    @Update("UPDATE wvp_stream_push " +
            "SET status=#{status} " +
            "WHERE app=#{app} AND stream=#{stream}")
    int updateStatus(@Param("app") String app, @Param("stream") String stream, @Param("status") boolean status);

    @Update("UPDATE wvp_stream_push " +
            "SET push_ing=#{pushIng} " +
            "WHERE app=#{app} AND stream=#{stream}")
    int updatePushStatus(@Param("app") String app, @Param("stream") String stream, @Param("pushIng") boolean pushIng);

    @Update("UPDATE wvp_stream_push " +
            "SET status=#{status} " +
            "WHERE media_server_id=#{mediaServerId}")
    void updateStatusByMediaServerId(@Param("mediaServerId") String mediaServerId, @Param("status") boolean status);

    @Update("<script> "+
            "UPDATE wvp_stream_push SET status=0  where id in (" +
            "<foreach collection='offlineStreams' item='item' separator=','>" +
            "#{item.id} " +
            "</foreach>" +
            ")</script>")
    void offline(List<StreamPush> offlineStreams);

    @Update("<script> "+
            "UPDATE wvp_stream_push SET status=1  where (app, stream) in (" +
            "<foreach collection='onlineStreams' item='item' separator=','>" +
            "(#{item.app}, #{item.stream}) " +
            "</foreach>" +
            ")</script>")
    void online(List<StreamPushItemFromRedis> onlineStreams);

    @Select("SELECT common_gb_channel_id FROM wvp_stream_push gb_id > 0")
    List<Integer> getOnlinePusherForGb();

    @Update("UPDATE wvp_stream_push SET status=0")
    void setAllStreamOffline();

    @MapKey("key")
    @Select("SELECT CONCAT(wsp.app,wsp.stream) as keyId, wsp.* from wvp_stream_push as wsp ")
    Map<String, StreamPush> getAllAppAndStream();

    @Select("select count(1) from wvp_stream_push ")
    int getAllCount();

    @Select(value = {" <script>" +
            " <if test='pushIngAsOnline == true'> select count(1) from wvp_stream_push where push_ing = true </if>" +
            " <if test='pushIngAsOnline == false'> select count(1)from wvp_stream_push where status = true  </if>" +
            " </script>"})
    int getAllOnline(Boolean usePushingAsStatus);

    @Select("<script> " +
            "select * from wvp_stream_push where (app, stream) in " +
            "<foreach collection='streamPushItems' item='item' separator=','>" +
            "(#{item.app}, #{item.stream}) " +
            "</foreach>" +
            "</script>")
    List<StreamPush> getListIn(@Param("streamPushItems") List<StreamPushItemFromRedis> streamPushItems);

    @Select("select* from wvp_stream_push where id = #{id}")
    StreamPush query(@Param("id") Integer id);

    @Update({"<script>" +
            "<foreach collection='gpsMsgInfoList' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_stream_push" +
            " SET longitude = #{item.lng}, latitude= #{item.lat}" +
            " WHERE gb_id=#{item.id}" +
            "</foreach>" +
            "</script>"})
    void updateStreamGPS(List<GPSMsgInfo> gpsMsgInfoList);
}
