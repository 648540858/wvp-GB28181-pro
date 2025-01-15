package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.GroupTree;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface GroupMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(Group group);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int addBusinessGroup(Group group);

    int delete(@Param("id") int id);

    int update(Group group);

    List<Group> query(@Param("query") String query, @Param("parentId") String parentId, @Param("businessGroup") String businessGroup);

    List<Group> getChildren(@Param("parentId") int parentId);

    Group queryOne(@Param("id") int id);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int batchAdd(List<Group> groupList);

    List<GroupTree> queryForTree(@Param("query") String query, @Param("parentId") Integer parentId);

    List<GroupTree> queryForTreeByBusinessGroup(@Param("query") String query,
                                                @Param("businessGroup") String businessGroup);

    List<GroupTree> queryBusinessGroupForTree(@Param("query") String query);

    Group queryOneByDeviceId(@Param("deviceId") String deviceId, @Param("businessGroup") String businessGroup);

    int batchDelete(List<Group> allChildren);

    Group queryBusinessGroup(@Param("businessGroup") String businessGroup);

    List<Group> queryByBusinessGroup(@Param("businessGroup") String businessGroup);

    int deleteByBusinessGroup(@Param("businessGroup") String businessGroup);

    int updateChild(@Param("parentId") Integer parentId, Group group);

    List<Group> queryInGroupListByDeviceId(List<Group> groupList);

    Set<Group> queryInChannelList(List<CommonGBChannel> channelList);

    Set<Group> queryParentInChannelList(Set<Group> groupSet);

    List<CommonGBChannel> queryForPlatform(@Param("platformId") Integer platformId);

    Set<Group> queryNotShareGroupForPlatformByChannelList(List<CommonGBChannel> channelList, @Param("platformId") Integer platformId);

    Set<Group> queryNotShareGroupForPlatformByGroupList(Set<Group> allGroup, @Param("platformId") Integer platformId);

    Set<Group> queryByChannelList(List<CommonGBChannel> channelList);

    void updateParentId(List<Group> groupListForAdd);

    void updateParentIdWithBusinessGroup(List<Group> groupListForAdd);

    List<Platform> queryForPlatformByGroupId(@Param("groupId") int groupId);

    void deletePlatformGroup(@Param("groupId") int groupId);
}
