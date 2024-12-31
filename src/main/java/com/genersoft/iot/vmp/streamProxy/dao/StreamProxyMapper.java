package com.genersoft.iot.vmp.streamProxy.dao;

import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.dao.provider.StreamProxyProvider;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface StreamProxyMapper {

    @Insert("INSERT INTO wvp_stream_proxy (type, app, stream,relates_media_server_id, src_url, " +
            "timeout, ffmpeg_cmd_key, rtsp_type, enable_audio, enable_mp4, enable, pulling, " +
            "enable_remove_none_reader, enable_disable_none_reader, server_id, create_time) VALUES" +
            "(#{type}, #{app}, #{stream}, #{relatesMediaServerId}, #{srcUrl}, " +
            "#{timeout}, #{ffmpegCmdKey}, #{rtspType}, #{enableAudio}, #{enableMp4}, #{enable}, #{pulling}, " +
            "#{enableRemoveNoneReader}, #{enableDisableNoneReader}, #{serverId}, #{createTime} )")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(StreamProxy streamProxyDto);

    @Update("UPDATE wvp_stream_proxy " +
            "SET type=#{type}, " +
            "app=#{app}," +
            "stream=#{stream}," +
            "relates_media_server_id=#{relatesMediaServerId}, " +
            "src_url=#{srcUrl}," +
            "timeout=#{timeout}, " +
            "ffmpeg_cmd_key=#{ffmpegCmdKey}, " +
            "rtsp_type=#{rtspType}, " +
            "enable_audio=#{enableAudio}, " +
            "enable=#{enable}, " +
            "pulling=#{pulling}, " +
            "enable_remove_none_reader=#{enableRemoveNoneReader}, " +
            "enable_disable_none_reader=#{enableDisableNoneReader}, " +
            "enable_mp4=#{enableMp4} " +
            "WHERE id=#{id}")
    int update(StreamProxy streamProxyDto);

    @Delete("DELETE FROM wvp_stream_proxy WHERE app=#{app} AND stream=#{stream}")
    int delByAppAndStream(String app, String stream);

    @SelectProvider(type = StreamProxyProvider.class, method = "selectAll")
    List<StreamProxy> selectAll(@Param("query") String query, @Param("pulling") Boolean pulling, @Param("mediaServerId") String mediaServerId);

    @SelectProvider(type = StreamProxyProvider.class, method = "selectOneByAppAndStream")
    StreamProxy selectOneByAppAndStream(@Param("app") String app, @Param("stream") String stream);

    @SelectProvider(type = StreamProxyProvider.class, method = "selectForPushingInMediaServer")
    List<StreamProxy> selectForPushingInMediaServer(@Param("mediaServerId")  String mediaServerId, @Param("enable") boolean enable);


    @Select("select count(1) from wvp_stream_proxy")
    int getAllCount();

    @Select("select count(1) from wvp_stream_proxy where pulling = true")
    int getOnline();

    @Delete("DELETE FROM wvp_stream_proxy WHERE id=#{id}")
    int delete(@Param("id") int id);

    @Delete(value = "<script>" +
            "DELETE FROM wvp_stream_proxy WHERE id in (" +
            "<foreach collection='streamProxiesForRemove' index='index' item='item' separator=','> " +
            "#{item.id}"+
            "</foreach>" +
            ")" +
            "</script>")
    void deleteByList(List<StreamProxy> streamProxiesForRemove);

    @Update("UPDATE wvp_stream_proxy " +
            "SET pulling=true " +
            "WHERE id=#{id}")
    int online(@Param("id") int id);

    @Update("UPDATE wvp_stream_proxy " +
            "SET pulling=false " +
            "WHERE id=#{id}")
    int offline(@Param("id") int id);

    @SelectProvider(type = StreamProxyProvider.class, method = "select")
    StreamProxy select(@Param("id") int id);

    @Update("UPDATE wvp_stream_proxy " +
            " SET pulling=false, media_server_id = null," +
            " stream_key = null " +
            " WHERE id=#{id}")
    void removeStream(@Param("id")int id);

    @Update("UPDATE wvp_stream_proxy " +
            " SET pulling=#{pulling}, media_server_id = #{mediaServerId}, " +
            " stream_key = #{streamKey} " +
            " WHERE id=#{id}")
    void addStream(StreamProxy streamProxy);
}
