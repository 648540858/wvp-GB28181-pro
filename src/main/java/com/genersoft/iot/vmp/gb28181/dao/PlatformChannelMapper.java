package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mapper
@Repository
public interface PlatformChannelMapper {

    int addChannels(@Param("platformId") Integer platformId, @Param("channelList") List<CommonGBChannel> channelList);

    int delChannelForDeviceId(String deviceId);

    List<Device> queryDeviceByPlatformIdAndChannelId(@Param("platformId") String platformId, @Param("channelId") String channelId);

    List<Platform> queryPlatFormListForGBWithGBId(@Param("channelId") Integer channelId, List<String> platforms);

    List<Device> queryDeviceInfoByPlatformIdAndChannelId(@Param("platformId") String platformId, @Param("channelId") String channelId);

    List<Platform> queryParentPlatformByChannelId(@Param("channelId") String channelId);

    List<PlatformChannel> queryForPlatformForWebList(@Param("platformId") Integer platformId, @Param("query") String query,
                                                     @Param("dataType") Integer dataType, @Param("online") Boolean online,
                                                     @Param("hasShare") Boolean hasShare);

    List<CommonGBChannel> queryOneWithPlatform(@Param("platformId") Integer platformId, @Param("channelDeviceId") String channelDeviceId);

    List<CommonGBChannel> queryNotShare(@Param("platformId") Integer platformId, List<Integer> channelIds);

    List<CommonGBChannel> queryShare(@Param("platformId") Integer platformId, List<Integer> channelIds);

    int removeChannelsWithPlatform(@Param("platformId") Integer platformId, List<CommonGBChannel> channelList);

    int removeChannels(List<CommonGBChannel> channelList);

    int addPlatformGroup(Collection<Group> groupListNotShare, @Param("platformId") Integer platformId);

    int addPlatformRegion(List<Region> regionListNotShare, @Param("platformId") Integer platformId);

    int removePlatformGroup(List<Group> groupList, @Param("platformId") Integer platformId);

    void removePlatformGroupById(@Param("id") int id, @Param("platformId") Integer platformId);

    void removePlatformRegionById(@Param("id") int id, @Param("platformId") Integer platformId);

    Set<Group> queryShareChildrenGroup(@Param("parentId") Integer parentId, @Param("platformId") Integer platformId);

    Set<Region> queryShareChildrenRegion(@Param("parentId") String parentId, @Param("platformId") Integer platformId);

    Set<Group> queryShareParentGroupByGroupSet(Set<Group> groupSet, @Param("platformId") Integer platformId);

    Set<Region> queryShareParentRegionByRegionSet(Set<Region> regionSet, @Param("platformId") Integer platformId);

    List<Platform> queryPlatFormListByChannelList(Collection<Integer> ids);

    List<Platform> queryPlatFormListByChannelId(@Param("channelId") int channelId);

    void removeChannelsByPlatformId(@Param("platformId") Integer platformId);

    void removePlatformGroupsByPlatformId(@Param("platformId") Integer platformId);

    void removePlatformRegionByPlatformId(@Param("platformId") Integer platformId);

    void updateCustomChannel(PlatformChannel channel);

    CommonGBChannel queryShareChannel(@Param("platformId") int platformId, @Param("gbId") int gbId);

    Set<Group> queryShareGroup(@Param("platformId") Integer platformId);

    Set<Region> queryShareRegion(Integer id);
}
