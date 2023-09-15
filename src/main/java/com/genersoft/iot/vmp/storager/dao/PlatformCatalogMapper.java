package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.PlatformCatalog;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Mapper
@Repository
public interface PlatformCatalogMapper {

    @Insert("INSERT INTO wvp_platform_catalog (id, name, platform_id, parent_id, civil_code, business_group_id) VALUES" +
            "(#{id}, #{name}, #{platformId}, #{parentId}, #{civilCode}, #{businessGroupId})")
    int add(PlatformCatalog platformCatalog);

    @Delete("DELETE from wvp_platform_catalog WHERE platform_id=#{platformId} and id=#{id}")
    int del(@Param("platformId") String platformId, @Param("id") String id);

    @Delete("DELETE from wvp_platform_catalog WHERE platform_id=#{platformId}")
    int delByPlatformId(@Param("platformId") String platformId);

    @Select("SELECT pc.*, count(pc2.id) as children_count from wvp_platform_catalog pc " +
            "left join wvp_platform_catalog pc2 on pc.id = pc2.parent_id " +
            "WHERE pc.parent_id=#{parentId} AND pc.platform_id=#{platformId} " +
            "group by pc.id, pc.name, pc.platform_id, pc.business_group_id, pc.civil_code, pc.parent_id")
    List<PlatformCatalog> selectByParentId(@Param("platformId") String platformId, @Param("parentId") String parentId);

    @Update(value = {" <script>" +
            "UPDATE wvp_platform_catalog " +
            "SET name=#{name}" +
            "WHERE id=#{id} and platform_id=#{platformId}"+
            "</script>"})
    int update(@Param("platformCatalog") PlatformCatalog platformCatalog);

    @Select("SELECT *, (SELECT COUNT(1) from wvp_platform_catalog where parent_id = pc.id) as children_count  from wvp_platform_catalog pc WHERE pc.platform_id=#{platformId}")
    List<PlatformCatalog> selectByPlatForm(@Param("platformId") String platformId);

    @Select("SELECT pc.* FROM  wvp_platform_catalog pc WHERE pc.id = (SELECT pp.catalog_id from wvp_platform pp WHERE pp.server_gb_id=#{platformId})")
    PlatformCatalog selectDefaultByPlatFormId(@Param("platformId") String platformId);

    @Select("SELECT pc.id as channel_id, pc.name, pc.civil_code, pc.business_group_id,'1' as parental, pc.parent_id  " +
            " from wvp_platform_catalog pc WHERE pc.platform_id=#{platformId}")
    List<DeviceChannel> queryCatalogInPlatform(@Param("platformId") String platformId);

    @Select("SELECT *, " +
            "(SELECT COUNT(1) from wvp_platform_catalog where parent_id = pc.id) as children_count " +
            " from wvp_platform_catalog pc " +
            " WHERE pc.id=#{id} and pc.platform_id=#{platformId}")
    PlatformCatalog selectByPlatFormAndCatalogId(@Param("platformId") String platformId, @Param("id") String id);
}
