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
    List<Group> getNodes(String parentId);

    @Select(" select * from wvp_common_group " +
            " WHERE common_group_id = #{id} ")
    Group query(int id);

    @Select(" select * from wvp_common_group " +
            " WHERE common_group_device_id = #{deviceId} ")
    Group queryByDeviceId(String deviceId);

    @Insert("INSERT INTO wvp_common_group (" +
            "common_group_device_id, " +
            "common_group_name, " +
            "common_group_parent_id, " +
            "common_group_update_time, " +
            "common_group_create_time ) " +
            "VALUES (" +
            "#{commonGroupDeviceId}, " +
            "#{commonGroupName}, " +
            "#{commonGroupParentId}, " +
            "#{commonGroupUpdateTime}, " +
            "#{commonGroupCreateTime})")
    int add(Group group);

    @Delete("delete from wvp_common_group where common_group_id = #{id}")
    int remove(int id);


    @Delete("delete from wvp_common_group where common_group_device_id = #{deviceId}")
    int removeByDeviceId(String deviceId);


    @Update(value = {" <script>" +
            "UPDATE wvp_common_group " +
            "SET common_group_update_time=#{commonGroupUpdateTime}" +
            "<if test='commonGroupName != null'>, common_group_name=#{commonGroupName}</if>" +
            "<if test='commonGroupDeviceId != null'>, common_group_device_id=#{commonGroupDeviceId}</if>" +
            "<if test='commonGroupParentId != null'>, common_group_parent_id=#{commonGroupParentId}</if>" +
            "<if test='commonGroupUpdateTime != null'>, common_group_update_time=#{commonGroupUpdateTime}</if>" +
            "WHERE common_group_id=#{commonGroupId}" +
            " </script>"})
    int update(Group Group);


    @Insert(value = "<script>" +
            "insert into wvp_common_group ( " +
            "common_group_device_id, " +
            "common_group_name, " +
            "common_group_parent_id, " +
            "common_group_create_time, " +
            "common_group_update_time " +
            ") values " +
            "<foreach collection='allGroup' index='index' item='item' separator=','> " +
            "( " +
            "#{item.commonGroupDeviceId}, " +
            "#{item.commonGroupName}, " +
            "#{item.commonGroupParentId}, " +
            "#{item.commonGroupCreateTime}, " +
            "#{item.commonGroupUpdateTime} " +
            ")" +
            "</foreach>" +
            "</script>")
    int addAll(List<Group> allGroup);
}
