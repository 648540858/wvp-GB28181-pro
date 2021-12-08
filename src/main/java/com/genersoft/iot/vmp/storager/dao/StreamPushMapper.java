package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Mapper
@Repository
public interface StreamPushMapper {

    @Insert("INSERT INTO stream_push (app, stream, totalReaderCount, originType, originTypeStr, " +
            "createStamp, aliveSecond, mediaServerId) VALUES" +
            "('${app}', '${stream}', '${totalReaderCount}', '${originType}', '${originTypeStr}', " +
            "'${createStamp}', '${aliveSecond}', '${mediaServerId}' )")
    int add(StreamPushItem streamPushItem);

    @Update("UPDATE stream_push " +
            "SET app=#{app}," +
            "stream=#{stream}," +
            "mediaServerId=#{mediaServerId}," +
            "totalReaderCount=#{totalReaderCount}, " +
            "originType=#{originType}," +
            "originTypeStr=#{originTypeStr}, " +
            "createStamp=#{createStamp}, " +
            "aliveSecond=#{aliveSecond} " +
            "WHERE app=#{app} AND stream=#{stream}")
    int update(StreamPushItem streamPushItem);

    @Delete("DELETE FROM stream_push WHERE app=#{app} AND stream=#{stream}")
    int del(String app, String stream);

    @Delete("<script> "+
            "DELETE FROM stream_push where " +
            "<foreach collection='streamPushItems' item='item' separator='or'>" +
            "(app=#{item.app} and stream=#{item.stream}) " +
            "</foreach>" +
            "</script>")
    int delAll(List<StreamPushItem> streamPushItems);

    @Select("SELECT st.*, pgs.gbId, pgs.status, pgs.name, pgs.longitude, pgs.latitude FROM stream_push st LEFT JOIN gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream")
    List<StreamPushItem> selectAll();

    @Select("SELECT st.*, pgs.gbId, pgs.status, pgs.name, pgs.longitude, pgs.latitude FROM stream_push st LEFT JOIN gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream WHERE st.enable=${enable}")
    List<StreamPushItem> selectForEnable(boolean enable);

    @Select("SELECT st.*, pgs.gbId, pgs.status, pgs.name, pgs.longitude, pgs.latitude FROM stream_push st LEFT JOIN gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream WHERE st.app=#{app} AND st.stream=#{stream}")
    StreamPushItem selectOne(String app, String stream);

    @Insert("<script>"  +
            "INSERT INTO stream_push (app, stream, totalReaderCount, originType, originTypeStr, " +
            "createStamp, aliveSecond, mediaServerId) " +
            "VALUES <foreach collection='streamPushItems' item='item' index='index' >" +
            "( '${item.app}', '${item.stream}', '${item.totalReaderCount}', '${item.originType}', " +
            "'${item.originTypeStr}','${item.createStamp}', '${item.aliveSecond}', '${item.mediaServerId}' )" +
            " </foreach>" +
            "</script>")
    void addAll(List<StreamPushItem> streamPushItems);

    @Delete("DELETE FROM stream_push")
    void clear();

    @Delete("DELETE FROM stream_push WHERE mediaServerId=#{mediaServerId}")
    void deleteWithoutGBId(String mediaServerId);

    @Select("SELECT * FROM stream_push WHERE mediaServerId=#{mediaServerId}")
    List<StreamPushItem> selectAllByMediaServerId(String mediaServerId);

}
