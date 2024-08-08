package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.Region;
import com.genersoft.iot.vmp.gb28181.bean.RegionTree;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

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
    List<Region> getChildren(@Param("parentId") String parentId);

    @Select("SELECT * from wvp_common_region WHERE id = #{id} ")
    Region queryOne(@Param("id") int id);

    @Select(" select dc.civil_code as civil_code " +
            " from wvp_device_channel dc " +
            " where dc.civil_code not in " +
            " (select device_id from wvp_common_region)")
    List<String> getUninitializedCivilCode();

    @Select(" <script>" +
            " SELECT device_id from wvp_common_region " +
            " where device_id in " +
            " <foreach collection='codes'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            " </script>")
    List<String> queryInList(Set<String> codes);


    @Insert(" <script>" +
            " INSERT INTO wvp_common_region (" +
            " device_id," +
            " name, " +
            " parent_device_id," +
            " create_time," +
            " update_time) " +
            " VALUES " +
            " <foreach collection='regionList' index='index' item='item' separator=','> " +
            " (#{item.deviceId}, #{item.name}, #{item.parentDeviceId},#{item.createTime},#{item.updateTime})" +
            " </foreach> " +
            " </script>")
    int batchAdd(List<Region> regionList);

    @Select(" <script>" +
            " SELECT " +
            " device_id as id," +
            " name as label, " +
            " parent_device_id," +
            " id as db_id," +
            " 0 as type," +
            " false as is_leaf" +
            " from wvp_common_region " +
            " where " +
            " <if test='parentId != null'> parent_device_id = #{parentId} </if> " +
            " <if test='parentId == null'> parent_device_id is null </if> " +
            " <if test='query != null'> AND (device_id LIKE concat('%',#{query},'%') OR name LIKE concat('%',#{query},'%'))</if> " +
            " </script>")
    List<RegionTree> queryForTree(@Param("query") String query, @Param("parentId") String parentId);

    @Select("SELECT * from wvp_common_region WHERE device_id = #{deviceId} ")
    Region queryOneByDeviceId(@Param("deviceId") String deviceId);

    @Delete("<script>" +
            " DELETE FROM wvp_common_region WHERE id in " +
            " <foreach collection='allChildren'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>")
    void batchDelete(List<Region> allChildren);

    @Select(" <script>" +
            " SELECT * from wvp_common_region " +
            " where device_id in " +
            " <foreach collection='regionList'  item='item'  open='(' separator=',' close=')' > #{item.deviceId}</foreach>" +
            " </script>")
    List<Region> queryInRegionList(List<Region> regionList);
}
