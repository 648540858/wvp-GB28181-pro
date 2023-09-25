package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.utils.Coordtransform;
import com.genersoft.iot.vmp.service.ICommonGbChannelService;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author lin
 */
@Service
public class DeviceChannelServiceImpl implements IDeviceChannelService {

    private final static Logger logger = LoggerFactory.getLogger(DeviceChannelServiceImpl.class);

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private DeviceChannelMapper channelMapper;

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    EventPublisher eventPublisher;

    @Autowired
    ICommonGbChannelService commonGbChannelService;

    @Override
    public DeviceChannel updateGps(DeviceChannel deviceChannel, Device device) {
        if (deviceChannel.getLongitude()*deviceChannel.getLatitude() > 0) {
            if (device == null) {
                device = deviceMapper.getDeviceByDeviceId(deviceChannel.getDeviceId());
            }

            if ("WGS84".equals(device.getGeoCoordSys())) {
                deviceChannel.setLongitudeWgs84(deviceChannel.getLongitude());
                deviceChannel.setLatitudeWgs84(deviceChannel.getLatitude());
                Double[] position = Coordtransform.WGS84ToGCJ02(deviceChannel.getLongitude(), deviceChannel.getLatitude());
                deviceChannel.setLongitudeGcj02(position[0]);
                deviceChannel.setLatitudeGcj02(position[1]);
            }else if ("GCJ02".equals(device.getGeoCoordSys())) {
                deviceChannel.setLongitudeGcj02(deviceChannel.getLongitude());
                deviceChannel.setLatitudeGcj02(deviceChannel.getLatitude());
                Double[] position = Coordtransform.GCJ02ToWGS84(deviceChannel.getLongitude(), deviceChannel.getLatitude());
                deviceChannel.setLongitudeWgs84(position[0]);
                deviceChannel.setLatitudeWgs84(position[1]);
            }else {
                deviceChannel.setLongitudeGcj02(0.00);
                deviceChannel.setLatitudeGcj02(0.00);
                deviceChannel.setLongitudeWgs84(0.00);
                deviceChannel.setLatitudeWgs84(0.00);
            }
        }else {
            deviceChannel.setLongitudeGcj02(deviceChannel.getLongitude());
            deviceChannel.setLatitudeGcj02(deviceChannel.getLatitude());
            deviceChannel.setLongitudeWgs84(deviceChannel.getLongitude());
            deviceChannel.setLatitudeWgs84(deviceChannel.getLatitude());
        }
        return deviceChannel;
    }

    @Override
    public void updateChannel(String deviceId, DeviceChannel channel) {
        String channelId = channel.getChannelId();
        channel.setDeviceId(deviceId);
//        StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(deviceId, channelId);
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
        if (inviteInfo != null && inviteInfo.getStreamInfo() != null) {
            channel.setStreamId(inviteInfo.getStreamInfo().getStream());
        }
        String now = DateUtil.getNow();
        channel.setUpdateTime(now);
        DeviceChannel deviceChannel = channelMapper.queryChannel(deviceId, channelId);
        channel = updateGps(channel, null);
        if (deviceChannel == null) {
            channel.setCreateTime(now);
            channelMapper.add(channel);
        }else {
            channelMapper.update(channel);
        }
        channelMapper.updateChannelSubCount(deviceId,channel.getParentId());
    }

