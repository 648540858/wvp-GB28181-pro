package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyDto;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface StreamProxyMapper {

    @Insert("INSERT INTO stream_proxy (type, app, stream, url, src_url, dst_url, " +
            "timeout_ms, ffmpeg_cmd_key, rtp_type, enable_hls, enable_mp4, enable) VALUES" +
            "('${type}','${app}', '${stream}', '${url}', '${src_url}', '${dst_url}', " +
            "'${timeout_ms}', '${ffmpeg_cmd_key}', '${rtp_type}', ${enable_hls}, ${enable_mp4}, ${enable} )")
    int add(StreamProxyDto streamProxyDto);

    @Update("UPDATE stream_proxy " +
            "SET type=#{type}, " +
            "app=#{app}," +
            "stream=#{stream}," +
            "url=#{url}, " +
            "src_url=#{src_url}," +
            "dst_url=#{dst_url}, " +
            "timeout_ms=#{timeout_ms}, " +
            "ffmpeg_cmd_key=#{ffmpeg_cmd_key}, " +
            "rtp_type=#{rtp_type}, " +
            "enable_hls=#{enable_hls}, " +
            "enable=#{enable}, " +
            "enable_mp4=#{enable_mp4} " +
            "WHERE app=#{app} AND stream=#{stream}")
    int update(StreamProxyDto streamProxyDto);

    @Delete("DELETE FROM stream_proxy WHERE app=#{app} AND stream=#{stream}")
    int del(String app, String stream);

    @Select("SELECT * FROM stream_proxy")
    List<StreamProxyDto> selectAll();

    @Select("SELECT * FROM stream_proxy WHERE enable=${enable}")
    List<StreamProxyDto> selectForEnable(boolean enable);

    @Select("SELECT * FROM stream_proxy WHERE app=#{app} AND stream=#{stream}")
    StreamProxyDto selectOne(String app, String stream);
}
