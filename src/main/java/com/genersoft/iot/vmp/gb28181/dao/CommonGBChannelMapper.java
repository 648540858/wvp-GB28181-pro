package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Mapper
@Repository
public interface CommonGBChannelMapper {

    CommonGBChannel queryByDeviceId(@Param("gbDeviceId") String gbDeviceId);

    @Options(useGeneratedKeys = true, keyProperty = "gbId", keyColumn = "id")
    int insert(CommonGBChannel commonGBChannel);

    CommonGBChannel queryById(@Param("gbId") int gbId);

    void delete(int gbId);

    int update(CommonGBChannel commonGBChannel);

    int updateStatusById(@Param("gbId") int gbId, @Param("status") String status);

    int updateStatusForListById(List<CommonGBChannel> commonGBChannels, @Param("status") String status);

    List<CommonGBChannel> queryInListByStatus(List<CommonGBChannel> commonGBChannelList, @Param("status") String status);

    int batchAdd(List<CommonGBChannel> commonGBChannels);

    int updateStatus(List<CommonGBChannel> commonGBChannels);

    void reset(@Param("id") int id, @Param("dataType") Integer dataType, @Param("dataDeviceId") int dataDeviceId, @Param("updateTime") String updateTime);

    List<CommonGBChannel> queryByIds(Collection<Integer> ids);

    void batchDelete(List<CommonGBChannel> channelListInDb);

    List<CommonGBChannel> queryListByCivilCode(@Param("query") String query, @Param("online") Boolean online,
                                               @Param("dataType") Integer dataType, @Param("civilCode") String civilCode);

    List<CommonGBChannel> queryListByParentId(@Param("query") String query, @Param("online") Boolean online,
                                              @Param("dataType") Integer dataType, @Param("groupDeviceId") String groupDeviceId);


    List<RegionTree> queryForRegionTreeByCivilCode(@Param("query") String query, @Param("parentDeviceId") String parentDeviceId);

    int removeCivilCode(List<Region> allChildren);

    int updateRegion(@Param("civilCode") String civilCode, @Param("channelList") List<CommonGBChannel> channelList);

    List<CommonGBChannel> queryByIdsOrCivilCode(@Param("civilCode") String civilCode, @Param("ids") List<Integer> ids);

    int removeCivilCodeByChannels(List<CommonGBChannel> channelList);

    List<CommonGBChannel> queryByCivilCode(@Param("civilCode") String civilCode);

    List<CommonGBChannel> queryByGbDeviceIds(@Param("dataType") Integer dataType, List<Integer> deviceIds);

    List<Integer> queryByGbDeviceIdsForIds(@Param("dataType") Integer dataType, List<Integer> deviceIds);

    List<CommonGBChannel> queryByGroupList(List<Group> groupList);

    int removeParentIdByChannels(List<CommonGBChannel> channelList);

    List<CommonGBChannel> queryByBusinessGroup(@Param("businessGroup") String businessGroup);

    List<CommonGBChannel> queryByParentId(@Param("parentId") String parentId);

    int updateBusinessGroupByChannelList(@Param("businessGroup") String businessGroup, List<CommonGBChannel> channelList);

    int updateParentIdByChannelList(@Param("parentId") String parentId, List<CommonGBChannel> channelList);

    List<GroupTree> queryForGroupTreeByParentId(@Param("query") String query, @Param("parent") String parent);

    int updateGroup(@Param("parentId") String parentId, @Param("businessGroup") String businessGroup,
                    List<CommonGBChannel> channelList);

    int batchUpdate(List<CommonGBChannel> commonGBChannels);

    List<CommonGBChannel> queryWithPlatform(@Param("platformId") Integer platformId);

    List<CommonGBChannel> queryShareChannelByParentId(@Param("parentId") String parentId, @Param("platformId") Integer platformId);

    List<CommonGBChannel> queryShareChannelByCivilCode(@Param("civilCode") String civilCode, @Param("platformId") Integer platformId);

    int updateCivilCodeByChannelList(@Param("civilCode") String civilCode, List<CommonGBChannel> channelList);

    List<CommonGBChannel> queryListByStreamPushList(@Param("dataType") Integer dataType, List<StreamPush> streamPushList);

    void updateGpsByDeviceIdForStreamPush(@Param("dataType") Integer dataType, List<CommonGBChannel> channels);

    List<CommonGBChannel> queryList(@Param("query") String query, @Param("online") Boolean online,
                                    @Param("hasRecordPlan") Boolean hasRecordPlan, @Param("dataType") Integer dataType);

    void removeRecordPlan(List<Integer> channelIds);

    void addRecordPlan(List<Integer> channelIds, @Param("planId") Integer planId);

    void addRecordPlanForAll(@Param("planId") Integer planId);

    void removeRecordPlanByPlanId(@Param("planId") Integer planId);

    List<CommonGBChannel> queryForRecordPlanForWebList(@Param("planId") Integer planId, @Param("query") String query,
                                                       @Param("dataType") Integer dataType, @Param("online") Boolean online,
                                                       @Param("hasLink") Boolean hasLink);

    CommonGBChannel queryByDataId(@Param("dataType") Integer dataType, @Param("dataDeviceId") Integer dataDeviceId);
}
