package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.PlatformCatalog;
import com.genersoft.iot.vmp.gb28181.bean.PlatformGbStream;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;


@Mapper
@Repository
public interface PlatformCatalogMapper {

    @Insert("INSERT INTO platform_catalog (id, name, platformId, parentId, civilCode, businessGroupId) VALUES" +
            "(#{id}, #{name}, #{platformId}, #{parentId}, #{civilCode}, #{businessGroupId})")
    int add(PlatformCatalog platformCatalog);

    @Delete("DELETE FROM platform_catalog WHERE id=#{id}")
    int del(String id);

    @Delete("DELETE FROM platform_catalog WHERE platformId=#{platformId}")
    int delByPlatformId(String platformId);

    @Select("SELECT pc.*, count(pc2.id) as childrenCount FROM platform_catalog pc " +
            "left join platform_catalog pc2 on pc.id = pc2.parentId " +
            "WHERE pc.parentId=#{parentId} AND pc.platformId=#{platformId} group by pc.id")
    List<PlatformCatalog> selectByParentId(String platformId, String parentId);

    @Select("SELECT *, (SELECT COUNT(1) from platform_catalog where parentId = pc.id) as childrenCount  FROM platform_catalog pc WHERE pc.id=#{id}")
    PlatformCatalog select(String id);

    @Update(value = {" <script>" +
            "UPDATE platform_catalog " +
            "SET name=#{name}" +
            "WHERE id=#{id}"+
            "</script>"})
    int update(PlatformCatalog platformCatalog);

    @Select("SELECT *, (SELECT COUNT(1) from platform_catalog where parentId = pc.id) as childrenCount  FROM platform_catalog pc WHERE pc.platformId=#{platformId}")
    List<PlatformCatalog> selectByPlatForm(String platformId);

    @Select("SELECT pc.* FROM  platform_catalog pc WHERE  pc.id = (SELECT pp.catalogId from parent_platform pp WHERE pp.serverGBId=#{platformId})")
    PlatformCatalog selectDefaultByPlatFormId(String platformId);


    @Select("SELECT pc.* FROM  platform_catalog pc WHERE pc.id = #{id}")
    PlatformCatalog selectParentCatalog(String id);

    @Select("SELECT pc.id as channelId, pc.name, pc.civilCode, pc.businessGroupId,'1' as parental, pc.parentId  " +
            " FROM platform_catalog pc WHERE pc.platformId=#{platformId}")
    List<DeviceChannel> queryCatalogInPlatform(String platformId);
}
