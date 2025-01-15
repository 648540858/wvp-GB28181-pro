package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MediaServerMapper {

    int add(MediaServer mediaServerItem);

    int update(MediaServer mediaServerItem);


    int updateByHostAndPort(MediaServer mediaServerItem);

    MediaServer queryOne(String id);

    List<MediaServer> queryAll();

    void delOne(String id);

    void delOneByIPAndPort(@Param("host") String host, @Param("port") int port);

    int delDefault();

    MediaServer queryOneByHostAndPort(@Param("host") String host, @Param("port") int port);

    MediaServer queryDefault();

    List<MediaServer> queryAllWithAssistPort();

}
