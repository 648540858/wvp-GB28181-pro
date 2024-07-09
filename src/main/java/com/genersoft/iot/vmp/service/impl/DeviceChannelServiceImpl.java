package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMobilePositionMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author lin
 */
@Slf4j
@Service
@DS("master")
public class DeviceChannelServiceImpl implements IDeviceChannelService {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private DeviceChannelMapper channelMapper;

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceMobilePositionMapper deviceMobilePositionMapper;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Override
    public void updateChannel(String deviceId, DeviceChannel channel) {
        String channelId = channel.getDeviceId();
        channel.setDeviceId(deviceId);
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
        if (inviteInfo != null && inviteInfo.getStreamInfo() != null) {
            channel.setStreamId(inviteInfo.getStreamInfo().getStream());
        }
        String now = DateUtil.getNow();
        channel.setUpdateTime(now);
        DeviceChannel deviceChannel = channelMapper.queryChannel(deviceId, channelId);
        if (deviceChannel == null) {
            channel.setCreateTime(now);
            channelMapper.add(channel);
        }else {
            channelMapper.update(channel);
        }
        channelMapper.updateChannelSubCount(deviceId,channel.getParentId());
    }

    @Override
    public int updateChannels(Device device, List<DeviceChannel> channels) {
        List<DeviceChannel> addChannels = new ArrayList<>();
        List<DeviceChannel> updateChannels = new ArrayList<>();
        HashMap<String, DeviceChannel> channelsInStore = new HashMap<>();
        if (channels != null && channels.size() > 0) {
            List<DeviceChannel> channelList = channelMapper.queryChannelsByDeviceDbId(device.getId());
            if (channelList.isEmpty()) {
                for (DeviceChannel channel : channels) {
                    channel.setDeviceDbId(device.getId());
                    InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channel.getDeviceId());
                    if (inviteInfo != null && inviteInfo.getStreamInfo() != null) {
                        channel.setStreamId(inviteInfo.getStreamInfo().getStream());
                    }
                    String now = DateUtil.getNow();
                    channel.setUpdateTime(now);
                    channel.setCreateTime(now);
                    addChannels.add(channel);
                }
            }else {
                for (DeviceChannel deviceChannel : channelList) {
                    channelsInStore.put(deviceChannel.getDeviceId(), deviceChannel);
                }
                for (DeviceChannel channel : channels) {
                    channel.setDeviceDbId(device.getId());
                    InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channel.getDeviceId());
                    if (inviteInfo != null && inviteInfo.getStreamInfo() != null) {
                        channel.setStreamId(inviteInfo.getStreamInfo().getStream());
                    }
                    String now = DateUtil.getNow();
                    channel.setUpdateTime(now);
                    if (channelsInStore.get(channel.getDeviceId()) != null) {
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
            result.add(deviceChannel);
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
        channelMapper.online(channel.getDeviceId(), channel.getDeviceId());
    }

    @Override
    public int channelsOffline(List<DeviceChannel> channels) {
        return channelMapper.batchOffline(channels);
    }


    @Override
    public void offline(DeviceChannel channel) {
        channelMapper.offline(channel.getDeviceId(), channel.getDeviceId());
    }

    @Override
    public void delete(DeviceChannel channel) {
        channelMapper.del(channel.getId());
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
            log.info("[重置通道码流类型] 设备: {}, 码流： {}", channel.getDeviceId(), channel.getStreamIdentification());
        }else {
            log.info("[更新通道码流类型] 设备: {}, 通道：{}， 码流： {}", channel.getDeviceId(), channel.getDeviceId(),
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

        if (deviceChannel.getDeviceId().equals(deviceChannel.getDeviceId())) {
            deviceChannel.setDeviceId(null);
        }
        if (deviceChannel.getGpsTime() == null) {
            deviceChannel.setGpsTime(DateUtil.getNow());
        }

        int updated = channelMapper.updatePosition(deviceChannel);
        if (updated == 0) {
            return;
        }

        List<DeviceChannel> deviceChannels = new ArrayList<>();
        if (deviceChannel.getDeviceId() == null) {
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
            log.warn("[更新通道位置信息后发送通知] 设备可能是平台，上报的位置信息未标明通道编号，" +
                    "导致所有通道被更新位置， deviceId:{}", device.getDeviceId());
        }
        for (DeviceChannel channel : deviceChannels) {
            // 向关联了该通道并且开启移动位置订阅的上级平台发送移动位置订阅消息
            mobilePosition.setChannelId(channel.getDeviceId());
            try {
                eventPublisher.mobilePositionEventPublish(mobilePosition);
            }catch (Exception e) {
                log.error("[向上级转发移动位置失败] ", e);
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
    public void startPlay(String deviceId, String channelId, String stream) {
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到设备： " +deviceId);
        }
        channelMapper.startPlay(device.getId(), channelId, stream);
    }

    @Override
    public void stopPlay(String deviceId, String channelId) {
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到设备： " +deviceId);
        }
        channelMapper.stopPlay(device.getId(), channelId);
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

    @Override
    public void cleanChannelsForDevice(int deviceId) {
        channelMapper.cleanChannelsByDeviceId(deviceId);
    }

    @Override
    @Transactional
    public boolean resetChannels(String deviceId, List<DeviceChannel> deviceChannelList) {
        if (CollectionUtils.isEmpty(deviceChannelList)) {
            return false;
        }
        List<DeviceChannel> allChannels = channelMapper.queryAllChannels(deviceId);
        Map<String,DeviceChannel> allChannelMap = new ConcurrentHashMap<>();
        if (allChannels.size() > 0) {
            for (DeviceChannel deviceChannel : allChannels) {
                allChannelMap.put(deviceChannel.getDeviceId(), deviceChannel);
            }
        }
        // 数据去重
        List<DeviceChannel> channels = new ArrayList<>();

        List<DeviceChannel> updateChannels = new ArrayList<>();
        List<DeviceChannel> addChannels = new ArrayList<>();
        List<DeviceChannel> deleteChannels = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        Map<String, Integer> subContMap = new HashMap<>();

        // 数据去重
        Set<String> gbIdSet = new HashSet<>();
        for (DeviceChannel deviceChannel : deviceChannelList) {
            if (gbIdSet.contains(deviceChannel.getDeviceId())) {
                stringBuilder.append(deviceChannel.getDeviceId()).append(",");
                continue;
            }
            gbIdSet.add(deviceChannel.getDeviceId());
            if (allChannelMap.containsKey(deviceChannel.getDeviceId())) {
                deviceChannel.setStreamId(allChannelMap.get(deviceChannel.getDeviceId()).getStreamId());
                deviceChannel.setHasAudio(allChannelMap.get(deviceChannel.getDeviceId()).getHasAudio());
                if (allChannelMap.get(deviceChannel.getDeviceId()).getStatus().equalsIgnoreCase(deviceChannel.getStatus())){
                    List<String> strings = platformChannelMapper.queryParentPlatformByChannelId(deviceChannel.getDeviceId());
                    if (!CollectionUtils.isEmpty(strings)){
                        strings.forEach(platformId->{
                            eventPublisher.catalogEventPublish(platformId, deviceChannel, deviceChannel.getStatus().equals("ON")? CatalogEvent.ON:CatalogEvent.OFF);
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
            allChannelMap.remove(deviceChannel.getDeviceId());
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
        deleteChannels.addAll(allChannelMap.values());
        if (!channels.isEmpty()) {
            for (DeviceChannel channel : channels) {
                if (subContMap.get(channel.getDeviceId()) != null){
                    Integer count = subContMap.get(channel.getDeviceId());
                    if (count > 0) {
                        channel.setSubCount(count);
                        channel.setParental(1);
                    }
                }
            }
        }

        if (stringBuilder.length() > 0) {
            log.info("[目录查询]收到的数据存在重复： {}" , stringBuilder);
        }
        if(CollectionUtils.isEmpty(channels)){
            log.info("通道重设，数据为空={}" , deviceChannelList);
            return false;
        }
        int limitCount = 50;
        boolean result = false;
        if (!result && !addChannels.isEmpty()) {
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
        if (!result && !updateChannels.isEmpty()) {
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
        if (!result && !deleteChannels.isEmpty()) {
            System.out.println("删除： " + deleteChannels.size());
            if (deleteChannels.size() > limitCount) {
                for (int i = 0; i < deleteChannels.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > deleteChannels.size()) {
                        toIndex = deleteChannels.size();
                    }
                    result = result || channelMapper.batchDel(deleteChannels.subList(i, toIndex)) < 0;
                }
            }else {
                result = result || channelMapper.batchDel(deleteChannels) < 0;
            }
        }

        return true;

    }
}
