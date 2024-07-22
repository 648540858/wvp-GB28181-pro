package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.Region;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RegionMapper {

    @Insert("INSERT INTO wvp_common_region (device_id, name, parent_device_id, create_time, update_time) " +
            "VALUES (#{deviceId}, #{name}, #{parentDeviceId}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void add(Region region);

    @Delete("DELETE FROM wvp_common_region WHERE id=#{id}")
    int delete(@Param("id") int id);

    @Update(" UPDATE wvp_common_region " +
            " SET update_time=#{updateTime}, device_id=#{deviceId}, name=#{name}, parent_device_id=#{parentDeviceId}" +
            " WHERE id = #{id}")
    int update(Region region);

    @Select(value = {" <script>" +
            "SELECT *  from wvp_common_region WHERE 1=1 " +
            " <if test='query != null'> AND (device_id LIKE concat('%',#{query},'%') OR name LIKE concat('%',#{query},'%'))</if> " +
            " <if test='parentId != null'> AND parent_device_id = #{parentId}</if> " +
            "ORDER BY id " +
            " </script>"})
    List<Region> query(@Param("query") String query, @Param("parentId") String parentId);

    @Select("SELECT * from wvp_common_region WHERE parent_device_id = #{parentId} ORDER BY id ")
    List<Region> getChildren(String parentId);

    @Select("SELECT * from wvp_common_region WHERE id = #{id} ")
    Region queryOne(int id);
}