    @Override
    public int updateChannels(String deviceId, List<DeviceChannel> channels) {
        List<DeviceChannel> addChannels = new ArrayList<>();
        List<DeviceChannel> updateChannels = new ArrayList<>();
        HashMap<String, DeviceChannel> channelsInStore = new HashMap<>();
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (channels != null && channels.size() > 0) {
            List<DeviceChannel> channelList = channelMapper.queryChannels(deviceId, null, null, null, null,null);
            if (channelList.size() == 0) {
                for (DeviceChannel channel : channels) {
                    channel.setDeviceId(deviceId);
                    InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channel.getChannelId());
                    if (inviteInfo != null && inviteInfo.getStreamInfo() != null) {
                        channel.setStreamId(inviteInfo.getStreamInfo().getStream());
                    }
                    String now = DateUtil.getNow();
                    channel.setUpdateTime(now);
                    channel.setCreateTime(now);
                    channel = updateGps(channel, device);
                    addChannels.add(channel);
                }
            }else {
                for (DeviceChannel deviceChannel : channelList) {
                    channelsInStore.put(deviceChannel.getChannelId(), deviceChannel);
                }
                for (DeviceChannel channel : channels) {
                    channel.setDeviceId(deviceId);
                    InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channel.getChannelId());
                    if (inviteInfo != null && inviteInfo.getStreamInfo() != null) {
                        channel.setStreamId(inviteInfo.getStreamInfo().getStream());
                    }
                    String now = DateUtil.getNow();
                    channel.setUpdateTime(now);
                    channel = updateGps(channel, device);
                    if (channelsInStore.get(channel.getChannelId()) != null) {
                        updateChannels.add(channel);
                    }else {
                        addChannels.add(channel);
                        channel.setCreateTime(now);
                    }
                }
            }
            int limitCount = 50;
            if (addChannels.size() > 0) {
                if (addChannels.size() > limitCount) {
                    for (int i = 0; i < addChannels.size(); i += limitCount) {
                        int toIndex = i + limitCount;
                        if (i + limitCount > addChannels.size()) {
                            toIndex = addChannels.size();
                        }
                        channelMapper.batchAdd(addChannels.subList(i, toIndex));
                    }
                }else {
                    channelMapper.batchAdd(addChannels);
                }
            }
            if (updateChannels.size() > 0) {
                if (updateChannels.size() > limitCount) {
                    for (int i = 0; i < updateChannels.size(); i += limitCount) {
                        int toIndex = i + limitCount;
                        if (i + limitCount > updateChannels.size()) {
                            toIndex = updateChannels.size();
                        }
                        channelMapper.batchUpdate(updateChannels.subList(i, toIndex));
                    }
                }else {
                    channelMapper.batchUpdate(updateChannels);
                }
            }
        }
        return addChannels.size() + updateChannels.size();
    }

    @Override
    public ResourceBaseInfo getOverview() {

        int online = channelMapper.getOnlineCount();
        int total = channelMapper.getAllChannelCount();

        return new ResourceBaseInfo(total, online);
    }


    @Override
    public List<ChannelReduce> queryAllChannelList(String platformId) {
        return channelMapper.queryChannelListInAll(null, null, null, platformId, null);
    }

    @Override
    public boolean updateAllGps(Device device) {
        List<DeviceChannel> deviceChannels = channelMapper.getChannelsWithoutTransform(device.getDeviceId());
        List<DeviceChannel> result = new CopyOnWriteArrayList<>();
        if (deviceChannels.size() == 0) {
            return true;
        }
        String now = DateUtil.getNow();
        deviceChannels.parallelStream().forEach(deviceChannel -> {
            deviceChannel.setUpdateTime(now);
            result.add(updateGps(deviceChannel, device));
        });
        int limitCount = 50;
        if (result.size() > limitCount) {
            for (int i = 0; i < result.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > result.size()) {
                    toIndex = result.size();
                }
                channelMapper.batchUpdate(result.subList(i, toIndex));
            }
        }else {
            channelMapper.batchUpdate(result);
        }

        return true;
    }

    @Override
    public List<Device> getDeviceByChannelId(String channelId) {

        return channelMapper.getDeviceByChannelId(channelId);
    }

    @Override
    public int deleteChannels(List<DeviceChannel> deleteChannelList) {
       return channelMapper.batchDel(deleteChannelList);
    }

    @Override
    public int channelsOnline(List<DeviceChannel> channels) {
        return channelMapper.batchOnline(channels);
    }

    @Override
    public int channelsOffline(List<DeviceChannel> channels) {
        return channelMapper.batchOffline(channels);
    }

    @Override
    public DeviceChannel getOne(String deviceId, String channelId){
        return channelMapper.queryChannel(deviceId, channelId);
    }

    @Override
    public void batchUpdateChannel(List<DeviceChannel> channels) {
        channelMapper.batchUpdate(channels);
        for (DeviceChannel channel : channels) {
            if (channel.getParentId() != null) {
                channelMapper.updateChannelSubCount(channel.getDeviceId(), channel.getParentId());
            }
        }
    }

    @Override
    public void batchAddChannel(List<DeviceChannel> channels) {
        channelMapper.batchAdd(channels);
        for (DeviceChannel channel : channels) {
            if (channel.getParentId() != null) {
                channelMapper.updateChannelSubCount(channel.getDeviceId(), channel.getParentId());
            }
        }
    }

    @Override
    @Transactional
    public boolean resetChannels(Device device, List<DeviceChannel> deviceChannelList) {
        if (CollectionUtils.isEmpty(deviceChannelList)) {
            return false;
        }
        List<DeviceChannel> allChannels = channelMapper.queryAllChannels(device.getDeviceId());
        Map<String,DeviceChannel> allChannelMap = new ConcurrentHashMap<>();
        if (allChannels.size() > 0) {
            for (DeviceChannel deviceChannel : allChannels) {
                allChannelMap.put(deviceChannel.getChannelId(), deviceChannel);
            }
        }
        // 数据去重
        List<DeviceChannel> channels = new ArrayList<>();

        List<DeviceChannel> updateChannels = new ArrayList<>();
        List<DeviceChannel> addChannels = new ArrayList<>();

        StringBuilder stringBuilder = new StringBuilder();
        Map<String, Integer> subContMap = new HashMap<>();

        // 数据去重
        Set<String> gbIdSet = new HashSet<>();
        for (DeviceChannel deviceChannel : deviceChannelList) {
            if (gbIdSet.contains(deviceChannel.getChannelId())) {
                stringBuilder.append(deviceChannel.getChannelId()).append(",");
                continue;
            }
            gbIdSet.add(deviceChannel.getChannelId());
            if (allChannelMap.containsKey(deviceChannel.getChannelId())) {
                deviceChannel.setStreamId(allChannelMap.get(deviceChannel.getChannelId()).getStreamId());
                deviceChannel.setHasAudio(allChannelMap.get(deviceChannel.getChannelId()).isHasAudio());
                deviceChannel.setCommonGbChannelId(allChannelMap.get(deviceChannel.getChannelId()).getCommonGbChannelId());
                if (allChannelMap.get(deviceChannel.getChannelId()).isStatus() !=deviceChannel.isStatus()){
                    List<String> strings = platformChannelMapper.queryParentPlatformByChannelId(deviceChannel.getChannelId());
                    if (!CollectionUtils.isEmpty(strings)){
                        strings.forEach(platformId->{
                            eventPublisher.catalogEventPublish(platformId, deviceChannel, deviceChannel.isStatus()? CatalogEvent.ON:CatalogEvent.OFF);
                        });
                    }

                }
                deviceChannel.setUpdateTime(DateUtil.getNow());
                updateChannels.add(deviceChannel);
            }else {
                deviceChannel.setCreateTime(DateUtil.getNow());
                deviceChannel.setUpdateTime(DateUtil.getNow());
                addChannels.add(deviceChannel);
            }
            channels.add(deviceChannel);
            if (!ObjectUtils.isEmpty(deviceChannel.getParentId())) {
                if (subContMap.get(deviceChannel.getParentId()) == null) {
                    subContMap.put(deviceChannel.getParentId(), 1);
                }else {
                    Integer count = subContMap.get(deviceChannel.getParentId());
                    subContMap.put(deviceChannel.getParentId(), count++);
                }
            }
        }
        if (channels.size() > 0) {
            for (DeviceChannel channel : channels) {
                if (subContMap.get(channel.getChannelId()) != null){
                    Integer count = subContMap.get(channel.getChannelId());
                    if (count > 0) {
                        channel.setSubCount(count);
                        channel.setParental(1);
                    }
                }
            }
        }

        if (stringBuilder.length() > 0) {
            logger.info("[目录查询]收到的数据存在重复： {}" , stringBuilder);
        }
        if(CollectionUtils.isEmpty(channels)){
            logger.info("通道重设，数据为空={}" , deviceChannelList);
            return false;
        }
        try {
            int limitCount = 50;
            int cleanChannelsResult = 0;
            if (channels.size() > limitCount) {
                for (int i = 0; i < channels.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > channels.size()) {
                        toIndex = channels.size();
                    }
                    cleanChannelsResult += channelMapper.cleanChannelsNotInList(device.getDeviceId(), channels.subList(i, toIndex));
                }
            } else {
                cleanChannelsResult = channelMapper.cleanChannelsNotInList(device.getDeviceId(), channels);
            }
            boolean result = cleanChannelsResult < 0;
            if (!result && addChannels.size() > 0) {
                if (addChannels.size() > limitCount) {
                    for (int i = 0; i < addChannels.size(); i += limitCount) {
                        int toIndex = i + limitCount;
                        if (i + limitCount > addChannels.size()) {
                            toIndex = addChannels.size();
                        }
                        result = result || channelMapper.batchAdd(addChannels.subList(i, toIndex)) < 0;
                    }
                }else {
                    result = result || channelMapper.batchAdd(addChannels) < 0;
                }
            }
            if (!result && updateChannels.size() > 0) {
                if (updateChannels.size() > limitCount) {
                    for (int i = 0; i < updateChannels.size(); i += limitCount) {
                        int toIndex = i + limitCount;
                        if (i + limitCount > updateChannels.size()) {
                            toIndex = updateChannels.size();
                        }
                        result = result || channelMapper.batchUpdate(updateChannels.subList(i, toIndex)) < 0;
                    }
                }else {
                    result = result || channelMapper.batchUpdate(updateChannels) < 0;
                }
            }

            if (result) {
                //事务回滚
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            if (device.isAutoSyncChannel()) {
                commonGbChannelService.syncChannelFromGb28181Device(device.getDeviceId(), null, true, true);
            }
            return true;
        }catch (Exception e) {
            logger.error("未处理的异常 ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }
}
