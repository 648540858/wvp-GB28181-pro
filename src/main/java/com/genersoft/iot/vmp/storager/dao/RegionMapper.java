package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.service.bean.Region;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface RegionMapper {
    @Select(" <script>" +
            "select * from wvp_common_region where" +
            "<if test='parentDeviceId != null'> common_region_parent_id = #{parentDeviceId}</if>" +
            "<if test='parentDeviceId == null'> common_region_parent_id is null</if>" +
            " </script>")
    List<Region> getChildren(@Param("parentDeviceId") String parentDeviceId);

    @Insert(" <script>" +
            "INSERT INTO wvp_common_region (" +
            " common_region_device_id, " +
            " common_region_name, " +
            "<if test='region.commonRegionParentId != null'>common_region_parent_id, </if>" +
            " common_region_create_time, " +
            " common_region_update_time ) " +
            " VALUES (" +
            " #{region.commonRegionDeviceId}, " +
            " #{region.commonRegionName}, " +
            "<if test='region.commonRegionParentId != null'> #{region.commonRegionParentId}, </if>" +
            " #{region.commonRegionCreateTime}, " +
            " #{region.commonRegionUpdateTime})" +
            " </script>")
    int add(@Param("region") Region region);

    @Delete("delete from wvp_common_region where common_region_device_id = #{regionDeviceId}")
    int deleteByDeviceId(@Param("regionDeviceId") String regionDeviceId);

    @Update(value = {" <script>" +
            " UPDATE wvp_common_region " +
            " SET common_region_update_time=#{updateTime}, common_region_name=#{name}" +
            " HERE common_region_device_id=#{regionDeviceId}" +
            " </script>"})
    int updateRegionName(@Param("name") String name, @Param("updateTime") String updateTime, @Param("regionDeviceId") String regionDeviceId);

    @Insert(value = "<script>" +
            "insert into wvp_common_region ( " +
            "common_region_device_id, " +
            "common_region_name, " +
            "common_region_parent_id, " +
            "common_region_create_time, " +
            "common_region_update_time " +
            ") values " +
            "<foreach collection='allRegion' index='index' item='item' separator=','> " +
            "( " +
            "#{item.commonRegionDeviceId}, " +
            "#{item.commonRegionName}, " +
            "#{item.commonRegionParentId}, " +
            "#{item.commonRegionCreateTime}, " +
            "#{item.commonRegionUpdateTime} " +
            ")" +
            "</foreach>" +
            "</script>")
    int addAll(List<Region> allRegion);


    @Select("<script> "+
            "SELECT * FROM wvp_common_region WHERE common_region_device_id in" +
            "<foreach collection='allRegion'  item='item'  open='(' separator=',' close=')' > #{item.commonRegionDeviceId}</foreach>" +
            "</script>")
    List<Region> queryInList(List<Region> allRegion);


    @Update({"<script>" +
            "<foreach collection='regionInForUpdate' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_common_region" +
            " SET common_region_name=#{item.commonRegionName}" +
            " WHERE common_region_device_id=#{item.commonRegionDeviceId}"+
            "</foreach>" +
            "</script>"})
    void updateAllForName(List<Region> regionInForUpdate);

    @Select("select * from wvp_common_region where common_region_id = #{commonRegionId}")
    Region queryRegion(@Param("commonRegionId") int commonRegionId);

    @Update(value = {" <script>" +
            " UPDATE wvp_common_region " +
            " SET" +
            " common_region_update_time=#{region.commonRegionUpdateTime}," +
            " common_region_device_id=#{region.commonRegionDeviceId}," +
            " common_region_name=#{region.commonRegionName}," +
            " common_region_parent_id=#{region.commonRegionParentId}" +
            " WHERE common_region_id=#{region.commonRegionId}" +
            " </script>"})
    void update(@Param("region") Region region);

    @Update(value = {" <script>" +
            " UPDATE wvp_common_region " +
            " SET" +
            " common_region_parent_id=#{commonRegionDeviceIdForNew}" +
            " WHERE common_region_parent_id=#{commonRegionDeviceIdForOld}" +
            " </script>"})
    void updateChild(@Param("commonRegionDeviceIdForOld") String commonRegionDeviceIdForOld,
                     @Param("commonRegionDeviceIdForNew") String commonRegionDeviceIdForNew);
    @Select("<script> "+
            "select * from wvp_common_region where 1=1 " +
            "<if test='query != null'> and (common_region_device_id LIKE concat('%',#{query},'%') or common_region_name LIKE concat('%',#{query},'%') )  </if>" +
            "</script>")
    List<Region> query(String query);

    @Select("<script> "+
            "select * from wvp_common_region where common_region_device_id LIKE concat(#{regionDeviceId},'%') " +
            "</script>")
    List<Region> queryAllChildByDeviceId(@Param("regionDeviceId") String regionDeviceId);

    @Select("<script> "+
            "delete from wvp_common_region where common_region_id in" +
            "<foreach collection='regionList'  item='item'  open='(' separator=',' close=')' > #{item.commonRegionId}</foreach>" +
            "</script>")
    void removeRegionByList(@Param("regionList") List<Region> regionList);

    @Select("select * from wvp_common_region where common_region_device_id = #{regionDeviceId}")
    Region queryRegionByDeviceId(@Param("regionDeviceId") String regionDeviceId);

    @MapKey("commonRegionDeviceId")
    @Select("select * from wvp_common_region")
    Map<String, Region> getAllForMap();


    @Select("select " +
            "common_region_id as common_gb_id, " +
            "common_region_device_id as common_gb_device_id, " +
            "common_region_name as common_gb_name, " +
            "from wvp_common_region")
    List<CommonGbChannel> queryAllForCommonChannel();

}
