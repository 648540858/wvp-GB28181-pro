package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.BatchLimit;
import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.service.IPlatformChannelService;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
            // 获取所有通道
            List<CommonGbChannel> allChannelList = commonGbChannelMapper.getAll();
            if (!allChannelList.isEmpty()) {
                result.addAll(allChannelList);
                // 获取所有分组

                // 获取所有地区
            }
        }else {
            // 查询所有关联了的国标通道
            if (platform.isShareGroup()) {
                // 获取相关分组
            }
            if (platform.isShareRegion()) {

            }
        }
        return result;
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
