package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.PlatformGbStream;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;


@Mapper
@Repository
public interface PlatformGbStreamMapper {

    @Insert("INSERT INTO platform_gb_stream (app, stream, platformId) VALUES" +
            "('${app}', '${stream}', '${platformId}')")
    int add(PlatformGbStream platformGbStream);

    @Delete("DELETE FROM platform_gb_stream WHERE app=#{app} AND stream=#{stream}")
    int delByAppAndStream(String app, String stream);

    @Delete("DELETE FROM platform_gb_stream WHERE platformId=#{platformId}")
    int delByPlatformId(String platformId);

    @Select("SELECT * FROM platform_gb_stream WHERE app=#{app} AND stream=#{stream}")
    List<StreamProxyItem> selectByAppAndStream(String app, String stream);

    @Select("SELECT * FROM platform_gb_stream WHERE app=#{app} AND stream=#{stream} AND platformId=#{serverGBId}")
    StreamProxyItem selectOne(String app, String stream, String serverGBId);
}
