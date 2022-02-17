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

    @Insert("REPLACE INTO platform_gb_stream (app, stream, platformId, catalogId) VALUES" +
            "('${app}', '${stream}', '${platformId}', '${catalogId}')")
    int add(PlatformGbStream platformGbStream);


    @Insert("<script> " +
            "REPLACE into platform_gb_stream " +
            "(app, stream, platformId, catalogId) " +
            "values " +
            "<foreach collection='streamPushItems' index='index' item='item' separator=','> " +
            "('${item.app}', '${item.stream}', '${platformId}', '${catalogId}')" +
            "</foreach> " +
            "</script>")
    int batchAdd(String platformId, String catalogId, List<StreamPushItem> streamPushItems);

    @Delete("DELETE FROM platform_gb_stream WHERE app=#{app} AND stream=#{stream}")
    int delByAppAndStream(String app, String stream);

    @Delete("DELETE FROM platform_gb_stream WHERE platformId=#{platformId}")
    int delByPlatformId(String platformId);

    @Select("SELECT " +
            "pp.* " +
            "FROM " +
            "platform_gb_stream pgs " +
            "LEFT JOIN parent_platform pp ON pp.serverGBId = pgs.platformId " +
            "WHERE " +
            "pgs.app =#{app} " +
            "AND pgs.stream =#{stream} ")
    List<ParentPlatform> selectByAppAndStream(String app, String stream);

    @Select("SELECT pgs.*, gs.gbId  FROM platform_gb_stream pgs " +
            "LEFT JOIN gb_stream gs ON pgs.app = gs.app AND pgs.stream = gs.stream " +
            "WHERE pgs.app=#{app} AND pgs.stream=#{stream} AND pgs.platformId=#{serverGBId}")
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
    List<PlatformCatalog> queryChannelInParentPlatformAndCatalogForCatalog(String platformId, String catalogId);

    @Delete("DELETE FROM platform_gb_stream WHERE catalogId=#{id}")
    int delByCatalogId(String id);

    @Select("<script> " +
            "SELECT " +
            "pp.* " +
            "FROM " +
            "parent_platform pp " +
            "left join platform_gb_stream pgs on " +
            "pp.serverGBId = pgs.platformId " +
            "WHERE " +
            "pgs.app = #{app} " +
            "AND pgs.stream = #{stream}" +
            "AND pp.serverGBId IN" +
            "<foreach collection='platforms'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</script> ")
    List<ParentPlatform> queryPlatFormListForGBWithGBId(String app, String stream, List<String> platforms);

    @Delete("DELETE FROM platform_gb_stream WHERE app=#{app} AND stream=#{stream} AND platformId=#{platformId}")
    int delByAppAndStreamAndPlatform(String app, String stream, String platformId);

    @Delete("<script> "+
            "DELETE FROM platform_gb_stream where " +
            "<foreach collection='gbStreams' item='item' separator='or'>" +
            "(app=#{item.app} and stream=#{item.stream}) " +
            "</foreach>" +
            "</script>")
    void delByGbStreams(List<GbStream> gbStreams);



}
