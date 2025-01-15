package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Region;
import com.genersoft.iot.vmp.gb28181.bean.RegionTree;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface RegionMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void add(Region region);

    int delete(@Param("id") int id);

    int update(Region region);

    List<Region> query(@Param("query") String query, @Param("parentId") String parentId);

    List<Region> getChildren(@Param("parentId") Integer parentId);

    Region queryOne(@Param("id") int id);

    List<String> getUninitializedCivilCode();

    List<String> queryInList(Set<String> codes);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int batchAdd(List<Region> regionList);

    List<RegionTree> queryForTree(@Param("query") String query, @Param("parentId") Integer parentId);

    void batchDelete(List<Region> allChildren);

    List<Region> queryInRegionListByDeviceId(List<Region> regionList);

    List<CommonGBChannel> queryByPlatform(@Param("platformId") Integer platformId);

    void updateParentId(List<Region> regionListForAdd);

    void updateChild(@Param("parentId") int parentId, @Param("parentDeviceId") String parentDeviceId);

    Region queryByDeviceId(@Param("deviceId") String deviceId);

    Set<Region> queryParentInChannelList(Set<Region> regionSet);

    Set<Region> queryByChannelList(List<CommonGBChannel> channelList);

    Set<Region> queryNotShareRegionForPlatformByChannelList(List<CommonGBChannel> channelList, @Param("platformId") Integer platformId);

    Set<Region> queryNotShareRegionForPlatformByRegionList(Set<Region> allRegion, @Param("platformId") Integer platformId);

}
