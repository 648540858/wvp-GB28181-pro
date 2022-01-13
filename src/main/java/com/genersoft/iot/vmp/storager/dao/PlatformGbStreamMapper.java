package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.PlatformCatalog;
import com.genersoft.iot.vmp.gb28181.bean.PlatformGbStream;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;


@Mapper
@Repository
public interface PlatformGbStreamMapper {

    @Insert("INSERT INTO platform_gb_stream (app, stream, platformId, catalogId) VALUES" +
            "('${app}', '${stream}', '${platformId}', '${catalogId}')")
    int add(PlatformGbStream platformGbStream);

    @Delete("DELETE FROM platform_gb_stream WHERE app=#{app} AND stream=#{stream}")
    int delByAppAndStream(String app, String stream);

    @Delete("DELETE FROM platform_gb_stream WHERE platformId=#{platformId}")
    int delByPlatformId(String platformId);

    @Select("SELECT * FROM platform_gb_stream WHERE app=#{app} AND stream=#{stream}")
    List<StreamProxyItem> selectByAppAndStream(String app, String stream);

    @Select("SELECT * FROM platform_gb_stream WHERE app=#{app} AND stream=#{stream} AND platformId=#{serverGBId}")
    StreamProxyItem selectOne(String app, String stream, String serverGBId);

    @Select("select gs.* \n" +
            "from gb_stream gs\n" +
            "    left join platform_gb_stream pgs\n" +
            "        on gs.app = pgs.app and gs.stream = pgs.stream\n" +
            "where pgs.platformId=#{platformId} and pgs.catalogId=#{catalogId}")
    List<GbStream> queryChannelInParentPlatformAndCatalog(String platformId, String catalogId);

    @Select("select gs.gbId as id, gs.name as name, pgs.platformId as platformId, pgs.catalogId as catalogId , 0 as childrenCount, 2 as type\n" +
            "from gb_stream gs\n" +
            "    left join platform_gb_stream pgs\n" +
            "        on gs.app = pgs.app and gs.stream = pgs.stream\n" +
            "where pgs.platformId=#{platformId} and pgs.catalogId=#{catalogId}")
    List<PlatformCatalog> queryChannelInParentPlatformAndCatalogForCatlog(String platformId, String catalogId);

    @Delete("DELETE FROM platform_gb_stream WHERE catalogId=#{id}")
    int delByCatalogId(String id);

}
