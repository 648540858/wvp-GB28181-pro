package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface StreamProxyMapper {

    @Insert("INSERT INTO wvp_stream_proxy (type, name, app, stream,media_server_id, url, src_url, dst_url, " +
            "timeout_ms, ffmpeg_cmd_key, rtp_type, enable_audio, enable_mp4, enable, status, enable_remove_none_reader, enable_disable_none_reader, create_time) VALUES" +
            "(#{type}, #{name}, #{app}, #{stream}, #{mediaServerId}, #{url}, #{srcUrl}, #{dstUrl}, " +
            "#{timeoutMs}, #{ffmpegCmdKey}, #{rtpType}, #{enableAudio}, #{enableMp4}, #{enable}, #{status}, " +
            "#{enableRemoveNoneReader}, #{enableDisableNoneReader}, #{createTime} )")
    int add(StreamProxyItem streamProxyDto);

    @Update("UPDATE wvp_stream_proxy " +
            "SET type=#{type}, " +
            "name=#{name}," +
            "app=#{app}," +
            "stream=#{stream}," +
            "url=#{url}, " +
            "media_server_id=#{mediaServerId}, " +
            "src_url=#{srcUrl}," +
            "dst_url=#{dstUrl}, " +
            "timeout_ms=#{timeoutMs}, " +
            "ffmpeg_cmd_key=#{ffmpegCmdKey}, " +
            "rtp_type=#{rtpType}, " +
            "enable_audio=#{enableAudio}, " +
            "enable=#{enable}, " +
            "status=#{status}, " +
            "enable_remove_none_reader=#{enableRemoveNoneReader}, " +
            "enable_disable_none_reader=#{enableDisableNoneReader}, " +
            "enable_mp4=#{enableMp4} " +
            "WHERE app=#{app} AND stream=#{stream}")
    int update(StreamProxyItem streamProxyDto);

    @Delete("DELETE FROM wvp_stream_proxy WHERE app=#{app} AND stream=#{stream}")
    int del(String app, String stream);

    @Select("SELECT st.*, pgs.gb_id, pgs.name, pgs.longitude, pgs.latitude FROM wvp_stream_proxy st LEFT join wvp_gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream order by st.create_time desc")
    List<StreamProxyItem> selectAll();

    @Select("SELECT st.*, pgs.gb_id, pgs.name, pgs.longitude, pgs.latitude FROM wvp_stream_proxy st LEFT join wvp_gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream WHERE st.enable=#{enable} order by st.create_time desc")
    List<StreamProxyItem> selectForEnable(boolean enable);

    @Select("SELECT st.*, pgs.gb_id, pgs.name, pgs.longitude, pgs.latitude FROM wvp_stream_proxy st LEFT join wvp_gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream WHERE st.app=#{app} AND st.stream=#{stream} order by st.create_time desc")
    StreamProxyItem selectOne(@Param("app") String app, @Param("stream") String stream);

    @Select("SELECT st.*, pgs.gb_id, pgs.name, pgs.longitude, pgs.latitude FROM wvp_stream_proxy st " +
            "LEFT join wvp_gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream " +
            "WHERE st.enable=#{enable} and st.media_server_id= #{id} order by st.create_time desc")
    List<StreamProxyItem> selectForEnableInMediaServer( @Param("id")  String id, @Param("enable") boolean enable);

    @Select("SELECT st.*, pgs.gb_id, pgs.name, pgs.longitude, pgs.latitude FROM wvp_stream_proxy st " +
            "LEFT join wvp_gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream " +
            "WHERE st.media_server_id= #{id} order by st.create_time desc")
    List<StreamProxyItem> selectInMediaServer(String id);

    @Update("UPDATE wvp_stream_proxy " +
            "SET status=#{status} " +
            "WHERE media_server_id=#{mediaServerId}")
    void updateStatusByMediaServerId(@Param("mediaServerId") String mediaServerId, @Param("status") boolean status);

    @Update("UPDATE wvp_stream_proxy " +
            "SET status=#{status} " +
            "WHERE app=#{app} AND stream=#{stream}")
    int updateStatus(@Param("app") String app, @Param("stream") String stream, @Param("status") boolean status);

    @Delete("DELETE FROM wvp_stream_proxy WHERE enable_remove_none_reader=true AND media_server_id=#{mediaServerId}")
    void deleteAutoRemoveItemByMediaServerId(String mediaServerId);

    @Select("SELECT st.*, pgs.gb_id, pgs.name, pgs.longitude, pgs.latitude FROM wvp_stream_proxy st LEFT join wvp_gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream WHERE st.enable_remove_none_reader=true AND st.media_server_id=#{mediaServerId} order by st.create_time desc")
    List<StreamProxyItem> selectAutoRemoveItemByMediaServerId(String mediaServerId);

    @Select("select count(1) as total, sum(status) as online from wvp_stream_proxy")
    ResourceBaseInfo getOverview();

    @Select("select count(1) from wvp_stream_proxy")

    int getAllCount();

    @Select("select count(1) from wvp_stream_proxy where status = true")
    int getOnline();
}
