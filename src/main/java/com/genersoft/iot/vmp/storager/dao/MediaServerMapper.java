package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;


@Mapper
@Repository
public interface MediaServerMapper {

    @Insert("INSERT INTO media_server (" +
            "id, " +
            "ip, " +
            "hookIp, " +
            "sdpIp, " +
            "streamIp, " +
            "httpPort, " +
            "httpSSlPort, " +
            "rtmpPort, " +
            "rtmpSSlPort, " +
            "rtpProxyPort, " +
            "rtspPort, " +
            "rtspSSLPort, " +
            "autoConfig, " +
            "secret, " +
            "streamNoneReaderDelayMS, " +
            "rtpEnable, " +
            "rtpPortRange, " +
            "sendRtpPortRange, " +
            "recordAssistPort, " +
            "defaultServer, " +
            "createTime, " +
            "updateTime" +
            ") VALUES " +
            "(" +
            "'${id}', " +
            "'${ip}', " +
            "'${hookIp}', " +
            "'${sdpIp}', " +
            "'${streamIp}', " +
            "${httpPort}, " +
            "${httpSSlPort}, " +
            "${rtmpPort}, " +
            "${rtmpSSlPort}, " +
            "${rtpProxyPort}, " +
            "${rtspPort}, " +
            "${rtspSSLPort}, " +
            "${autoConfig}, " +
            "'${secret}', " +
            "${streamNoneReaderDelayMS}, " +
            "${rtpEnable}, " +
            "'${rtpPortRange}', " +
            "'${sendRtpPortRange}', " +
            "${recordAssistPort}, " +
            "${defaultServer}, " +
            "'${createTime}', " +
            "'${updateTime}')")
    int add(MediaServerItem mediaServerItem);

    @Update(value = {" <script>" +
            "UPDATE media_server " +
            "SET updateTime='${updateTime}'" +
            "<if test=\"ip != null\">, ip='${ip}'</if>" +
            "<if test=\"hookIp != null\">, hookIp='${hookIp}'</if>" +
            "<if test=\"sdpIp != null\">, sdpIp='${sdpIp}'</if>" +
            "<if test=\"streamIp != null\">, streamIp='${streamIp}'</if>" +
            "<if test=\"httpPort != null\">, httpPort=${httpPort}</if>" +
            "<if test=\"httpSSlPort != null\">, httpSSlPort=${httpSSlPort}</if>" +
            "<if test=\"rtmpPort != null\">, rtmpPort=${rtmpPort}</if>" +
            "<if test=\"rtmpSSlPort != null\">, rtmpSSlPort=${rtmpSSlPort}</if>" +
            "<if test=\"rtpProxyPort != null\">, rtpProxyPort=${rtpProxyPort}</if>" +
            "<if test=\"rtspPort != null\">, rtspPort=${rtspPort}</if>" +
            "<if test=\"rtspSSLPort != null\">, rtspSSLPort=${rtspSSLPort}</if>" +
            "<if test=\"autoConfig != null\">, autoConfig=${autoConfig}</if>" +
            "<if test=\"streamNoneReaderDelayMS != null\">, streamNoneReaderDelayMS=${streamNoneReaderDelayMS}</if>" +
            "<if test=\"rtpEnable != null\">, rtpEnable=${rtpEnable}</if>" +
            "<if test=\"rtpPortRange != null\">, rtpPortRange='${rtpPortRange}'</if>" +
            "<if test=\"sendRtpPortRange != null\">, sendRtpPortRange='${sendRtpPortRange}'</if>" +
            "<if test=\"secret != null\">, secret='${secret}'</if>" +
            "<if test=\"recordAssistPort != null\">, recordAssistPort=${recordAssistPort}</if>" +
            "WHERE id='${id}'"+
            " </script>"})
    int update(MediaServerItem mediaServerItem);

    @Update(value = {" <script>" +
            "UPDATE media_server " +
            "SET updateTime='${updateTime}'" +
            "<if test=\"id != null\">, id='${id}'</if>" +
            "<if test=\"hookIp != null\">, hookIp='${hookIp}'</if>" +
            "<if test=\"sdpIp != null\">, sdpIp='${sdpIp}'</if>" +
            "<if test=\"streamIp != null\">, streamIp='${streamIp}'</if>" +
            "<if test=\"httpSSlPort != null\">, httpSSlPort=${httpSSlPort}</if>" +
            "<if test=\"rtmpPort != null\">, rtmpPort=${rtmpPort}</if>" +
            "<if test=\"rtmpSSlPort != null\">, rtmpSSlPort=${rtmpSSlPort}</if>" +
            "<if test=\"rtpProxyPort != null\">, rtpProxyPort=${rtpProxyPort}</if>" +
            "<if test=\"rtspPort != null\">, rtspPort=${rtspPort}</if>" +
            "<if test=\"rtspSSLPort != null\">, rtspSSLPort=${rtspSSLPort}</if>" +
            "<if test=\"autoConfig != null\">, autoConfig=${autoConfig}</if>" +
            "<if test=\"streamNoneReaderDelayMS != null\">, streamNoneReaderDelayMS=${streamNoneReaderDelayMS}</if>" +
            "<if test=\"rtpEnable != null\">, rtpEnable=${rtpEnable}</if>" +
            "<if test=\"rtpPortRange != null\">, rtpPortRange='${rtpPortRange}'</if>" +
            "<if test=\"sendRtpPortRange != null\">, sendRtpPortRange='${sendRtpPortRange}'</if>" +
            "<if test=\"secret != null\">, secret='${secret}'</if>" +
            "<if test=\"recordAssistPort != null\">, recordAssistPort=${recordAssistPort}</if>" +
            "WHERE ip='${ip}' and httpPort=${httpPort}"+
            " </script>"})
    int updateByHostAndPort(MediaServerItem mediaServerItem);

    @Select("SELECT * FROM media_server WHERE id='${id}'")
    MediaServerItem queryOne(String id);

    @Select("SELECT * FROM media_server")
    List<MediaServerItem> queryAll();

    @Select("DELETE FROM media_server WHERE id='${id}'")
    void delOne(String id);

    @Select("DELETE FROM media_server WHERE ip='${host}' and httpPort=${port}")
    void delOneByIPAndPort(String host, int port);

    @Select("DELETE FROM media_server WHERE defaultServer=1;")
    void delDefault();

    @Select("SELECT * FROM media_server WHERE ip='${host}' and httpPort=${port}")
    MediaServerItem queryOneByHostAndPort(String host, int port);

    @Select("SELECT * FROM media_server WHERE defaultServer=1")
    MediaServerItem queryDefault();
}
