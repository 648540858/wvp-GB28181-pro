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
            "rtsp_port,"+
            "flv_port," +
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
            "#{rtspPort}, " +
            "#{flvPort}, " +
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
            "<if test=\"ip != null\">, ip=#{ip}</if>" +
            "<if test=\"hookIp != null\">, hook_ip=#{hookIp}</if>" +
            "<if test=\"sdpIp != null\">, sdp_ip=#{sdpIp}</if>" +
            "<if test=\"streamIp != null\">, stream_ip=#{streamIp}</if>" +
            "<if test=\"httpPort != null\">, http_port=#{httpPort}</if>" +
            "<if test=\"httpSSlPort != null\">, http_ssl_port=#{httpSSlPort}</if>" +
            "<if test=\"rtmpPort != null\">, rtmp_port=#{rtmpPort}</if>" +
            "<if test=\"rtmpSSlPort != null\">, rtmp_ssl_port=#{rtmpSSlPort}</if>" +
            "<if test=\"rtpProxyPort != null\">, rtp_proxy_port=#{rtpProxyPort}</if>" +
            "<if test=\"rtspPort != null\">, rtsp_port=#{rtspPort}</if>" +
            "<if test=\"rtspSSLPort != null\">, rtsp_ssl_port=#{rtspSSLPort}</if>" +
            "<if test=\"flvPort != null\">, flv_port=#{flvPort}</if>" +
            "<if test=\"flvSSLPort != null\">, flv_ssl_port=#{flvSSLPort}</if>" +
            "<if test=\"wsFlvPort != null\">, ws_flv_port=#{wsFlvPort}</if>" +
            "<if test=\"wsFlvSSLPort != null\">, ws_flv_ssl_port=#{wsFlvSSLPort}</if>" +
            "<if test=\"autoConfig != null\">, auto_config=#{autoConfig}</if>" +
            "<if test=\"rtpEnable != null\">, rtp_enable=#{rtpEnable}</if>" +
            "<if test=\"rtpPortRange != null\">, rtp_port_range=#{rtpPortRange}</if>" +
            "<if test=\"sendRtpPortRange != null\">, send_rtp_port_range=#{sendRtpPortRange}</if>" +
            "<if test=\"secret != null\">, secret=#{secret}</if>" +
            "<if test=\"recordAssistPort != null\">, record_assist_port=#{recordAssistPort}</if>" +
            "<if test=\"hookAliveInterval != null\">, hook_alive_interval=#{hookAliveInterval}</if>" +
            "<if test=\"recordDay != null\">, record_day=#{recordDay}</if>" +
            "<if test=\"recordPath != null\">, record_path=#{recordPath}</if>" +
            "<if test=\"serverId != null\">, server_id=#{serverId}</if>" +
            "<if test=\"type != null\">, type=#{type}</if>" +
            "WHERE id=#{id}"+
            " </script>"})
    int update(MediaServer mediaServerItem);

    @Update(value = {" <script>" +
            "UPDATE wvp_media_server " +
            "SET update_time=#{updateTime}" +
            "<if test=\"id != null\">, id=#{id}</if>" +
            "<if test=\"hookIp != null\">, hook_ip=#{hookIp}</if>" +
            "<if test=\"sdpIp != null\">, sdp_ip=#{sdpIp}</if>" +
            "<if test=\"streamIp != null\">, stream_ip=#{streamIp}</if>" +
            "<if test=\"httpSSlPort != null\">, http_ssl_port=#{httpSSlPort}</if>" +
            "<if test=\"rtmpPort != null\">, rtmp_port=#{rtmpPort}</if>" +
            "<if test=\"rtmpSSlPort != null\">, rtmp_ssl_port=#{rtmpSSlPort}</if>" +
            "<if test=\"rtpProxyPort != null\">, rtp_proxy_port=#{rtpProxyPort}</if>" +
            "<if test=\"rtspPort != null\">, rtsp_port=#{rtspPort}</if>" +
            "<if test=\"rtspSSLPort != null\">, rtsp_ssl_port=#{rtspSSLPort}</if>" +
            "<if test=\"flvPort != null\">, flv_port=#{flvPort}</if>" +
            "<if test=\"flvSSLPort != null\">, flv_ssl_port=#{flvSSLPort}</if>" +
            "<if test=\"wsFlvPort != null\">, ws_flv_port=#{wsFlvPort}</if>" +
            "<if test=\"wsFlvSSLPort != null\">, ws_flv_ssl_port=#{wsFlvSSLPort}</if>" +
            "<if test=\"autoConfig != null\">, auto_config=#{autoConfig}</if>" +
            "<if test=\"rtpEnable != null\">, rtp_enable=#{rtpEnable}</if>" +
            "<if test=\"rtpPortRange != null\">, rtp_port_range=#{rtpPortRange}</if>" +
            "<if test=\"sendRtpPortRange != null\">, send_rtp_port_range=#{sendRtpPortRange}</if>" +
            "<if test=\"secret != null\">, secret=#{secret}</if>" +
            "<if test=\"recordAssistPort != null\">, record_assist_port=#{recordAssistPort}</if>" +
            "<if test=\"recordDay != null\">, record_day=#{recordDay}</if>" +
            "<if test=\"recordPath != null\">, record_path=#{recordPath}</if>" +
            "<if test=\"type != null\">, type=#{type}</if>" +
            "<if test=\"transcodeSuffix != null\">, transcode_suffix=#{transcodeSuffix}</if>" +
            "<if test=\"hookAliveInterval != null\">, hook_alive_interval=#{hookAliveInterval}</if>" +
            "<if test=\"serverId != null\">, server_id=#{serverId}</if>" +
            "WHERE ip=#{ip} and http_port=#{httpPort}"+
            " </script>"})
    int updateByHostAndPort(MediaServer mediaServerItem);

    @Select("SELECT * FROM wvp_media_server WHERE id=#{id} and server_id = #{serverId}")
    MediaServer queryOne(@Param("id") String id, @Param("serverId") String serverId);

    @Select("SELECT * FROM wvp_media_server where server_id = #{serverId}")
    List<MediaServer> queryAll(@Param("serverId") String serverId);

    @Delete("DELETE FROM wvp_media_server WHERE id=#{id} and server_id = #{serverId}")
    void delOne(String id, @Param("serverId") String serverId);

    @Select("SELECT * FROM wvp_media_server WHERE ip=#{host} and http_port=#{port} and server_id = #{serverId}")
    MediaServer queryOneByHostAndPort(@Param("host") String host, @Param("port") int port, @Param("serverId") String serverId);

    @Select("SELECT * FROM wvp_media_server WHERE default_server=true and server_id = #{serverId}")
    MediaServer queryDefault(@Param("serverId") String serverId);

    @Select("SELECT * FROM wvp_media_server WHERE record_assist_port > 0 and server_id = #{serverId}")
    List<MediaServer> queryAllWithAssistPort(@Param("serverId") String serverId);

}
