package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.PlatformCatalog;
import com.genersoft.iot.vmp.gb28181.bean.PlatformGbStream;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;


@Mapper
@Repository
public interface PlatformGbStreamMapper {

    @Insert("INSERT INTO wvp_platform_gb_stream (gb_stream_id, platform_id, catalog_id) VALUES" +
            "( #{gbStreamId}, #{platformId}, #{catalogId})")
    int add(PlatformGbStream platformGbStream);


    @Insert("<script> " +
            "INSERT into wvp_platform_gb_stream " +
            "(gb_stream_id, platform_id, catalog_id) " +
            "values " +
            "<foreach collection='streamPushItems' index='index' item='item' separator=','> " +
            "(#{item.gbStreamId}, #{item.platform_id}, #{item.catalogId})" +
            "</foreach> " +
            "</script>")
    int batchAdd(List<StreamPushItem> streamPushItems);

    @Delete("DELETE from wvp_platform_gb_stream WHERE gb_stream_id = (select gb_stream_id from wvp_gb_stream where app=#{app} AND stream=#{stream})")
    int delByAppAndStream(@Param("app") String app, @Param("stream") String stream);

    @Delete("DELETE from wvp_platform_gb_stream WHERE platform_id=#{platformId}")
    int delByPlatformId(String platformId);

    @Select("SELECT " +
            "pp.* " +
            "FROM " +
            "wvp_platform_gb_stream pgs " +
            "LEFT JOIN wvp_platform pp ON pp.server_gb_id = pgs.platform_id " +
            "LEFT join wvp_gb_stream gs ON gs.gb_stream_id = pgs.gb_stream_id " +
            "WHERE " +
            "gs.app =#{app} " +
            "AND gs.stream =#{stream} ")
    List<ParentPlatform> selectByAppAndStream(@Param("app") String app, @Param("stream") String stream);

    @Select("SELECT pgs.*, gs.gb_id  from wvp_platform_gb_stream pgs " +
            "LEFT join wvp_gb_stream gs ON pgs.gb_stream_id = gs.gb_stream_id  " +
            "WHERE gs.app=#{app} AND gs.stream=#{stream} AND pgs.platform_id=#{platformId}")
    StreamProxyItem selectOne(@Param("app") String app, @Param("stream") String stream, @Param("platformId") String platformId);

    @Select("select gs.* \n" +
            "from wvp_gb_stream gs\n" +
            "    left join wvp_platform_gb_stream pgs\n" +
            "        on gs.gb_stream_id = pgs.gb_stream_id\n" +
            "where pgs.platform_id=#{platformId} and pgs.catalog_id=#{catalogId}")
    List<GbStream> queryChannelInParentPlatformAndCatalog(@Param("platformId") String platformId, @Param("catalogId") String catalogId);

    @Select("select gs.gb_id as id, gs.name as name, pgs.platform_id as platform_id, pgs.catalog_id as catalog_id , 0 as children_count, 2 as type\n" +
            "from wvp_gb_stream gs\n" +
            "    left join wvp_platform_gb_stream pgs\n" +
            "        on gs.gb_stream_id = pgs.gb_stream_id\n" +
            "where pgs.platform_id=#{platformId} and pgs.catalog_id=#{catalogId}")
    List<PlatformCatalog> queryChannelInParentPlatformAndCatalogForCatalog(@Param("platformId") String platformId, @Param("catalogId") String catalogId);

    @Select("<script> " +
            "SELECT " +
            "pp.* " +
            "FROM " +
            "wvp_platform pp " +
            "left join wvp_platform_gb_stream pgs on " +
            "pp.server_gb_id = pgs.platform_id " +
            "left join wvp_gb_stream gs " +
            "on gs.gb_stream_id = pgs.gb_stream_id " +
            "WHERE " +
            "gs.app = #{app} " +
            "AND gs.stream = #{stream}" +
            "AND pp.server_gb_id IN" +
            "<foreach collection='platforms'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</script> ")
    List<ParentPlatform> queryPlatFormListForGBWithGBId(@Param("app") String app, @Param("stream") String stream, @Param("platforms") List<String> platforms);

    @Delete("DELETE from wvp_platform_gb_stream WHERE gb_stream_id = (select id from wvp_gb_stream where app=#{app} AND stream=#{stream}) AND platform_id=#{platformId}")
    int delByAppAndStreamAndPlatform(String app, String stream, String platformId);

    @Delete("<script> "+
            "DELETE from wvp_platform_gb_stream where gb_stream_id in " +
            "<foreach collection='gbStreams' item='item' open='(' separator=',' close=')' >" +
            "#{item.gbStreamId}" +
            "</foreach>" +
            "</script>")
    void delByGbStreams(List<GbStream> gbStreams);

    @Delete("<script> "+
            "DELETE from wvp_platform_gb_stream where platform_id=#{platformId} and gb_stream_id in " +
            "<foreach collection='gbStreams' item='item' open='(' separator=',' close=')'>" +
            "#{item.gbStreamId} " +
            "</foreach>" +
            "</script>")
    void delByAppAndStreamsByPlatformId(@Param("gbStreams") List<GbStream> gbStreams, @Param("platformId") String platformId);

    @Delete("DELETE from wvp_platform_gb_stream WHERE platform_id=#{platformId} and catalog_id=#{catalogId}")
    int delByPlatformAndCatalogId(@Param("platformId") String platformId, @Param("catalogId") String catalogId);
}
