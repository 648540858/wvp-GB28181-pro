package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.utils.Coordtransform;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author lin
 */
@Service
public class DeviceChannelServiceImpl implements IDeviceChannelService {

    private final static Logger logger = LoggerFactory.getLogger(DeviceChannelServiceImpl.class);

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private DeviceChannelMapper channelMapper;

    @Autowired
    private DeviceMapper deviceMapper;

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
        StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(deviceId, channelId);
        if (streamInfo != null) {
            channel.setStreamId(streamInfo.getStream());
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
            List<DeviceChannel> channelList = channelMapper.queryChannels(deviceId, null, null, null, null);
            if (channelList.size() == 0) {
                for (DeviceChannel channel : channels) {
                    channel.setDeviceId(deviceId);
                    StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(deviceId, channel.getChannelId());
                    if (streamInfo != null) {
                        channel.setStreamId(streamInfo.getStream());
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
                    StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(deviceId, channel.getChannelId());
                    if (streamInfo != null) {
                        channel.setStreamId(streamInfo.getStream());
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
            int limitCount = 300;
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
}
