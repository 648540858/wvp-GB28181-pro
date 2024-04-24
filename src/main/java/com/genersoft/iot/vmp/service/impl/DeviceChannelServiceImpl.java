package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.utils.Coordtransform;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMobilePositionMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author lin
 */
@Service
@DS("master")
public class DeviceChannelServiceImpl implements IDeviceChannelService {

    private final static Logger logger = LoggerFactory.getLogger(DeviceChannelServiceImpl.class);

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private DeviceChannelMapper channelMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceMobilePositionMapper deviceMobilePositionMapper;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

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
    public void online(DeviceChannel channel) {
        channelMapper.online(channel.getDeviceId(), channel.getChannelId());
    }

    @Override
    public int channelsOffline(List<DeviceChannel> channels) {
        return channelMapper.batchOffline(channels);
    }


    @Override
    public void offline(DeviceChannel channel) {
        channelMapper.offline(channel.getDeviceId(), channel.getChannelId());
    }

    @Override
    public void delete(DeviceChannel channel) {
        channelMapper.del(channel.getDeviceId(), channel.getChannelId());
    }

    @Override
    public DeviceChannel getOne(String deviceId, String channelId){
        return channelMapper.queryChannel(deviceId, channelId);
    }

    @Override
    public synchronized void batchUpdateChannel(List<DeviceChannel> channels) {
        String now = DateUtil.getNow();
        for (DeviceChannel channel : channels) {
            channel.setUpdateTime(now);
        }
        int limitCount = 1000;
        if (!channels.isEmpty()) {
            if (channels.size() > limitCount) {
                for (int i = 0; i < channels.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > channels.size()) {
                        toIndex = channels.size();
                    }
                    channelMapper.batchUpdate(channels.subList(i, toIndex));
                }
            }else {
                channelMapper.batchUpdate(channels);
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
    public void updateChannelStreamIdentification(DeviceChannel channel) {
        assert !ObjectUtils.isEmpty(channel.getDeviceId());
        assert !ObjectUtils.isEmpty(channel.getStreamIdentification());
        if (ObjectUtils.isEmpty(channel.getStreamIdentification())) {
            logger.info("[重置通道码流类型] 设备: {}, 码流： {}", channel.getDeviceId(), channel.getStreamIdentification());
        }else {
            logger.info("[更新通道码流类型] 设备: {}, 通道：{}， 码流： {}", channel.getDeviceId(), channel.getChannelId(),
                    channel.getStreamIdentification());
        }
        channelMapper.updateChannelStreamIdentification(channel);
    }

    @Override
    public List<DeviceChannel> queryChaneListByDeviceId(String deviceId) {
        return channelMapper.queryAllChannels(deviceId);
    }

    @Override
    public void updateChannelGPS(Device device, DeviceChannel deviceChannel, MobilePosition mobilePosition) {
        if (userSetting.getSavePositionHistory()) {
            deviceMobilePositionMapper.insertNewPosition(mobilePosition);
        }

        if (deviceChannel.getChannelId().equals(deviceChannel.getDeviceId())) {
            deviceChannel.setChannelId(null);
        }
        if (deviceChannel.getGpsTime() == null) {
            deviceChannel.setGpsTime(DateUtil.getNow());
        }

        int updated = channelMapper.updatePosition(deviceChannel);
        if (updated == 0) {
            return;
        }

        List<DeviceChannel> deviceChannels = new ArrayList<>();
        if (deviceChannel.getChannelId() == null) {
            // 有的设备这里上报的deviceId与通道Id是一样，这种情况更新设备下的全部通道
            List<DeviceChannel> deviceChannelsInDb = queryChaneListByDeviceId(device.getDeviceId());
            deviceChannels.addAll(deviceChannelsInDb);
        }else {
            deviceChannels.add(deviceChannel);
        }
        if (deviceChannels.isEmpty()) {
            return;
        }
        if (deviceChannels.size() > 100) {
            logger.warn("[更新通道位置信息后发送通知] 设备可能是平台，上报的位置信息未标明通道编号，" +
                    "导致所有通道被更新位置， deviceId:{}", device.getDeviceId());
        }
        for (DeviceChannel channel : deviceChannels) {
            // 向关联了该通道并且开启移动位置订阅的上级平台发送移动位置订阅消息
            mobilePosition.setChannelId(channel.getChannelId());
            try {
                eventPublisher.mobilePositionEventPublish(mobilePosition);
            }catch (Exception e) {
                logger.error("[向上级转发移动位置失败] ", e);
            }
            // 发送redis消息。 通知位置信息的变化
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("time", DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(mobilePosition.getTime()));
            jsonObject.put("serial", mobilePosition.getDeviceId());
            jsonObject.put("code", mobilePosition.getChannelId());
            jsonObject.put("longitude", mobilePosition.getLongitude());
            jsonObject.put("latitude", mobilePosition.getLatitude());
            jsonObject.put("altitude", mobilePosition.getAltitude());
            jsonObject.put("direction", mobilePosition.getDirection());
            jsonObject.put("speed", mobilePosition.getSpeed());
            redisCatchStorage.sendMobilePositionMsg(jsonObject);
        }
    }

    @Override
    public void stopPlay(String deviceId, String channelId) {
        channelMapper.stopPlay(deviceId, channelId);
    }

    @Override
    @Transactional
    public void batchUpdateChannelGPS(List<DeviceChannel> channelList) {
        for (DeviceChannel deviceChannel : channelList) {
            deviceChannel.setUpdateTime(DateUtil.getNow());
            if (deviceChannel.getGpsTime() == null) {
                deviceChannel.setGpsTime(DateUtil.getNow());
            }
        }
        int count = 1000;
        if (channelList.size() > count) {
            for (int i = 0; i < channelList.size(); i+=count) {
                int toIndex = i+count;
                if ( i + count > channelList.size()) {
                    toIndex = channelList.size();
                }
                List<DeviceChannel> channels = channelList.subList(i, toIndex);
                channelMapper.batchUpdatePosition(channels);
            }
        }else {
            channelMapper.batchUpdatePosition(channelList);
        }
    }

    @Override
    @Transactional
    public void batchAddMobilePosition(List<MobilePosition> mobilePositions) {
//        int count = 500;
//        if (mobilePositions.size() > count) {
//            for (int i = 0; i < mobilePositions.size(); i+=count) {
//                int toIndex = i+count;
//                if ( i + count > mobilePositions.size()) {
//                    toIndex = mobilePositions.size();
//                }
//                List<MobilePosition> mobilePositionsSub = mobilePositions.subList(i, toIndex);
//                deviceMobilePositionMapper.batchadd(mobilePositionsSub);
//            }
//        }else {
//            deviceMobilePositionMapper.batchadd(mobilePositions);
//        }
        deviceMobilePositionMapper.batchadd(mobilePositions);
    }
}
