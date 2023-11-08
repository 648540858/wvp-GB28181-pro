package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.service.bean.Group;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface GroupMapper {

    @Select(value = " <script>" +
            " select * from wvp_common_group " +
            " WHERE 1=1 " +
            " <if test='parentId != null' >  AND common_group_parent_id = #{parentId}</if>" +
            " <if test='parentId == null' >  AND common_group_parent_id is null </if>" +
            " order by common_group_id ASC " +
            " </script>")
    List<Group> getNodes(@Param("parentId") String parentId);

    @Select(" select * from wvp_common_group " +
            " WHERE common_group_id = #{id} ")
    Group queryOne(@Param("id") int id);

    @Select(" select * from wvp_common_group " +
            " WHERE common_group_device_id = #{deviceId} ")
    Group queryByDeviceId(@Param("deviceId") String deviceId);

    @Insert("INSERT INTO wvp_common_group (" +
            "common_group_device_id, " +
            "common_group_name, " +
            "common_group_parent_id, " +
            "common_group_top_id, " +
            "common_group_update_time, " +
            "common_group_create_time ) " +
            "VALUES (" +
            "#{commonGroupDeviceId}, " +
            "#{commonGroupName}, " +
            "#{commonGroupParentId}, " +
            "#{commonGroupTopId}, " +
            "#{commonGroupUpdateTime}, " +
            "#{commonGroupCreateTime})")
    int add(@Param("group") Group group);

    @Delete("delete from wvp_common_group where common_group_id = #{id}")
    int remove(@Param("id") int id);


    @Delete("delete from wvp_common_group where common_group_device_id = #{deviceId}")
    int removeByDeviceId(@Param("deviceId") String deviceId);


    @Update(value = {" <script>" +
            "UPDATE wvp_common_group " +
            "SET common_group_update_time=#{group.commonGroupUpdateTime}" +
            "<if test='group.commonGroupName != null'>, common_group_name=#{group.commonGroupName}</if>" +
            "<if test='group.commonGroupDeviceId != null'>, common_group_device_id=#{group.commonGroupDeviceId}</if>" +
            "<if test='group.commonGroupParentId != null'>, common_group_parent_id=#{group.commonGroupParentId}</if>" +
            "<if test='group.commonGroupTopId != null'>, common_group_top_id=#{group.commonGroupTopId}</if>" +
            "<if test='group.commonGroupUpdateTime != null'>, common_group_update_time=#{group.commonGroupUpdateTime}</if>" +
            "WHERE common_group_id=#{group.commonGroupId}" +
            " </script>"})
    int update(@Param("group") Group group);


    @Insert(value = "<script>" +
            "insert into wvp_common_group ( " +
            "common_group_device_id, " +
            "common_group_name, " +
            "common_group_parent_id, " +
            "common_group_top_id, " +
            "common_group_create_time, " +
            "common_group_update_time " +
            ") values " +
            "<foreach collection='allGroup' index='index' item='item' separator=',' > " +
            "( " +
            "#{item.commonGroupDeviceId}, " +
            "#{item.commonGroupName}, " +
            "<if test='item.commonGroupParentId == null'>NULL, </if>" +
            "<if test='item.commonGroupParentId != null'>#{item.commonGroupParentId}, </if>" +
            "<if test='item.commonGroupTopId == null'>NULL, </if>" +
            "<if test='item.commonGroupTopId != null'>#{item.commonGroupTopId}, </if>" +
            "#{item.commonGroupCreateTime}, " +
            "#{item.commonGroupUpdateTime} " +
            ") " +
            "</foreach>" +
            "</script>")
    int addAll(@Param("allGroup") List<Group> allGroup);

    @Select("<script> "+
            "SELECT * FROM wvp_common_group WHERE common_group_device_id in" +
            "<foreach collection='allGroup'  item='item'  open='(' separator=',' close=')' > #{item.commonGroupDeviceId}</foreach>" +
            "</script>")
    List<Group> queryInList(@Param("allGroup") List<Group> allGroup);

    @Select("<script> "+
            "select * from wvp_common_group where 1=1 " +
            "<if test='query != null'> and (common_group_device_id LIKE concat('%',#{query},'%') or common_group_name LIKE concat('%',#{query},'%') )  </if>" +
            "</script>")
    List<Group> query(@Param("query") String query);

    @Update(value = {" <script>" +
            " UPDATE wvp_common_group " +
            " SET" +
            " common_group_parent_id=#{newParentDeviceId}" +
            " WHERE common_group_parent_id=#{oldParentDeviceId}" +
            " </script>"})
    void updateParentDeviceId(@Param("oldParentDeviceId") String oldParentDeviceId, @Param("newParentDeviceId") String newParentDeviceId);

    @Select("<script> "+
            "select * from wvp_common_group where common_group_parent_id = #{groupParentId}" +
            "</script>")
    List<Group> queryChildGroupList(@Param("groupParentId") String groupParentId);

    @Select("<script> "+
            "select * from wvp_common_group where " +
            "<if test='groupParentId != null'> " +
            "common_group_top_id = #{groupParentId} and common_group_parent_id is null and common_group_device_id != common_group_top_id " +
            "</if>" +
            "<if test='groupParentId == null'> common_group_device_id = common_group_top_id</if>" +
            "</script>")
    List<Group> queryVirtualGroupList(@Param("groupParentId") String groupParentId);
}
