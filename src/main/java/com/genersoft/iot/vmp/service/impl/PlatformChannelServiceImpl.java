package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.service.IPlatformChannelService;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.ParentPlatformMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformCatalogMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lin
 */
@Service
public class PlatformChannelServiceImpl implements IPlatformChannelService {

    private final static Logger logger = LoggerFactory.getLogger(PlatformChannelServiceImpl.class);

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    private SubscribeHolder subscribeHolder;


    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    @Autowired
    private PlatformCatalogMapper catalogManager;

    @Autowired
    private ParentPlatformMapper platformMapper;

    @Autowired
    EventPublisher eventPublisher;

    @Override
    public int updateChannelForGB(String platformId, List<ChannelReduce> channelReduces, String catalogId) {
        ParentPlatform platform = platformMapper.getParentPlatByServerGBId(platformId);
        if (platform == null) {
            logger.warn("更新级联通道信息时未找到平台{}的信息", platformId);
            return 0;
        }
        Map<Integer, ChannelReduce> deviceAndChannels = new HashMap<>();
        for (ChannelReduce channelReduce : channelReduces) {
            channelReduce.setCatalogId(catalogId);
            deviceAndChannels.put(channelReduce.getId(), channelReduce);
        }
        List<Integer> deviceAndChannelList = new ArrayList<>(deviceAndChannels.keySet());
        // 查询当前已经存在的
        List<Integer> channelIds = platformChannelMapper.findChannelRelatedPlatform(platformId, channelReduces);
        if (deviceAndChannelList != null) {
            deviceAndChannelList.removeAll(channelIds);
        }
        for (Integer channelId : channelIds) {
            deviceAndChannels.remove(channelId);
        }
        List<ChannelReduce> channelReducesToAdd = new ArrayList<>(deviceAndChannels.values());
        // 对剩下的数据进行存储
        int allCount = 0;
        boolean result = false;
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        int limitCount = 50;
        if (channelReducesToAdd.size() > 0) {
            if (channelReducesToAdd.size() > limitCount) {
                for (int i = 0; i < channelReducesToAdd.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > channelReducesToAdd.size()) {
                        toIndex = channelReducesToAdd.size();
                    }
                    int count = platformChannelMapper.addChannels(platformId, channelReducesToAdd.subList(i, toIndex));
                    result = result || count < 0;
                    allCount += count;
                    logger.info("[关联通道]国标通道 平台：{}, 共需关联通道数:{}, 已关联：{}", platformId, channelReducesToAdd.size(), toIndex);
                }
            }else {
                allCount = platformChannelMapper.addChannels(platformId, channelReducesToAdd);
                result = result || allCount < 0;
                logger.info("[关联通道]国标通道 平台：{}, 关联通道数:{}", platformId, channelReducesToAdd.size());
            }

            if (result) {
                //事务回滚
                dataSourceTransactionManager.rollback(transactionStatus);
                allCount = 0;
            }else {
                logger.info("[关联通道]国标通道 平台：{}, 正在存入数据库", platformId);
                dataSourceTransactionManager.commit(transactionStatus);

            }
            SubscribeInfo catalogSubscribe = subscribeHolder.getCatalogSubscribe(platformId);
            if (catalogSubscribe != null) {
                List<DeviceChannel> deviceChannelList = getDeviceChannelListByChannelReduceList(channelReducesToAdd, catalogId, platform);
                if (deviceChannelList != null) {
                    eventPublisher.catalogEventPublish(platformId, deviceChannelList, CatalogEvent.ADD);
                }
            }
            logger.info("[关联通道]国标通道 平台：{}, 存入数据库成功", platformId);
        }
        return allCount;
    }

    private List<DeviceChannel> getDeviceChannelListByChannelReduceList(List<ChannelReduce> channelReduces, String catalogId, ParentPlatform platform) {
        List<DeviceChannel> deviceChannelList = new ArrayList<>();
        if (channelReduces.size() > 0){
            PlatformCatalog catalog = catalogManager.selectByPlatFormAndCatalogId(platform.getServerGBId(),catalogId);
            if (catalog == null && catalogId.equals(platform.getDeviceGBId())) {
                for (ChannelReduce channelReduce : channelReduces) {
                    DeviceChannel deviceChannel = deviceChannelMapper.queryChannel(channelReduce.getDeviceId(), channelReduce.getChannelId());
                    deviceChannel.setParental(0);
                    deviceChannel.setCivilCode(platform.getServerGBDomain());
                    deviceChannelList.add(deviceChannel);
                }
                return deviceChannelList;
            } else if (catalog == null || !catalogId.equals(platform.getDeviceGBId())) {
                logger.warn("未查询到目录{}的信息", catalogId);
                return null;
            }
            for (ChannelReduce channelReduce : channelReduces) {
                DeviceChannel deviceChannel = deviceChannelMapper.queryChannel(channelReduce.getDeviceId(), channelReduce.getChannelId());
                deviceChannel.setParental(0);
                deviceChannel.setCivilCode(catalog.getCivilCode());
                deviceChannel.setParentId(catalog.getParentId());
                deviceChannel.setBusinessGroupId(catalog.getBusinessGroupId());
                deviceChannelList.add(deviceChannel);
            }
        }
        return deviceChannelList;
    }

    @Override
    public int delAllChannelForGB(String platformId, String catalogId) {

        int result;
        if (platformId == null) {
            return 0;
        }
        ParentPlatform platform = platformMapper.getParentPlatByServerGBId(platformId);
        if (platform == null) {
            return 0;
        }
        if (ObjectUtils.isEmpty(catalogId)) {
           catalogId = platform.getDeviceGBId();
        }

        if ((result = platformChannelMapper.delChannelForGBByCatalogId(platformId, catalogId)) > 0) {
            List<DeviceChannel> deviceChannels = platformChannelMapper.queryAllChannelInCatalog(platformId, catalogId);
            eventPublisher.catalogEventPublish(platformId, deviceChannels, CatalogEvent.DEL);
        }
        return result;
    }
}
