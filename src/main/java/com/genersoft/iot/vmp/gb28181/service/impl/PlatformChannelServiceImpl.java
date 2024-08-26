package com.genersoft.iot.vmp.gb28181.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.PlatformChannel;
import com.genersoft.iot.vmp.gb28181.bean.Region;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.GroupMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.RegionMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.beans.Transient;
import java.util.*;

/**
 * @author lin
 */
@Slf4j
@Service
@DS("master")
public class PlatformChannelServiceImpl implements IPlatformChannelService {

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private GroupMapper groupMapper;


    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private CommonGBChannelMapper commonGBChannelMapper;


    @Override
    public PageInfo<PlatformChannel> queryChannelList(int page, int count, String query, Boolean online, Integer platformId, Boolean hasShare) {
        PageHelper.startPage(page, count);
        List<PlatformChannel> all = platformChannelMapper.queryForPlatformSearch(platformId, query, online, hasShare);
        return new PageInfo<>(all);
    }

    @Override
    @Transient
    public int addAllChannel(Integer platformId) {
        List<CommonGBChannel> channelListNotShare = platformChannelMapper.queryNotShare(platformId, null);
        Assert.notEmpty(channelListNotShare, "所有通道已共享");
        int result = platformChannelMapper.addChannels(platformId, channelListNotShare);
        if (result > 0) {
            // 查询通道相关的行政区划信息是否共享，如果没共享就添加
            Set<Region> regionListNotShare =  getRegionNotShareByChannelList(channelListNotShare, platformId);
            if (!regionListNotShare.isEmpty()) {
                int addGroupResult = platformChannelMapper.addPlatformRegion(new ArrayList<>(regionListNotShare), platformId);
                if (addGroupResult > 0) {
                    for (Region region : regionListNotShare) {
                        // 分组信息排序时需要将顶层排在最后
                        channelListNotShare.add(0, CommonGBChannel.build(region));
                    }
                }
            }

            // 查询通道相关的分组信息是否共享，如果没共享就添加
            Set<Group> groupListNotShare =  getGroupNotShareByChannelList(channelListNotShare, platformId);
            if (!groupListNotShare.isEmpty()) {
                int addGroupResult = platformChannelMapper.addPlatformGroup(new ArrayList<>(groupListNotShare), platformId);
                if (addGroupResult > 0) {
                    for (Group group : groupListNotShare) {
                        // 分组信息排序时需要将顶层排在最后
                        channelListNotShare.add(0, CommonGBChannel.build(group));
                    }
                }
            }

            // 发送消息
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(platformId, channelListNotShare, CatalogEvent.ADD);
            } catch (Exception e) {
                log.warn("[关联全部通道] 发送失败，数量：{}", channelListNotShare.size(), e);
            }
        }
        return result;
    }

    /**
     * 获取通道使用的分组中未分享的
     */
    private Set<Group> getGroupNotShareByChannelList(List<CommonGBChannel> channelList, Integer platformId) {
        // 获取分组中未分享的节点
        Set<Group> groupList = groupMapper.queryNotShareGroupForPlatformByChannelList(channelList, platformId);
        // 获取这些节点的所有父节点
        if (groupList.isEmpty()) {
            return new HashSet<>();
        }
        Set<Group> allGroup = getAllGroup(groupList);
        allGroup.addAll(groupList);
        // 获取全部节点中未分享的
        return groupMapper.queryNotShareGroupForPlatformByGroupList(allGroup, platformId);
    }

    /**
     * 获取通道使用的分组中未分享的
     */
    private Set<Region> getRegionNotShareByChannelList(List<CommonGBChannel> channelList, Integer platformId) {
        // 获取分组中未分享的节点
        Set<Region> regionSet = regionMapper.queryNotShareRegionForPlatformByChannelList(channelList, platformId);
        // 获取这些节点的所有父节点
        if (regionSet.isEmpty()) {
            return new HashSet<>();
        }
        Set<Region> allRegion = getAllRegion(regionSet);
        allRegion.addAll(regionSet);
        // 获取全部节点中未分享的
        return regionMapper.queryNotShareRegionForPlatformByRegionList(allRegion, platformId);
    }

    /**
     * 移除空的共享，并返回移除的分组
     */
    private Set<Group> deleteEmptyGroup(Set<Group> groupSet, Integer platformId) {
        Iterator<Group> iterator = groupSet.iterator();
        while (iterator.hasNext()) {
            Group group = iterator.next();
            // groupSet 为当前通道直接使用的分组，如果已经没有子分组与其他的通道，则可以移除
            // 获取分组子节点
            Set<Group> children = platformChannelMapper.queryShareChildrenGroup(group.getDeviceId(), platformId);
            if (!children.isEmpty()) {
                iterator.remove();
                continue;
            }
            // 获取分组关联的通道
            List<CommonGBChannel> channelList = commonGBChannelMapper.queryShareChannelByParentId(group.getDeviceId(), platformId);
            if (!channelList.isEmpty()) {
                iterator.remove();
                continue;
            }
            platformChannelMapper.removePlatformGroupById(group.getId(), platformId);
        }
        // 如果空了，说明没有通道需要处理了
        if (groupSet.isEmpty()) {
            return new HashSet<>();
        }
        Set<Group> parent =  platformChannelMapper.queryShareParentGroupByGroupSet(groupSet, platformId);
        if (parent.isEmpty()) {
            return groupSet;
        }else {
            Set<Group> parentGroupSet = deleteEmptyGroup(parent, platformId);
            groupSet.addAll(parentGroupSet);
            return groupSet;
        }
    }

    /**
     * 移除空的共享，并返回移除的行政区划
     */
    private Set<Region> deleteEmptyRegion(Set<Region> regionSet, Integer platformId) {
        Iterator<Region> iterator = regionSet.iterator();
        while (iterator.hasNext()) {
            Region region = iterator.next();
            // groupSet 为当前通道直接使用的分组，如果已经没有子分组与其他的通道，则可以移除
            // 获取分组子节点
            Set<Region> children = platformChannelMapper.queryShareChildrenRegion(region.getDeviceId(), platformId);
            if (!children.isEmpty()) {
                iterator.remove();
                continue;
            }
            // 获取分组关联的通道
            List<CommonGBChannel> channelList = commonGBChannelMapper.queryShareChannelByCivilCode(region.getDeviceId(), platformId);
            if (!channelList.isEmpty()) {
                iterator.remove();
                continue;
            }
            platformChannelMapper.removePlatformRegionById(region.getId(), platformId);
        }
        // 如果空了，说明没有通道需要处理了
        if (regionSet.isEmpty()) {
            return new HashSet<>();
        }
        Set<Region> parent =  platformChannelMapper.queryShareParentRegionByRegionSet(regionSet, platformId);
        if (parent.isEmpty()) {
            return regionSet;
        }else {
            Set<Region> parentGroupSet = deleteEmptyRegion(parent, platformId);
            regionSet.addAll(parentGroupSet);
            return regionSet;
        }
    }

    private Set<Group> getAllGroup(Set<Group> groupList ) {
        if (groupList.isEmpty()) {
            return new HashSet<>();
        }
        Set<Group> channelList = groupMapper.queryParentInChannelList(groupList);
        if (channelList.isEmpty()) {
            return channelList;
        }
        Set<Group> allParentRegion = getAllGroup(channelList);
        channelList.addAll(allParentRegion);
        return channelList;
    }

    private Set<Region> getAllRegion(Set<Region> regionSet ) {
        if (regionSet.isEmpty()) {
            return new HashSet<>();
        }

        Set<Region> channelList = regionMapper.queryParentInChannelList(regionSet);
        if (channelList.isEmpty()) {
            return channelList;
        }
        Set<Region> allParentRegion = getAllRegion(channelList);
        channelList.addAll(allParentRegion);
        return channelList;
    }

    @Override
    @Transient
    public int addChannels(Integer platformId, List<Integer> channelIds) {
        List<CommonGBChannel> channelListNotShare = platformChannelMapper.queryNotShare(platformId, channelIds);
        Assert.notEmpty(channelListNotShare, "通道已共享");
        int result = platformChannelMapper.addChannels(platformId, channelListNotShare);
        if (result > 0) {
            // 查询通道相关的行政区划信息是否共享，如果没共享就添加
            Set<Region> regionListNotShare =  getRegionNotShareByChannelList(channelListNotShare, platformId);
            if (!regionListNotShare.isEmpty()) {
                int addGroupResult = platformChannelMapper.addPlatformRegion(new ArrayList<>(regionListNotShare), platformId);
                if (addGroupResult > 0) {
                    for (Region region : regionListNotShare) {
                        // 分组信息排序时需要将顶层排在最后
                        channelListNotShare.add(0, CommonGBChannel.build(region));
                    }
                }
            }

            // 查询通道相关的分组信息是否共享，如果没共享就添加
            Set<Group> groupListNotShare =  getGroupNotShareByChannelList(channelListNotShare, platformId);
            if (!groupListNotShare.isEmpty()) {
                int addGroupResult = platformChannelMapper.addPlatformGroup(new ArrayList<>(groupListNotShare), platformId);
                if (addGroupResult > 0) {
                    for (Group group : groupListNotShare) {
                        // 分组信息排序时需要将顶层排在最后
                        channelListNotShare.add(0, CommonGBChannel.build(group));
                    }
                }
            }

            // 发送消息
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(platformId, channelListNotShare, CatalogEvent.ADD);
            } catch (Exception e) {
                log.warn("[关联通道] 发送失败，数量：{}", channelListNotShare.size(), e);
            }
        }
        return result;
    }

    @Override
    public int removeAllChannel(Integer platformId) {
        List<CommonGBChannel> channelListShare = platformChannelMapper.queryShare(platformId,  null);
        Assert.notEmpty(channelListShare, "未共享任何通道");
        int result = platformChannelMapper.removeChannels(platformId, channelListShare);
        if (result > 0) {
            // 查询通道相关的分组信息
            Set<Region> regionSet = regionMapper.queryByChannelList(channelListShare);
            Set<Region> deleteRegion = deleteEmptyRegion(regionSet, platformId);
            if (!deleteRegion.isEmpty()) {
                for (Region region : deleteRegion) {
                    channelListShare.add(0, CommonGBChannel.build(region));
                }
            }

            // 查询通道相关的分组信息
            Set<Group> groupSet = groupMapper.queryByChannelList(channelListShare);
            Set<Group> deleteGroup = deleteEmptyGroup(groupSet, platformId);
            if (!deleteGroup.isEmpty()) {
                for (Group group : deleteGroup) {
                    channelListShare.add(0, CommonGBChannel.build(group));
                }
            }
            // 发送消息
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(platformId, channelListShare, CatalogEvent.DEL);
            } catch (Exception e) {
                log.warn("[移除全部关联通道] 发送失败，数量：{}", channelListShare.size(), e);
            }
        }
        return result;
    }

    @Override
    @Transient
    public int removeChannels(Integer platformId, List<Integer> channelIds) {
        List<CommonGBChannel> channelList = platformChannelMapper.queryShare(platformId, channelIds);
        Assert.notEmpty(channelList, "所选通道未共享");
        int result = platformChannelMapper.removeChannels(platformId, channelList);
        if (result > 0) {
            // 查询通道相关的分组信息
            Set<Region> regionSet = regionMapper.queryByChannelList(channelList);
            Set<Region> deleteRegion = deleteEmptyRegion(regionSet, platformId);
            if (!deleteRegion.isEmpty()) {
                for (Region region : deleteRegion) {
                    channelList.add(0, CommonGBChannel.build(region));
                }
            }

            // 查询通道相关的分组信息
            Set<Group> groupSet = groupMapper.queryByChannelList(channelList);
            Set<Group> deleteGroup = deleteEmptyGroup(groupSet, platformId);
            if (!deleteGroup.isEmpty()) {
                for (Group group : deleteGroup) {
                    channelList.add(0, CommonGBChannel.build(group));
                }
            }
            // 发送消息
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(platformId, channelList, CatalogEvent.DEL);
            } catch (Exception e) {
                log.warn("[移除关联通道] 发送失败，数量：{}", channelList.size(), e);
            }
        }
        return result;
    }
}
