package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.PlatformCatalog;
import com.genersoft.iot.vmp.gb28181.bean.TreeType;
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
import org.springframework.stereotype.Service;

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
        int result = 0;
        if (channelReducesToAdd.size() > 0) {
            result = platformChannelMapper.addChannels(platformId, channelReducesToAdd);
            // TODO 后续给平台增加控制开关以控制是否响应目录订阅
            List<DeviceChannel> deviceChannelList = getDeviceChannelListByChannelReduceList(channelReducesToAdd, catalogId, platform);
            eventPublisher.catalogEventPublish(platformId, deviceChannelList, CatalogEvent.ADD);
        }

        return result;
    }

    private List<DeviceChannel> getDeviceChannelListByChannelReduceList(List<ChannelReduce> channelReduces, String catalogId, ParentPlatform platform) {
        List<DeviceChannel> deviceChannelList = new ArrayList<>();
        if (channelReduces.size() > 0){
            PlatformCatalog catalog = catalogManager.select(catalogId);
            if (catalog == null && !catalogId.equals(platform.getServerGBId())) {
                logger.warn("未查询到目录{}的信息", catalogId);
                return null;
            }
            for (ChannelReduce channelReduce : channelReduces) {
                DeviceChannel deviceChannel = deviceChannelMapper.queryChannel(channelReduce.getDeviceId(), channelReduce.getChannelId());
                deviceChannel.setParental(0);
                deviceChannelList.add(deviceChannel);
                if (platform.getTreeType().equals(TreeType.CIVIL_CODE)){
                    deviceChannel.setCivilCode(catalogId);
                }else if (platform.getTreeType().equals(TreeType.BUSINESS_GROUP)){
                    deviceChannel.setParentId(catalogId);
                    if (catalog != null) {
                        deviceChannel.setBusinessGroupId(catalog.getBusinessGroupId());
                    }
                }
            }
        }
        return deviceChannelList;
    }
}
