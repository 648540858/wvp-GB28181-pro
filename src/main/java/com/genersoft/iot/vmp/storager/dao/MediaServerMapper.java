package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
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
            "rtsp_ssl_port,"+
            "auto_config,"+
            "secret,"+
            "rtp_enable,"+
            "rtp_port_range,"+
            "send_rtp_port_range,"+
            "record_assist_port,"+
            "default_server,"+
            "create_time,"+
            "update_time,"+
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
            "#{rtspSSLPort}, " +
            "#{autoConfig}, " +
            "#{secret}, " +
            "#{rtpEnable}, " +
            "#{rtpPortRange}, " +
            "#{sendRtpPortRange}, " +
            "#{recordAssistPort}, " +
            "#{defaultServer}, " +
            "#{createTime}, " +
            "#{updateTime}, " +
            "#{hookAliveInterval})")
    int add(MediaServerItem mediaServerItem);

    @Update(value = {" <script>" +
            "UPDATE wvp_media_server " +
            "SET update_time=#{updateTime}" +
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
            "<if test=\"autoConfig != null\">, auto_config=#{autoConfig}</if>" +
            "<if test=\"rtpEnable != null\">, rtp_enable=#{rtpEnable}</if>" +
            "<if test=\"rtpPortRange != null\">, rtp_port_range=#{rtpPortRange}</if>" +
            "<if test=\"sendRtpPortRange != null\">, send_rtp_port_range=#{sendRtpPortRange}</if>" +
            "<if test=\"secret != null\">, secret=#{secret}</if>" +
            "<if test=\"recordAssistPort != null\">, record_assist_port=#{recordAssistPort}</if>" +
            "<if test=\"hookAliveInterval != null\">, hook_alive_interval=#{hookAliveInterval}</if>" +
            "WHERE id=#{id}"+
            " </script>"})
    int update(MediaServerItem mediaServerItem);

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
            "<if test=\"autoConfig != null\">, auto_config=#{autoConfig}</if>" +
            "<if test=\"rtpEnable != null\">, rtp_enable=#{rtpEnable}</if>" +
            "<if test=\"rtpPortRange != null\">, rtp_port_range=#{rtpPortRange}</if>" +
            "<if test=\"sendRtpPortRange != null\">, send_rtp_port_range=#{sendRtpPortRange}</if>" +
            "<if test=\"secret != null\">, secret=#{secret}</if>" +
            "<if test=\"recordAssistPort != null\">, record_assist_port=#{recordAssistPort}</if>" +
            "<if test=\"hookAliveInterval != null\">, hook_alive_interval=#{hookAliveInterval}</if>" +
            "WHERE ip=#{ip} and http_port=#{httpPort}"+
            " </script>"})
    int updateByHostAndPort(MediaServerItem mediaServerItem);

    @Select("SELECT * FROM wvp_media_server WHERE id=#{id}")
    MediaServerItem queryOne(String id);

    @Select("SELECT * FROM wvp_media_server")
    List<MediaServerItem> queryAll();

    @Delete("DELETE FROM wvp_media_server WHERE id=#{id}")
    void delOne(String id);

    @Select("DELETE FROM wvp_media_server WHERE ip=#{host} and http_port=#{port}")
    void delOneByIPAndPort(@Param("host") String host, @Param("port") int port);

    @Delete("DELETE FROM wvp_media_server WHERE default_server=true")
    int delDefault();

    @Select("SELECT * FROM wvp_media_server WHERE ip=#{host} and http_port=#{port}")
    MediaServerItem queryOneByHostAndPort(@Param("host") String host, @Param("port") int port);

    @Select("SELECT * FROM wvp_media_server WHERE default_server=true")
    MediaServerItem queryDefault();
}
