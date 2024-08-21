package com.genersoft.iot.vmp.gb28181.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.PlatformChannel;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.GroupMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            // 查询通道相关的分组信息是否共享，如果没共享就添加
            Set<Group> groupListNotShare =  getGroupNotShareByChannelList(channelListNotShare, platformId);
            int addGroupResult = platformChannelMapper.addPlatformGroup(new ArrayList<>(groupListNotShare), platformId);
            if (addGroupResult > 0) {
                for (Group group : groupListNotShare) {
                    // 分组信息排序时需要将顶层排在最后
                    channelListNotShare.add(0, CommonGBChannel.build(group));
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
        Set<Group> groupList = groupMapper.queryNotShareForPlatformByChannelList(channelList, platformId);
        // 获取这些节点的所有父节点
        if (groupList.isEmpty()) {
            return new HashSet<>();
        }
        Set<Group> allGroup = getAllGroup(groupList);
        // 获取全部节点中未分享的
        return groupMapper.queryNotShareForPlatformByGroupList(allGroup, platformId);
    }

    /**
     * 移除空的共享，并返回移除的分组
     */
    private Set<Group> deleteEmptyGroup(Set<Group> groupSet, Integer platformId) {
        for (Group group : groupSet) {
            // 获取分组子节点
            List<Group> children = platformChannelMapper.getShareChildrenGroup(group.getDeviceId(), platformId);
            if (!children.isEmpty()) {
                groupSet.remove(group);
                continue;
            }
            // 获取分组关联的通道
            List<CommonGBChannel> channelList = platformChannelMapper.queryShareChannelByParentId(group.getDeviceId(), platformId);
            if (!channelList.isEmpty()) {
                groupSet.remove(group);
                continue;
            }
            platformChannelMapper.removePlatformGroupById(group.getId(), platformId);
        }
        if (!groupSet.isEmpty()) {

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

    @Override
    @Transient
    public int addChannels(Integer platformId, List<Integer> channelIds) {
        List<CommonGBChannel> channelListNotShare = platformChannelMapper.queryNotShare(platformId, channelIds);
        Assert.notEmpty(channelListNotShare, "通道已共享");
        int result = platformChannelMapper.addChannels(platformId, channelListNotShare);
        if (result > 0) {
            // 查询通道相关的分组信息是否共享，如果没共享就添加
            Set<Group> groupListNotShare =  getGroupNotShareByChannelList(channelListNotShare, platformId);
            int addGroupResult = platformChannelMapper.addPlatformGroup(new ArrayList<>(groupListNotShare), platformId);
            if (addGroupResult > 0) {
                for (Group group : groupListNotShare) {
                    // 分组信息排序时需要将顶层排在最后
                    channelListNotShare.add(0, CommonGBChannel.build(group));
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
        List<CommonGBChannel> channelListNotShare = platformChannelMapper.queryNotShare(platformId,  null);
        Assert.notEmpty(channelListNotShare, "未共享任何通道");
        int result = platformChannelMapper.removeChannels(platformId, channelListNotShare);
        if (result > 0) {
            // 查询通道相关的分组信息是否共享，如果没共享就添加
            Set<Group> groupSet = groupMapper.queryByChannelList(channelListNotShare);
            Set<Group> deleteGroup = deleteEmptyGroup(groupSet, platformId);
            if (!deleteGroup.isEmpty()) {
                channelListNotShare.add(0, CommonGBChannel.build(group));
            }
            // 发送消息
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(platformId, channelListNotShare, CatalogEvent.DEL);
            } catch (Exception e) {
                log.warn("[移除全部关联通道] 发送失败，数量：{}", channelListNotShare.size(), e);
            }
        }
        return result;
    }

    @Override
    public int removeChannels(Integer platformId, List<Integer> channelIds) {
        List<CommonGBChannel> channelList = platformChannelMapper.queryShare(platformId, channelIds);
        Assert.notEmpty(channelList, "所选通道未共享");
        int result = platformChannelMapper.removeChannels(platformId, channelList);
        if (result > 0) {
            // 查询通道相关的分组信息是否共享，如果没共享就添加
            List<Group> groupListShareEmptyChannel =  getGroupShareEmptyChannel(channelList, platformId);
            int addGroupResult = platformChannelMapper.removePlatformGroup(groupListShareEmptyChannel, platformId);
            if (addGroupResult > 0) {
                for (Group group : groupListShareEmptyChannel) {
                    // 分组信息排序时需要将顶层排在最后
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
