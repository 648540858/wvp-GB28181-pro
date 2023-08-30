package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.service.bean.BusinessGroup;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BusinessGroupMapper {

    @Select(value = " <script>" +
            " select * from wvp_common_business_group " +
            " WHERE 1=1 " +
            " <if test='parentId != null' >  AND common_business_group_parent_id = #{parentId}</if>" +
            " <if test='parentId == null' >  AND common_business_group_parent_id is null </if>" +
            " order by common_business_group_id ASC " +
            " </script>")
    List<BusinessGroup> getNodes(String parentId);

    @Select(" select * from wvp_common_business_group " +
            " WHERE common_business_group_id = #{id} ")
    BusinessGroup query(int id);

    @Select(" select * from wvp_common_business_group " +
            " WHERE common_business_group_device_id = #{deviceId} ")
    BusinessGroup queryByDeviceId(String deviceId);

    @Insert("INSERT INTO wvp_common_business_group (" +
            "common_business_group_device_id, " +
            "common_business_group_name, " +
            "common_business_group_parent_id, " +
            "common_business_group_path, " +
            "common_business_group_update_time, " +
            "common_business_group_create_time ) " +
            "VALUES (" +
            "#{commonBusinessGroupDeviceId}, " +
            "#{commonBusinessGroupName}, " +
            "#{commonBusinessGroupParentId}, " +
            "#{commonBusinessGroupPath}, " +
            "#{commonBusinessGroupUpdateTime}, " +
            "#{commonBusinessGroupCreateTime})")
    int add(BusinessGroup businessGroup);

    @Delete("delete from wvp_common_business_group where common_business_group_id = #{id}")
    int remove(int id);


    @Delete("delete from wvp_common_business_group where common_business_group_device_id = #{deviceId}")
    int removeByDeviceId(String deviceId);


    @Update(value = {" <script>" +
            "UPDATE wvp_common_business_group " +
            "SET common_business_group_update_time=#{commonBusinessGroupUpdateTime}" +
            "<if test='commonBusinessGroupName != null'>, common_business_group_name=#{commonBusinessGroupName}</if>" +
            "<if test='commonBusinessGroupDeviceId != null'>, common_business_group_device_id=#{commonBusinessGroupDeviceId}</if>" +
            "<if test='commonBusinessGroupParentId != null'>, common_business_group_parent_id=#{commonBusinessGroupParentId}</if>" +
            "<if test='commonBusinessGroupPath != null'>, common_business_group_path=#{commonBusinessGroupPath}</if>" +
            "<if test='commonBusinessGroupUpdateTime != null'>, common_business_group_update_time=#{commonBusinessGroupUpdateTime}</if>" +
            "WHERE common_business_group_id=#{commonBusinessGroupId}" +
            " </script>"})
    int update(BusinessGroup businessGroup);
}
