package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.GroupTree;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

@Mapper
public interface GroupMapper {

    @Insert("INSERT INTO wvp_common_group (device_id, name, parent_device_id, business_group, platform_id, create_time, update_time) " +
            "VALUES (#{deviceId}, #{name}, #{parentDeviceId}, #{businessGroup}, #{platformId}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(Group group);

    @Delete("DELETE FROM wvp_common_group WHERE id=#{id}")
    int delete(@Param("id") int id);

    @Update(" UPDATE wvp_common_group " +
            " SET update_time=#{updateTime}, device_id=#{deviceId}, name=#{name}, parent_device_id=#{parentDeviceId}, business_group=#{businessGroup}" +
            " WHERE id = #{id}")
    int update(Group group);

    @Select(value = {" <script>" +
            "SELECT * from wvp_common_group WHERE 1=1 " +
            " <if test='query != null'> AND (device_id LIKE concat('%',#{query},'%') OR name LIKE concat('%',#{query},'%'))</if> " +
            " <if test='parentId != null and businessGroupId != null '> AND parent_device_id = #{parentId} AND business_group=#{businessGroup} </if> " +
            "ORDER BY id " +
            " </script>"})
    List<Group> query(@Param("query") String query, @Param("parentId") String parentId, @Param("businessGroup") String businessGroup);

    @Select(value = {" <script>" +
            "SELECT * from wvp_common_group WHERE parent_device_id = #{parentId} "+
            " <if test='platformId != null'> AND platform_id = #{platformId}</if> " +
            " <if test='platformId == null'> AND platform_id is null</if> " +
            " </script>"})
    List<Group> getChildren(@Param("parentId") String parentId , @Param("platformId") Integer platformId);

    @Select("SELECT * from wvp_common_group WHERE id = #{id} ")
    Group queryOne(@Param("id") int id);

    @Select(" select coalesce(dc.gb_civil_code, dc.civil_code) as civil_code " +
            " from wvp_device_channel dc " +
            " where coalesce(dc.gb_civil_code, dc.civil_code) not in " +
            " (select device_id from wvp_common_group)")
    List<String> getUninitializedCivilCode();

    @Select(" <script>" +
            " SELECT device_id from wvp_common_group " +
            " where device_id in " +
            " <foreach collection='codes'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            " </script>")
    List<String> queryInList(Set<String> codes);


    @Insert(" <script>" +
            " INSERT INTO wvp_common_group (" +
            " device_id," +
            " name, " +
            " parent_device_id," +
            " create_time," +
            " update_time) " +
            " VALUES " +
            " <foreach collection='groupList' index='index' item='item' separator=','> " +
            " (#{item.deviceId}, #{item.name}, #{item.parentDeviceId},#{item.createTime},#{item.updateTime})" +
            " </foreach> " +
            " </script>")
    int batchAdd(List<Group> groupList);

    @Select(" <script>" +
            " SELECT " +
            " device_id as id," +
            " name as label, " +
            " parent_device_id," +
            " id as db_id," +
            " 0 as type," +
            " false as is_leaf" +
            " from wvp_common_group " +
            " where " +
            " <if test='parentId != null'> parent_device_id = #{parentId} </if> " +
            " <if test='parentId == null'> parent_device_id is null </if> " +
            " <if test='platformId != null'> platform_id = #{platformId} </if> " +
            " <if test='platformId == null'> platform_id is null </if> " +
            " <if test='query != null'> AND (device_id LIKE concat('%',#{query},'%') OR name LIKE concat('%',#{query},'%'))</if> " +
            " </script>")
    List<GroupTree> queryForTree(@Param("query") String query, @Param("parentId") String parentId,
                                 @Param("platformId") Integer platformId);

    @Select(" <script>" +
            " SELECT " +
            " device_id as id," +
            " name as label, " +
            " parent_device_id," +
            " id as db_id," +
            " 0 as type," +
            " false as is_leaf" +
            " from wvp_common_group " +
            " where device_id=business_group" +
            " <if test='platformId != null'> AND platform_id = #{platformId} </if> " +
            " <if test='platformId == null'> AND platform_id is null </if> " +
            " <if test='query != null'> AND (device_id LIKE concat('%',#{query},'%') OR name LIKE concat('%',#{query},'%'))</if> " +
            " </script>")
    List<GroupTree> queryBusinessGroupForTree(String query, Integer platformId);

    @Select("SELECT * from wvp_common_group WHERE device_id = #{deviceId} and business_group = #{businessGroup}")
    Group queryOneByDeviceId(@Param("deviceId") String deviceId, @Param("businessGroup") String businessGroup);

    @Delete("<script>" +
            " DELETE FROM wvp_common_group WHERE id in " +
            " <foreach collection='allChildren'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>")
    int batchDelete(List<Group> allChildren);

    @Select("SELECT * from wvp_common_group WHERE device_id = #{businessGroup} and business_group = #{businessGroup} ")
    Group queryBusinessGroup(@Param("businessGroup") String businessGroup);

    @Select("SELECT * from wvp_common_group WHERE business_group = #{businessGroup} ")
    List<Group> queryByBusinessGroup(@Param("businessGroup") String businessGroup);

    @Delete("DELETE FROM wvp_common_group WHERE business_group = #{businessGroup}")
    int deleteByBusinessGroup(@Param("businessGroup") String businessGroup);


}
