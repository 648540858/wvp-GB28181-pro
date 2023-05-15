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
            "(#{type}, #{name}, #{app}, #{stream}, #{mediaServerId}, #{url}, #{src_url}, #{dst_url}, " +
            "#{timeout_ms}, #{ffmpeg_cmd_key}, #{rtp_type}, #{enable_audio}, #{enable_mp4}, #{enable}, #{status}, " +
            "#{enable_remove_none_reader}, #{enable_disable_none_reader}, #{createTime} )")
    int add(StreamProxyItem streamProxyDto);

    @Update("UPDATE wvp_stream_proxy " +
            "SET type=#{type}, " +
            "name=#{name}," +
            "app=#{app}," +
            "stream=#{stream}," +
            "url=#{url}, " +
            "media_server_id=#{mediaServerId}, " +
            "src_url=#{src_url}," +
            "dst_url=#{dst_url}, " +
            "timeout_ms=#{timeout_ms}, " +
            "ffmpeg_cmd_key=#{ffmpeg_cmd_key}, " +
            "rtp_type=#{rtp_type}, " +
            "enable_audio=#{enable_audio}, " +
            "enable=#{enable}, " +
            "status=#{status}, " +
            "enable_remove_none_reader=#{enable_remove_none_reader}, " +
            "enable_disable_none_reader=#{enable_disable_none_reader}, " +
            "enable_mp4=#{enable_mp4} " +
            "WHERE app=#{app} AND stream=#{stream}")
    int update(StreamProxyItem streamProxyDto);

    @Delete("DELETE FROM wvp_stream_proxy WHERE app=#{app} AND stream=#{stream}")
    int del(String app, String stream);

    @Select("SELECT st.*, pgs.gb_id, pgs.name, pgs.longitude, pgs.latitude FROM wvp_stream_proxy st LEFT join wvp_gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream order by st.create_time desc")
    List<StreamProxyItem> selectAll();

    @Select("SELECT st.*, pgs.gb_id, pgs.name, pgs.longitude, pgs.latitude FROM wvp_stream_proxy st LEFT join wvp_gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream WHERE st.enable=#{enable} order by st.create_time desc")
    List<StreamProxyItem> selectForEnable(boolean enable);

    @Select("SELECT st.*, pgs.gb_id, pgs.name, pgs.longitude, pgs.latitude FROM wvp_stream_proxy st LEFT join wvp_gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream WHERE st.app=#{app} AND st.stream=#{stream} order by st.create_time desc")
    StreamProxyItem selectOne(String app, String stream);

    @Select("SELECT st.*, pgs.gb_id, pgs.name, pgs.longitude, pgs.latitude FROM wvp_stream_proxy st " +
            "LEFT join wvp_gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream " +
            "WHERE st.enable=#{enable} and st.media_server_id= #{id} order by st.create_time desc")
    List<StreamProxyItem> selectForEnableInMediaServer(String id, boolean enable);

    @Select("SELECT st.*, pgs.gb_id, pgs.name, pgs.longitude, pgs.latitude FROM wvp_stream_proxy st " +
            "LEFT join wvp_gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream " +
            "WHERE st.media_server_id= #{id} order by st.create_time desc")
    List<StreamProxyItem> selectInMediaServer(String id);

    @Update("UPDATE wvp_stream_proxy " +
            "SET status=#{status} " +
            "WHERE media_server_id=#{mediaServerId}")
    void updateStatusByMediaServerId(String mediaServerId, boolean status);

    @Update("UPDATE wvp_stream_proxy " +
            "SET status=#{status} " +
            "WHERE app=#{app} AND stream=#{stream}")
    int updateStatus(String app, String stream, boolean status);

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
