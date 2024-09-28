package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.GroupTree;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

@Mapper
public interface GroupMapper {

    @Insert("INSERT INTO wvp_common_group (device_id, name, parent_id, parent_device_id, business_group, create_time, update_time, civil_code) " +
            "VALUES (#{deviceId}, #{name}, #{parentId}, #{parentDeviceId}, #{businessGroup}, #{createTime}, #{updateTime}, #{civilCode})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(Group group);

    @Insert("INSERT INTO wvp_common_group (device_id, name, business_group, create_time, update_time, civil_code) " +
            "VALUES (#{deviceId}, #{name}, #{businessGroup}, #{createTime}, #{updateTime}, #{civilCode})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int addBusinessGroup(Group group);

    @Delete("DELETE FROM wvp_common_group WHERE id=#{id}")
    int delete(@Param("id") int id);

    @Update(" UPDATE wvp_common_group " +
            " SET update_time=#{updateTime}, device_id=#{deviceId}, name=#{name}, parent_id=#{parentId}, " +
            " parent_device_id=#{parentDeviceId}, business_group=#{businessGroup}, civil_code=#{civilCode}" +
            " WHERE id = #{id}")
    int update(Group group);

    @Select(value = {" <script>" +
            "SELECT * from wvp_common_group WHERE 1=1 " +
            " <if test='query != null'> AND (device_id LIKE concat('%',#{query},'%') OR name LIKE concat('%',#{query},'%'))</if> " +
            " <if test='parentId != null and businessGroupId != null '> AND parent_device_id = #{parentId} AND business_group=#{businessGroup} </if> " +
            "ORDER BY id " +
            " </script>"})
    List<Group> query(@Param("query") String query, @Param("parentId") String parentId, @Param("businessGroup") String businessGroup);

    @Select("SELECT * from wvp_common_group WHERE parent_id = #{parentId} ")
    List<Group> getChildren(@Param("parentId") int parentId);

    @Select("SELECT * from wvp_common_group WHERE id = #{id} ")
    Group queryOne(@Param("id") int id);


    @Insert(" <script>" +
            " INSERT INTO wvp_common_group (" +
            " device_id," +
            " name, " +
            " parent_device_id," +
            " parent_id," +
            " business_group," +
            " create_time," +
            " civil_code," +
            " update_time) " +
            " VALUES " +
            " <foreach collection='groupList' index='index' item='item' separator=','> " +
            " (#{item.deviceId}, #{item.name}, #{item.parentDeviceId}, #{item.parentId}, #{item.businessGroup},#{item.createTime},#{item.civilCode},#{item.updateTime})" +
            " </foreach> " +
            " </script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int batchAdd(List<Group> groupList);

    @Select(" <script>" +
            " SELECT " +
            " * , " +
            " concat('group', id) as tree_id," +
            " 0 as type," +
            " false as is_leaf" +
            " from wvp_common_group " +
            " where 1=1 " +
            " <if test='parentId != null'> and parent_id = #{parentId} </if> " +
            " <if test='parentId == null'> and parent_id is null </if> " +
            " <if test='query != null'> AND (device_id LIKE concat('%',#{query},'%') OR name LIKE concat('%',#{query},'%'))</if> " +
            " </script>")
    List<GroupTree> queryForTree(@Param("query") String query, @Param("parentId") Integer parentId);

    @Select(" <script>" +
            " SELECT " +
            " * , " +
            " 0 as type," +
            " false as is_leaf" +
            " from wvp_common_group " +
            " where parent_id is not null and business_group = #{businessGroup} and device_id != #{businessGroup}" +
            " <if test='query != null'> AND (device_id LIKE concat('%',#{query},'%') OR name LIKE concat('%',#{query},'%'))</if> " +
            " </script>")
    List<GroupTree> queryForTreeByBusinessGroup(@Param("query") String query,
                                                @Param("businessGroup") String businessGroup);

    @Select(" <script>" +
            " SELECT " +
            " *," +
            " 0 as type," +
            " false as is_leaf" +
            " from wvp_common_group " +
            " where device_id=business_group" +
            " <if test='query != null'> AND (device_id LIKE concat('%',#{query},'%') OR name LIKE concat('%',#{query},'%'))</if> " +
            " </script>")
    List<GroupTree> queryBusinessGroupForTree(@Param("query") String query);

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

    @Update(" UPDATE wvp_common_group " +
            " SET parent_device_id=#{group.deviceId}, business_group = #{group.businessGroup}" +
            " WHERE parent_id = #{parentId}")
    int updateChild(@Param("parentId") Integer parentId, Group group);

    @Select(" <script>" +
            " SELECT * from wvp_common_group " +
            " where device_id in " +
            " <foreach collection='groupList'  item='item'  open='(' separator=',' close=')' > #{item.deviceId}</foreach>" +
            " </script>")
    List<Group> queryInGroupListByDeviceId(List<Group> groupList);

    @Select(" <script>" +
            " SELECT " +
            " * " +
            " from wvp_common_group " +
            " where (device_id, business_group) in " +
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > (#{item.gbParentId}, #{item.gbBusinessGroupId})</foreach>" +
            " </script>")
    Set<Group> queryInChannelList(List<CommonGBChannel> channelList);

    @Select(" <script>" +
            " SELECT " +
            " * " +
            " from wvp_common_group " +
            " where id in " +
            " <foreach collection='groupSet'  item='item'  open='(' separator=',' close=')' > #{item.parentId}</foreach>" +
            " </script>")
    Set<Group> queryParentInChannelList(Set<Group> groupSet);

    @Select(" <script>" +
            " SELECT " +
            " wcg.device_id as gb_device_id," +
            " wcg.name as gb_name," +
            " wcg.business_group as gb_business_group," +
            " 1 as gb_parental," +
            " wcg.parent_device_id as gb_parent_id" +
            " from wvp_common_group wcg" +
            " left join wvp_platform_group wpg on wpg.group_id = wcg.id" +
            " where wpg.platform_id = #{platformId} " +
            " </script>")
    List<CommonGBChannel> queryForPlatform(@Param("platformId") Integer platformId);

    @Select(" <script>" +
            " SELECT * " +
            " from wvp_common_group wcg" +
            " left join wvp_platform_group wpg on wpg.group_id = wcg.id and wpg.platform_id = #{platformId}" +
            " where wpg.platform_id is null and wcg.device_id in " +
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > #{item.gbParentId}</foreach>" +
            " </script>")
    Set<Group> queryNotShareGroupForPlatformByChannelList(List<CommonGBChannel> channelList, @Param("platformId") Integer platformId);

    @Select(" <script>" +
            " SELECT * " +
            " from wvp_common_group wcg" +
            " left join wvp_platform_group wpg on wpg.group_id = wcg.id and wpg.platform_id = #{platformId}" +
            " where wpg.platform_id IS NULL and wcg.id in " +
            " <foreach collection='allGroup'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>")
    Set<Group> queryNotShareGroupForPlatformByGroupList(Set<Group> allGroup, @Param("platformId") Integer platformId);


    @Select(" <script>" +
            " SELECT " +
            " * " +
            " from wvp_common_group " +
            " where device_id in " +
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > #{item.gbParentId}</foreach>" +
            " order by id " +
            "</script>")
    Set<Group> queryByChannelList(List<CommonGBChannel> channelList);

    @Update(value = " <script>" +
            " update wvp_common_group w1 " +
            " inner join (select * from wvp_common_group ) w2 on w1.parent_device_id = w2.device_id " +
            " set w1.parent_id = w2.id" +
            " where w1.id in " +
            " <foreach collection='groupListForAdd'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>", databaseId = "mysql")
    @Update( value = " <script>" +
            " update wvp_common_group w1\n" +
            " set parent_id = w2.id\n" +
            " from wvp_common_group w2\n" +
            " where w1.parent_device_id = w2.device_id\n" +
            "  and w1.id in " +
            " <foreach collection='groupListForAdd'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>", databaseId = "postgresql")
    @Update( value = " <script>" +
            " update wvp_common_group w1\n" +
            " set parent_id = w2.id\n" +
            " from wvp_common_group w2\n" +
            " where w1.parent_device_id = w2.device_id\n" +
            "  and w1.id in " +
            " <foreach collection='groupListForAdd'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>", databaseId = "kingbase")
    void updateParentId(List<Group> groupListForAdd);

    @Update(value = " <script>" +
            " update wvp_common_group w1 " +
            "    inner join (select * from wvp_common_group ) w2" +
            "    on w1.parent_device_id is null" +
            "           and w2.parent_device_id is null" +
            "           and w2.device_id = w2.business_group " +
            "           and w1.business_group = w2.device_id " +
            "            and w1.device_id != w1.business_group " +
            " set w1.parent_id = w2.id" +
            " where w1.id in " +
            " <foreach collection='groupListForAdd'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>", databaseId = "mysql")
    @Update( value = " <script>" +
            " update wvp_common_group w1 " +
            " set parent_id = w2.id " +
            " from wvp_common_group w2 " +
            " where w1.parent_device_id is null " +
            "       and w2.parent_device_id is null " +
            "       and w2.device_id = w2.business_group " +
            "       and w1.business_group = w2.device_id " +
            "       and w1.device_id != w1.business_group " +
            "       and w1.id in " +
            " <foreach collection='groupListForAdd'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>", databaseId = "kingbase")
    @Update( value = " <script>" +
            " update wvp_common_group w1 " +
            " set parent_id = w2.id " +
            " from wvp_common_group w2 " +
            " where w1.parent_device_id is null " +
            "       and w2.parent_device_id is null " +
            "       and w2.device_id = w2.business_group " +
            "       and w1.business_group = w2.device_id " +
            "       and w1.device_id != w1.business_group " +
            "       and w1.id in " +
            " <foreach collection='groupListForAdd'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>", databaseId = "postgresql")
    void updateParentIdWithBusinessGroup(List<Group> groupListForAdd);

    @Select(" <script>" +
            " SELECT " +
            " wp.* " +
            " from wvp_platform_group wpg " +
            " left join wvp_platform wp on wp.id = wpg.platform_id " +
            " where wpg.group_id = #{groupId} " +
            "</script>")
    List<Platform> queryForPlatformByGroupId(@Param("groupId") int groupId);

    @Delete("DELETE FROM wvp_platform_group WHERE group_id = #{groupId}")
    void deletePlatformGroup(@Param("groupId") int groupId);
}
