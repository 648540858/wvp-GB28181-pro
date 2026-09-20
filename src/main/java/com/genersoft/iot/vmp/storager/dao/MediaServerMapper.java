package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Mapper
@Repository
public interface MediaServerMapper {

    @Insert("INSERT INTO wvp_media_server (" +
            "id,"+
            "ip,"+
            "hook_ip,"+
            "sdp_ip,"+
            "stream_ip,"+
            "http_port,"+
            "http_ssl_port,"+
            "rtmp_port,"+
            "rtmp_ssl_port,"+
            "rtp_proxy_port,"+
            "jtt_proxy_port,"+
            "rtsp_port,"+
            "flv_port," +
            "mp4_port," +
            "flv_ssl_port," +
            "ws_flv_port," +
            "ws_flv_ssl_port," +
            "rtsp_ssl_port,"+
            "auto_config,"+
            "secret,"+
            "rtp_enable,"+
            "rtp_port_range,"+
            "send_rtp_port_range,"+
            "record_assist_port,"+
            "record_day,"+
            "record_path,"+
            "default_server,"+
            "type,"+
            "create_time,"+
            "update_time,"+
            "transcode_suffix,"+
            "server_id,"+
            "hook_alive_interval"+
            ") VALUES " +
            "(" +
            "#{id}, " +
            "#{ip}, " +
            "#{hookIp}, " +
            "#{sdpIp}, " +
            "#{streamIp}, " +
            "#{httpPort}, " +
            "#{httpSSlPort}, " +
            "#{rtmpPort}, " +
            "#{rtmpSSlPort}, " +
            "#{rtpProxyPort}, " +
            "#{jttProxyPort}, " +
            "#{rtspPort}, " +
            "#{flvPort}, " +
            "#{mp4Port}, " +
            "#{flvSSLPort}, " +
            "#{wsFlvPort}, " +
            "#{wsFlvSSLPort}, " +
            "#{rtspSSLPort}, " +
            "#{autoConfig}, " +
            "#{secret}, " +
            "#{rtpEnable}, " +
            "#{rtpPortRange}, " +
            "#{sendRtpPortRange}, " +
            "#{recordAssistPort}, " +
            "#{recordDay}, " +
            "#{recordPath}, " +
            "#{defaultServer}, " +
            "#{type}, " +
            "#{createTime}, " +
            "#{updateTime}, " +
            "#{transcodeSuffix}, " +
            "#{serverId}, " +
            "#{hookAliveInterval})")
    int add(MediaServer mediaServerItem);

    @Update(value = {" <script>" +
            "UPDATE wvp_media_server " +
            "SET update_time=#{updateTime}, transcode_suffix=#{transcodeSuffix} " +
            ", ip=#{ip}, hook_ip=#{hookIp}, sdp_ip=#{sdpIp}, stream_ip=#{streamIp}, http_port=#{httpPort}" +
            ", http_ssl_port=#{httpSSlPort}, rtmp_port=#{rtmpPort}, rtmp_ssl_port=#{rtmpSSlPort}" +
            ", rtp_proxy_port=#{rtpProxyPort}, jtt_proxy_port=#{jttProxyPort}, rtsp_port=#{rtspPort}" +
            ", rtsp_ssl_port=#{rtspSSLPort}, flv_port=#{flvPort}, mp4_port=#{mp4Port}" +
            ", flv_ssl_port=#{flvSSLPort}, ws_flv_port=#{wsFlvPort}, ws_flv_ssl_port=#{wsFlvSSLPort}" +
            ", auto_config=#{autoConfig}, rtp_enable=#{rtpEnable}, rtp_port_range=#{rtpPortRange}" +
            ", send_rtp_port_range=#{sendRtpPortRange}, secret=#{secret}, record_assist_port=#{recordAssistPort}" +
            ", hook_alive_interval=#{hookAliveInterval}, record_day=#{recordDay}, record_path=#{recordPath}" +
            ", server_id=#{serverId}, type=#{type}" +
            " WHERE id=#{id}"+
            " </script>"})
    int update(MediaServer mediaServerItem);

    @Select("SELECT * FROM wvp_media_server WHERE id=#{id}")
    MediaServer queryOne(@Param("id") String id);

    @Select("SELECT * FROM wvp_media_server WHERE id=#{id} and server_id = #{serverId}")
    MediaServer queryOneWithServerId(@Param("id") String id, @Param("serverId") String serverId);

    @Select("SELECT * FROM wvp_media_server where server_id = #{serverId}")
    List<MediaServer> queryAll(@Param("serverId") String serverId);

    @Select("SELECT * FROM wvp_media_server where default_server=false AND server_id = #{serverId}")
    List<MediaServer> queryAllWithOutDefault(@Param("serverId") String serverId);

    @Delete("DELETE FROM wvp_media_server WHERE id=#{id} and server_id = #{serverId}")
    void delOne(String id, @Param("serverId") String serverId);

    @Select("SELECT * FROM wvp_media_server WHERE ip=#{host} and http_port=#{port} and server_id = #{serverId}")
    MediaServer queryOneByHostAndPort(@Param("host") String host, @Param("port") int port, @Param("serverId") String serverId);

    @Select("SELECT * FROM wvp_media_server WHERE default_server=true and server_id = #{serverId}")
    MediaServer queryDefault(@Param("serverId") String serverId);

    @Select("SELECT * FROM wvp_media_server WHERE record_assist_port > 0 and server_id = #{serverId}")
    List<MediaServer> queryAllWithAssistPort(@Param("serverId") String serverId);

    @Delete("DELETE FROM wvp_media_server WHERE default_server=true and server_id = #{serverId}")
    void deleteDefault(@Param("serverId") String serverId);
}
