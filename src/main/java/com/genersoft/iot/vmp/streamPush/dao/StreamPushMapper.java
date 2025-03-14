package com.genersoft.iot.vmp.streamPush.dao;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.service.bean.StreamPushItemFromRedis;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
@Repository
public interface StreamPushMapper {

    Integer dataType = ChannelDataType.GB28181.value;

    @Insert("INSERT INTO wvp_stream_push (app, stream, media_server_id, server_id, push_time,  update_time, create_time, pushing, start_offline_push) VALUES" +
            "(#{app}, #{stream}, #{mediaServerId} , #{serverId} , #{pushTime} ,#{updateTime}, #{createTime}, #{pushing}, #{startOfflinePush})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(StreamPush streamPushItem);


    @Update(value = {" <script>" +
            "UPDATE wvp_stream_push " +
            "SET update_time=#{updateTime}" +
            "<if test=\"app != null\">, app=#{app}</if>" +
            "<if test=\"stream != null\">, stream=#{stream}</if>" +
            "<if test=\"mediaServerId != null\">, media_server_id=#{mediaServerId}</if>" +
            "<if test=\"serverId != null\">, server_id=#{serverId}</if>" +
            "<if test=\"pushTime != null\">, push_time=#{pushTime}</if>" +
            "<if test=\"pushing != null\">, pushing=#{pushing}</if>" +
            "<if test=\"startOfflinePush != null\">, start_offline_push=#{startOfflinePush}</if>" +
            "WHERE id = #{id}"+
            " </script>"})
    int update(StreamPush streamPushItem);

    @Delete("DELETE FROM wvp_stream_push WHERE id=#{id}")
    int del(@Param("id") int id);

    @Select(value = {" <script>" +
            " SELECT " +
            " st.*, " +
            " st.id as data_device_id, " +
            " wdc.*, " +
            " wdc.id as gb_id" +
            " from " +
            " wvp_stream_push st " +
            " LEFT join wvp_device_channel wdc " +
            " on wdc.data_type = 2 and st.id = wdc.data_device_id " +
            " WHERE " +
            " 1=1 " +
            " <if test='query != null'> AND (st.app LIKE concat('%',#{query},'%') escape '/' OR st.stream LIKE concat('%',#{query},'%') escape '/' " +
            " OR wdc.gb_device_id LIKE concat('%',#{query},'%') escape '/' OR wdc.gb_name LIKE concat('%',#{query},'%') escape '/')</if> " +
            " <if test='pushing == true' > AND st.pushing=1</if>" +
            " <if test='pushing == false' > AND st.pushing=0 </if>" +
            " <if test='mediaServerId != null' > AND st.media_server_id=#{mediaServerId} </if>" +
            " order by st.create_time desc" +
            " </script>"})
    List<StreamPush> selectAll(@Param("query") String query, @Param("pushing") Boolean pushing, @Param("mediaServerId") String mediaServerId);

    @Select("SELECT st.*, st.id as data_device_id, wdc.*, wdc.id as gb_id FROM wvp_stream_push st LEFT join wvp_device_channel wdc on  wdc.data_type = 2 and st.id = wdc.data_device_id WHERE st.app=#{app} AND st.stream=#{stream}")
    StreamPush selectByAppAndStream(@Param("app") String app, @Param("stream") String stream);

    @Insert("<script>"  +
            "Insert INTO wvp_stream_push ( " +
            " app, stream, media_server_id, server_id, push_time,  update_time, create_time, pushing, start_offline_push) " +
            " VALUES <foreach collection='streamPushItems' item='item' index='index' separator=','>" +
            " ( #{item.app}, #{item.stream}, #{item.mediaServerId},#{item.serverId} ,#{item.pushTime}, #{item.updateTime}, #{item.createTime}, #{item.pushing}, #{item.startOfflinePush} )" +
            " </foreach>" +
            " </script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int addAll(List<StreamPush> streamPushItems);

    @Select("SELECT st.*, st.id as data_device_id, wdc.*, wdc.id as gb_id FROM wvp_stream_push st LEFT join wvp_device_channel wdc on wdc.data_type = 2 and st.id = wdc.data_device_id WHERE st.media_server_id=#{mediaServerId}")
    List<StreamPush> selectAllByMediaServerId(String mediaServerId);

    @Select("SELECT st.*, st.id as data_device_id, wdc.*, wdc.id as gb_id FROM wvp_stream_push st LEFT join wvp_device_channel wdc on wdc.data_type = 2 and st.id = wdc.data_device_id WHERE st.media_server_id=#{mediaServerId} and wdc.gb_device_id is null")
    List<StreamPush> selectAllByMediaServerIdWithOutGbID(String mediaServerId);

    @Update("UPDATE wvp_stream_push " +
            "SET pushing=#{pushing}, server_id=#{serverId}, media_server_id=#{mediaServerId} " +
            "WHERE id=#{id}")
    int updatePushStatus(StreamPush streamPush);

    @Select("<script> "+
            "SELECT st.*, st.id as data_device_id, wdc.*, wdc.id as gb_id FROM wvp_stream_push st LEFT join wvp_device_channel wdc on wdc.data_type = 2 and st.id = wdc.data_device_id " +
            "where (st.app, st.stream) in (" +
            "<foreach collection='offlineStreams' item='item' separator=','>" +
            "(#{item.app}, #{item.stream}) " +
            "</foreach>" +
            ")</script>")
    List<StreamPush> getListFromRedis(List<StreamPushItemFromRedis> offlineStreams);


    @Select("SELECT CONCAT(app,stream) from wvp_stream_push")
    List<String> getAllAppAndStream();

    @Select("select count(1) from wvp_stream_push ")
    int getAllCount();

    @Select(value = {" <script>" +
            " select count(1) from wvp_stream_push where pushing = true" +
            " </script>"})
    int getAllPushing(Boolean usePushingAsStatus);

    @MapKey("uniqueKey")
    @Select("SELECT CONCAT(wsp.app, wsp.stream) as unique_key, wsp.*, wsp.* , wdc.id as gb_id " +
            " from wvp_stream_push wsp " +
            " LEFT join wvp_device_channel wdc on wdc.data_type = 2 and wsp.id = wdc.data_device_id")
    Map<String, StreamPush> getAllAppAndStreamMap();


    @MapKey("gbDeviceId")
    @Select("SELECT wdc.gb_device_id, wsp.id as data_device_id, wsp.*, wsp.* , wdc.id as gb_id " +
            " from wvp_stream_push wsp " +
            " LEFT join wvp_device_channel wdc on wdc.data_type = 2 and wsp.id = wdc.data_device_id")
    Map<String, StreamPush> getAllGBId();

    @Select("SELECT st.*, st.id as data_device_id, wdc.*, wdc.id as gb_id FROM wvp_stream_push st LEFT join wvp_device_channel wdc on wdc.data_type = 2 and st.id = wdc.data_device_id WHERE st.id=#{id}")
    StreamPush queryOne(@Param("id") int id);

    @Select("<script> "+
            "SELECT st.*, st.id as data_device_id, wdc.*, wdc.id as gb_id FROM wvp_stream_push st LEFT join wvp_device_channel wdc on wdc.data_type = 2 and st.id = wdc.data_device_id " +
            " where st.id in (" +
            " <foreach collection='ids' item='item' separator=','>" +
            " #{item} " +
            " </foreach>" +
            " )</script>")
    List<StreamPush> selectInSet(Set<Integer> ids);

    @Delete("<script> "+
            "DELETE FROM wvp_stream_push WHERE" +
            " id in (" +
            "<foreach collection='streamPushList' item='item' separator=','>" +
            " #{item.id} " +
            "</foreach>" +
            ")</script>")
    void batchDel(List<StreamPush> streamPushList);


    @Update({"<script>" +
            "<foreach collection='streamPushItemForUpdate' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_stream_push" +
            " SET update_time=#{item.updateTime}" +
            ", app=#{item.app}" +
            ", stream=#{item.stream}" +
            ", media_server_id=#{item.mediaServerId}" +
            ", server_id=#{item.serverId}" +
            ", push_time=#{item.pushTime}" +
            ", pushing=#{item.pushing}" +
            ", start_offline_push=#{item.startOfflinePush}" +
            " WHERE id=#{item.id}" +
            "</foreach>" +
            "</script>"})
    int batchUpdate(List<StreamPush> streamPushItemForUpdate);
}
