package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface StreamProxyMapper {

    @Insert("INSERT INTO stream_proxy (type, app, stream,mediaServerId, url, src_url, dst_url, " +
            "timeout_ms, ffmpeg_cmd_key, rtp_type, enable_hls, enable_mp4, enable, createTime) VALUES" +
            "('${type}','${app}', '${stream}', '${mediaServerId}','${url}', '${src_url}', '${dst_url}', " +
            "'${timeout_ms}', '${ffmpeg_cmd_key}', '${rtp_type}', ${enable_hls}, ${enable_mp4}, ${enable}, '${createTime}' )")
    int add(StreamProxyItem streamProxyDto);

    @Update("UPDATE stream_proxy " +
            "SET type=#{type}, " +
            "app=#{app}," +
            "stream=#{stream}," +
            "url=#{url}, " +
            "mediaServerId=#{mediaServerId}, " +
            "src_url=#{src_url}," +
            "dst_url=#{dst_url}, " +
            "timeout_ms=#{timeout_ms}, " +
            "ffmpeg_cmd_key=#{ffmpeg_cmd_key}, " +
            "rtp_type=#{rtp_type}, " +
            "enable_hls=#{enable_hls}, " +
            "enable=#{enable}, " +
            "enable_mp4=#{enable_mp4} " +
            "WHERE app=#{app} AND stream=#{stream}")
    int update(StreamProxyItem streamProxyDto);

    @Delete("DELETE FROM stream_proxy WHERE app=#{app} AND stream=#{stream}")
    int del(String app, String stream);

    @Select("<script>" +
            "SELECT st.*, pgs.gbId, pgs.name, pgs.longitude, pgs.latitude FROM stream_proxy st LEFT JOIN gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream where 1=1" +
            " <if test=\"query != null\"> AND (" +
            "st.app LIKE '%${query}%' " +
            "OR pgs.name LIKE '%${query}%' " +
            "OR pgs.gbId LIKE '%${query}%' " +
            "OR st.stream LIKE '%${query}%' " +
            "OR st.mediaServerId LIKE '%${query}%' " +
            "OR st.url LIKE '%${query}%' " +
            "OR st.src_url LIKE '%${query}%' " +
            "OR st.dst_url LIKE '%${query}%' " +
            "OR pgs.streamType LIKE '%${query}%')</if>" +
            "<if test=\"enable != null\"> AND st.enable = ${enable} </if> " +
            " order by st.createTime desc </script>")
    List<StreamProxyItem> selectAll(String query, Boolean enable);

    @Select("SELECT st.*, pgs.gbId, pgs.name, pgs.longitude, pgs.latitude FROM stream_proxy st LEFT JOIN gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream WHERE st.enable=${enable} order by st.createTime desc")
    List<StreamProxyItem> selectForEnable(boolean enable);

    @Select("SELECT st.*, pgs.gbId, pgs.name, pgs.longitude, pgs.latitude FROM stream_proxy st LEFT JOIN gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream WHERE st.app=#{app} AND st.stream=#{stream} order by st.createTime desc")
    StreamProxyItem selectOne(String app, String stream);

    @Select("SELECT st.*, pgs.gbId, pgs.name, pgs.longitude, pgs.latitude FROM stream_proxy st " +
            "LEFT JOIN gb_stream pgs on st.app = pgs.app AND st.stream = pgs.stream " +
            "WHERE st.enable=${enable} and st.mediaServerId = '${id}' order by st.createTime desc")
    List<StreamProxyItem> selectForEnableInMediaServer(String id, boolean enable);
}
