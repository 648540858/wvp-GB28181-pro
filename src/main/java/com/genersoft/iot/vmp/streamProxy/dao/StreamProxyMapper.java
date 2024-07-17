package com.genersoft.iot.vmp.streamProxy.dao;

import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface StreamProxyMapper {

    @Insert("INSERT INTO wvp_stream_proxy (type, name, app, stream,media_server_id, src_url, " +
            "timeout, ffmpeg_cmd_key, rtsp_type, enable_audio, enable_mp4, enable, pulling, stream_key, " +
            "enable_remove_none_reader, enable_disable_none_reader, create_time) VALUES" +
            "(#{type}, #{name}, #{app}, #{stream}, #{mediaServerId}, #{srcUrl}, " +
            "#{timeout}, #{ffmpegCmdKey}, #{rtspType}, #{enableAudio}, #{enableMp4}, #{enable}, #{pulling}, #{streamKey}, " +
            "#{enableRemoveNoneReader}, #{enableDisableNoneReader}, #{createTime} )")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(StreamProxy streamProxyDto);

    @Update("UPDATE wvp_stream_proxy " +
            "SET type=#{type}, " +
            "app=#{app}," +
            "stream=#{stream}," +
            "name=#{name}," +
            "app=#{app}," +
            "stream=#{stream}," +
            "url=#{url}, " +
            "media_server_id=#{mediaServerId}, " +
            "src_url=#{srcUrl}," +
            "timeout=#{timeout}, " +
            "ffmpeg_cmd_key=#{ffmpegCmdKey}, " +
            "rtsp_type=#{rtspType}, " +
            "enable_audio=#{enableAudio}, " +
            "enable=#{enable}, " +
            "pulling=#{pulling}, " +
            "stream_key=#{streamKey}, " +
            "enable_remove_none_reader=#{enableRemoveNoneReader}, " +
            "enable_disable_none_reader=#{enableDisableNoneReader}, " +
            "enable_mp4=#{enableMp4} " +
            "WHERE id=#{id}")
    int update(StreamProxy streamProxyDto);

    @Delete("DELETE FROM wvp_stream_proxy WHERE app=#{app} AND stream=#{stream}")
    int delByAppAndStream(String app, String stream);

    @Select("SELECT " +
            " st.*, " +
            " st.id as stream_proxy_id, " +
            " wdc.*, " +
            " wdc.id as gb_id" +
            " FROM wvp_stream_proxy st " +
            " LEFT join wvp_device_channel wdc " +
            " on st.id = wdc.stream_proxy_id " +
            " WHERE " +
            " 1=1 " +
            " <if test='query != null'> AND (st.app LIKE concat('%',#{query},'%') OR st.stream LIKE concat('%',#{query},'%') " +
            " OR wdc.gb_device_id LIKE concat('%',#{query},'%') OR wdc.gb_name LIKE concat('%',#{query},'%'))</if> " +
            " <if test='pulling == true' > AND st.pulling=1</if>" +
            " <if test='pulling == false' > AND st.pulling=0 </if>" +
            " <if test='mediaServerId != null' > AND st.media_server_id=#{mediaServerId} </if>" +
            "order by st.create_time desc")
    List<StreamProxy> selectAll(@Param("query") String query, @Param("pushing") Boolean pushing, @Param("mediaServerId") String mediaServerId);

    @Select("SELECT " +
            " st.*, " +
            " st.id as stream_proxy_id, " +
            " wdc.*, " +
            " wdc.id as gb_id" +
            " FROM wvp_stream_proxy st " +
            " LEFT join wvp_device_channel wdc " +
            " on st.id = wdc.stream_proxy_id " +
            " WHERE st.app=#{app} AND st.stream=#{stream} order by st.create_time desc")
    StreamProxy selectOneByAppAndStream(@Param("app") String app, @Param("stream") String stream);

    @Select("SELECT " +
            " st.*, " +
            " st.id as stream_proxy_id, " +
            " wdc.*, " +
            " wdc.id as gb_id" +
            " FROM wvp_stream_proxy st " +
            " LEFT join wvp_device_channel wdc " +
            " on st.id = wdc.stream_proxy_id " +
            "WHERE st.enable=#{enable} and st.media_server_id= #{id} order by st.create_time desc")
    List<StreamProxy> selectForEnableInMediaServer(@Param("id")  String id, @Param("enable") boolean enable);


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
}
