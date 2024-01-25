package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.BatchLimit;
import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.service.IPlatformChannelService;
import com.genersoft.iot.vmp.service.bean.Group;
import com.genersoft.iot.vmp.service.bean.Region;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author lin
 */
@Service
@DS("master")
public class PlatformChannelServiceImpl implements IPlatformChannelService {

    private final static Logger logger = LoggerFactory.getLogger(PlatformChannelServiceImpl.class);

    @Autowired
    private CommonChannelPlatformMapper platformChannelMapper;

    @Autowired
    private CommonChannelMapper commonGbChannelMapper;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Autowired
    EventPublisher eventPublisher;

    @Override
    @Transactional
    public int addChannelForGB(ParentPlatform platform, List<Integer> commonGbChannelIds) {
        assert platform != null;
        // 检查通道Id数据是否都是在数据库中存在的数据
        List<Integer> commonGbChannelIdsForSave = commonGbChannelMapper.getChannelIdsByIds(commonGbChannelIds);
        if (commonGbChannelIdsForSave.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "有效待关联通道Id为空");
        }
        // 去除已经关联的部分通道
        List<Integer> commonGbChannelIdsInDb = platformChannelMapper.findChannelsInDb(platform.getId(),
                commonGbChannelIdsForSave);
        if (!commonGbChannelIdsInDb.isEmpty()) {
            commonGbChannelIdsForSave.removeAll(commonGbChannelIdsInDb);
        }
        if (commonGbChannelIdsForSave.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "有效待关联通道Id为空");
        }
        int allCount = 0;
        if (commonGbChannelIdsForSave.size() > BatchLimit.count) {
            for (int i = 0; i < commonGbChannelIdsForSave.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > commonGbChannelIdsForSave.size()) {
                    toIndex = commonGbChannelIdsForSave.size();
                }
                int count = platformChannelMapper.addChannels(platform.getId(), commonGbChannelIdsForSave.subList(i, toIndex));
                allCount += count;
                logger.info("[关联通道]国标通道 平台：{}, 共需关联通道数:{}, 已关联：{}", platform.getServerGBId(), commonGbChannelIdsForSave.size(), allCount);
            }
        }else {
            allCount = platformChannelMapper.addChannels(platform.getId(), commonGbChannelIdsForSave);
            logger.info("[关联通道]国标通道 平台：{}, 关联通道数:{}", platform.getServerGBId(), commonGbChannelIdsForSave.size());
        }
        SubscribeInfo catalogSubscribe = subscribeHolder.getCatalogSubscribe(platform.getId());
        if (catalogSubscribe != null) {
            List<CommonGbChannel> channelList = commonGbChannelMapper.queryInIdList(commonGbChannelIdsForSave);
            if (channelList != null) {
                eventPublisher.catalogEventPublish(platform.getId(), channelList, CatalogEvent.ADD);
            }
        }
        return allCount;
    }

    @Override
    public int removeChannelForGB(ParentPlatform platform, List<Integer> commonGbChannelIds) {
        assert platform != null;
        if (commonGbChannelIds.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "有效待关联通道Id为空");
        }
        int allCount = 0;
        if (commonGbChannelIds.size() > BatchLimit.count) {
            for (int i = 0; i < commonGbChannelIds.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > commonGbChannelIds.size()) {
                    toIndex = commonGbChannelIds.size();
                }
                int count = platformChannelMapper.removeChannels(platform.getId(), commonGbChannelIds.subList(i, toIndex));
                allCount += count;
                logger.info("[关联通道]国标通道 平台：{}, 取消关联通道数:{}, 已关联：{}", platform.getServerGBId(), commonGbChannelIds.size(), allCount);
            }
        }else {
            allCount = platformChannelMapper.removeChannels(platform.getId(), commonGbChannelIds);
            logger.info("[关联通道]国标通道 平台：{}, 取消关联通道数:{}", platform.getServerGBId(), commonGbChannelIds.size());
        }
        SubscribeInfo catalogSubscribe = subscribeHolder.getCatalogSubscribe(platform.getId());
        if (catalogSubscribe != null) {
            List<CommonGbChannel> channelList = commonGbChannelMapper.queryInIdList(commonGbChannelIds);
            if (channelList != null) {
                eventPublisher.catalogEventPublish(platform.getId(), channelList, CatalogEvent.DEL);
            }
        }
        return allCount;
    }

    @Override
    public List<ParentPlatform> querySharePlatformListByChannelId(int commonGbId, List<Integer> platforms) {
        return platformChannelMapper.querySharePlatformListByChannelId(commonGbId, platforms);
    }

    @Override
    public List<CommonGbChannel> queryChannelList(ParentPlatform platform) {
        List<CommonGbChannel> result = new ArrayList<>();
        if (platform.isShareAllChannel()) {
            // 获取所有地区
            List<CommonGbChannel> allRegionList = regionMapper.queryAllForCommonChannel();
            if (!allRegionList.isEmpty()) {
                result.addAll(allRegionList);
            }
            // 获取所有分组
            List<CommonGbChannel> allGroupList = groupMapper.queryAllForCommonChannelByDeviceIdSet(null);
            if (!allGroupList.isEmpty()) {
                result.addAll(allGroupList);
            }
            // 获取所有通道
            List<CommonGbChannel> allChannelList = commonGbChannelMapper.getAll();
            if (!allChannelList.isEmpty()) {
                result.addAll(allChannelList);
            }
        }else {
            List<CommonGbChannel> channelList = commonGbChannelMapper.getShareChannelInPLatform(platform.getId());
            if (channelList.isEmpty()) {
                return result;
            }
            // 查询国标通道关联的分组以及这些分组的父级
            if (platform.isShareGroup()) {
                List<Group> groupList = groupMapper.queryAllByDeviceIds(channelList);
                if (!groupList.isEmpty()) {
                    for (Group group : groupList) {
                        result.add(CommonGbChannel.getInstance(group));
                    }
                }
            }
            // 查询国标通道关联的区域以及这些区域的父级
            if (platform.isShareRegion()) {
                List<Region> regions = regionMapper.queryAllByDeviceIds(channelList);
                if (!regions.isEmpty()) {
                    for (Region region : regions) {
                        result.add(CommonGbChannel.getInstance(region));
                    }
                }
            }
            result.addAll(channelList);
        }
        return result;
    }

    private Map<String, Group> getAllDependenceGroup(List<CommonGbChannel> commonGbChannelList) {
        Map<String, Group> result = new HashMap<>();
        // 查询这些ID对应的分组信息
        List<Group> groupList = groupMapper.queryAllByDeviceIds(commonGbChannelList);
        // 查询这些分组信息有可能涉及到的全部分组信息
        Map<String, Group> allGroupMap = groupMapper.queryAllByTopId(groupList);
        for (Group group : groupList) {
            result.put(group.getCommonGroupDeviceId(), group);
            List<Group> allParentGroup = new ArrayList<>();
            getAllParentGroup(group, allGroupMap, allParentGroup);
            if (!allParentGroup.isEmpty()) {
                for (Group parentGroup : allParentGroup) {
                    result.put(parentGroup.getCommonGroupDeviceId(), parentGroup);
                }
            }
        }
        return result;
    }

    private void getAllParentGroup(Group group, Map<String, Group> allGroupMap, List<Group> resultGroupList) {
        if (group == null
                || Objects.equals(group.getCommonGroupDeviceId(), group.getCommonGroupTopId())
                || Objects.equals(group.getCommonGroupDeviceId(), group.getCommonGroupParentId())) {
            return;
        }
        Group parentGroup = allGroupMap.get(group.getCommonGroupParentId());
        resultGroupList.add(parentGroup);
        getAllParentGroup(parentGroup, allGroupMap, resultGroupList);
    }

    @Override
    public CommonGbChannel queryChannelByPlatformIdAndChannelDeviceId(Integer platformId, String channelId) {
        return platformChannelMapper.queryChannelByPlatformIdAndChannelDeviceId(platformId, channelId);
    }

    @Override
    public List<CommonGbChannel> queryCommonGbChannellList(Integer platformId) {
        return platformChannelMapper.queryCommonGbChannellList(platformId);
    }

    @Override
    public List<CommonGbChannel> queryChannelListInRange(Integer platformId, List<CommonGbChannel> channelList) {
        return platformChannelMapper.queryChannelListInRange(platformId, channelList);
    }
}
