package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.service.bean.Region;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RegionMapper {
    @Select("select * from wvp_common_region where common_region_parent_id = #{parentDeviceId}")
    List<Region> getChildren(@Param("parentDeviceId") String parentDeviceId);

    @Insert("INSERT INTO wvp_common_region (" +
            " common_region_device_id, " +
            " common_region_name, " +
            " common_region_parent_id, " +
            " common_region_path, " +
            " common_region_create_time, " +
            " common_region_update_time ) " +
            " VALUES (" +
            " #{commonRegionDeviceId}, " +
            " #{commonRegionName}, " +
            " #{commonRegionParentId}, " +
            " #{commonRegionPath}, " +
            " #{commonRegionCreateTime}, " +
            " #{commonRegionUpdateTime})")
    int add(Region region);

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
            " common_region_update_time=#{commonRegionUpdateTime}," +
            " common_region_device_id=#{commonRegionDeviceId}," +
            " common_region_name=#{commonRegionName}," +
            " common_region_parent_id=#{commonRegionParentId}" +
            " WHERE common_region_id=#{commonRegionId}" +
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
}
