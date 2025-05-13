package com.genersoft.iot.vmp.gb28181.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.common.enums.DeviceControlType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelReduce;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMobilePositionMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.record.RecordInfoEndEvent;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.web.gb28181.dto.DeviceChannelExtend;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.message.Response;
import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * @author lin
 */
@Slf4j
@Service
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

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private IRedisRpcPlayService redisRpcPlayService;

    @Autowired
    private ISIPCommander commander;

    // 记录录像查询的结果等待
    private final Map<String, SynchronousQueue<RecordInfo>> topicSubscribers = new ConcurrentHashMap<>();

    /**
     * 监听录像查询结束事件
     */
    @Async("taskExecutor")
    @org.springframework.context.event.EventListener
    public void onApplicationEvent(RecordInfoEndEvent event) {
        SynchronousQueue<RecordInfo> queue = topicSubscribers.get("record" + event.getRecordInfo().getSn());
        if (queue != null) {
            queue.offer(event.getRecordInfo());
        }
    }

    @Autowired
    private ISIPCommander cmder;


    @Override
    public int updateChannels(Device device, List<DeviceChannel> channels) {
        if (CollectionUtils.isEmpty(channels)) {
            return 0;
        }
        List<DeviceChannel> addChannels = new ArrayList<>();
        List<DeviceChannel> updateChannels = new ArrayList<>();
        HashMap<String, DeviceChannel> channelsInStore = new HashMap<>();
        int result = 0;
        List<DeviceChannel> channelList = channelMapper.queryChannelsByDeviceDbId(device.getId());
        if (channelList.isEmpty()) {
            for (DeviceChannel channel : channels) {
                channel.setDataDeviceId(device.getId());
                InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
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
                channelsInStore.put(deviceChannel.getDataDeviceId() + deviceChannel.getDeviceId(), deviceChannel);
            }
            for (DeviceChannel channel : channels) {
                InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                if (inviteInfo != null && inviteInfo.getStreamInfo() != null) {
                    channel.setStreamId(inviteInfo.getStreamInfo().getStream());
                }
                String now = DateUtil.getNow();
                channel.setUpdateTime(now);
                DeviceChannel deviceChannelInDb = channelsInStore.get(channel.getDataDeviceId() + channel.getDeviceId());
                if ( deviceChannelInDb != null) {
                    channel.setId(deviceChannelInDb.getId());
                    channel.setUpdateTime(now);
                    updateChannels.add(channel);
                }else {
                    channel.setCreateTime(now);
                    channel.setUpdateTime(now);
                    addChannels.add(channel);
                }
            }
        }
        Set<String> channelSet = new HashSet<>();
        // 滤重
        List<DeviceChannel> addChannelList = new ArrayList<>();
        List<DeviceChannel> updateChannelList = new ArrayList<>();
        addChannels.forEach(channel -> {
            if (channelSet.add(channel.getDeviceId())) {
                addChannelList.add(channel);
            }
        });
        channelSet.clear();
        updateChannels.forEach(channel -> {
            if (channelSet.add(channel.getDeviceId())) {
                updateChannelList.add(channel);
            }
        });

        int limitCount = 500;
        if (!addChannelList.isEmpty()) {
            if (addChannelList.size() > limitCount) {
                for (int i = 0; i < addChannelList.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > addChannelList.size()) {
                        toIndex = addChannelList.size();
                    }
                    result += channelMapper.batchAdd(addChannelList.subList(i, toIndex));
                }
            }else {
                result += channelMapper.batchAdd(addChannelList);
            }
        }
        if (!updateChannelList.isEmpty()) {
            if (updateChannelList.size() > limitCount) {
                for (int i = 0; i < updateChannelList.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > updateChannelList.size()) {
                        toIndex = updateChannelList.size();
                    }
                    result += channelMapper.batchUpdate(updateChannelList.subList(i, toIndex));
                }
            }else {
                result += channelMapper.batchUpdate(updateChannelList);
            }
        }
        return result;
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
    public PageInfo<ChannelReduce> queryAllChannelList(int page, int count, String query, Boolean online, Boolean channelType, String platformId, String catalogId) {
        PageHelper.startPage(page, count);
        List<ChannelReduce> all = channelMapper.queryChannelListInAll(query, online, channelType, platformId, catalogId);
        return new PageInfo<>(all);
    }

    @Override
    public List<Device> getDeviceByChannelId(String channelId) {

        return channelMapper.getDeviceByChannelDeviceId(channelId);
    }

    @Override
    @Transactional
    public int deleteChannelsForNotify(List<DeviceChannel> channels) {
        int limitCount = 1000;
        int result = 0;
        if (!channels.isEmpty()) {
            if (channels.size() > limitCount) {
                for (int i = 0; i < channels.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > channels.size()) {
                        toIndex = channels.size();
                    }
                    result += channelMapper.batchDel(channels.subList(i, toIndex));
                }
            }else {
                result += channelMapper.batchDel(channels);
            }
        }
        return result;
    }

    @Transactional
    @Override
    public int updateChannelsStatus(List<DeviceChannel> channels) {
        int limitCount = 1000;
        int result = 0;
        if (!channels.isEmpty()) {
            if (channels.size() > limitCount) {
                for (int i = 0; i < channels.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > channels.size()) {
                        toIndex = channels.size();
                    }
                    result += channelMapper.batchUpdateStatus(channels.subList(i, toIndex));
                }
            }else {
                result += channelMapper.batchUpdateStatus(channels);
            }
        }
        return result;
    }

    @Override
    public void online(DeviceChannel channel) {
        channelMapper.online(channel.getId());
    }

    @Override
    public void offline(DeviceChannel channel) {
        channelMapper.offline(channel.getId());
    }

    @Override
    public void delete(DeviceChannel channel) {
        channelMapper.del(channel.getId());
    }

    @Override
    public DeviceChannel getOne(String deviceId, String channelId){
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到设备：" + deviceId);
        }
        return channelMapper.getOneByDeviceId(device.getId(), channelId);
    }

    @Override
    public DeviceChannel getOneForSource(String deviceId, String channelId){
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到设备：" + deviceId);
        }
        return channelMapper.getOneByDeviceIdForSource(device.getId(), channelId);
    }

    @Override
    public DeviceChannel getOneForSource(int deviceDbId, String channelId) {
        return channelMapper.getOneByDeviceIdForSource(deviceDbId, channelId);
    }

    @Override
    public DeviceChannel getOneBySourceId(int deviceDbId, String channelId) {
        return channelMapper.getOneBySourceChannelId(deviceDbId, channelId);
    }

    @Override
    @Transactional
    public synchronized void batchUpdateChannelForNotify(List<DeviceChannel> channels) {
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
                    channelMapper.batchUpdateForNotify(channels.subList(i, toIndex));
                }
            }else {
                channelMapper.batchUpdateForNotify(channels);
            }
        }
    }

    @Override
    @Transactional
    public void batchAddChannel(List<DeviceChannel> channels) {
        String now = DateUtil.getNow();
        for (DeviceChannel channel : channels) {
            channel.setUpdateTime(now);
            channel.setCreateTime(now);
        }
        int limitCount = 1000;
        if (!channels.isEmpty()) {
            if (channels.size() > limitCount) {
                for (int i = 0; i < channels.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > channels.size()) {
                        toIndex = channels.size();
                    }
                    channelMapper.batchAdd(channels.subList(i, toIndex));
                }
            }else {
                channelMapper.batchAdd(channels);
            }
        }
        for (DeviceChannel channel : channels) {
            if (channel.getParentId() != null) {
                channelMapper.updateChannelSubCount(channel.getDataDeviceId(), channel.getParentId());
            }
        }
    }

    @Override
    public void updateChannelStreamIdentification(DeviceChannel channel) {
        Assert.hasLength(channel.getStreamIdentification(), "码流标识必须存在");
        if (ObjectUtils.isEmpty(channel.getStreamIdentification())) {
            log.info("[重置通道码流类型] 设备: {}, 码流： {}", channel.getDeviceId(), channel.getStreamIdentification());
        }else {
            log.info("[更新通道码流类型] 设备: {}, 通道：{}， 码流： {}", channel.getDeviceId(), channel.getDeviceId(),
                    channel.getStreamIdentification());
        }
        if (channel.getId() > 0) {
            channelMapper.updateChannelStreamIdentification(channel);
        }else {
            channelMapper.updateAllChannelStreamIdentification(channel.getStreamIdentification());
        }
    }

    @Override
    public List<DeviceChannel> queryChaneListByDeviceId(String deviceId) {
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到通道：" + deviceId);
        }
        return channelMapper.queryChannelsByDeviceDbId(device.getId());
    }

    @Override
    public List<DeviceChannel> queryChaneListByDeviceDbId(Integer deviceDbId) {
        return channelMapper.queryChannelsByDeviceDbId(deviceDbId);
    }

    @Override
    public List<Integer> queryChaneIdListByDeviceDbIds(List<Integer> deviceDbIds) {
        return channelMapper.queryChaneIdListByDeviceDbIds(deviceDbIds);
    }

    @Override
    public void handlePtzCmd(@NotNull Integer dataDeviceId, @NotNull Integer gbId, Element rootElement, DeviceControlType type, ErrorCallback<String> callback) {

        // 根据通道ID，获取所属设备
        Device device = deviceMapper.query(dataDeviceId);
        if (device == null) {
            // 不存在则回复404
            log.warn("[INFO 消息] 通道所属设备不存在， 设备ID： {}", dataDeviceId);
            callback.run(Response.NOT_FOUND, "device  not found", null);
            return;
        }

        DeviceChannel deviceChannel = channelMapper.getOneForSource(gbId);
        if (deviceChannel == null) {
            log.warn("[deviceControl] 未找到设备原始通道， 设备： {}（{}），通道编号：{}", device.getName(),
                    device.getDeviceId(), gbId);
            callback.run(Response.NOT_FOUND, "channel  not found", null);
            return;
        }
        log.info("[deviceControl] 命令: {}, 设备： {}（{}）， 通道{}（{}", type,  device.getName(), device.getDeviceId(),
                deviceChannel.getName(), deviceChannel.getDeviceId());
        String cmdString = getText(rootElement, type.getVal());
        try {
            cmder.fronEndCmd(device, deviceChannel.getDeviceId(), cmdString, errorResult->{
                        callback.run(errorResult.statusCode, errorResult.msg, null);
                    }, errorResult->{
                        callback.run(errorResult.statusCode, errorResult.msg, null);
                    });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 云台/前端: {}", e.getMessage());
        }
    }

    @Override
    public void updateChannelGPS(Device device, DeviceChannel deviceChannel, MobilePosition mobilePosition) {
        if (userSetting.getSavePositionHistory()) {
            deviceMobilePositionMapper.insertNewPosition(mobilePosition);
        }

        if (deviceChannel.getDeviceId().equals(device.getDeviceId())) {
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
            mobilePosition.setChannelId(channel.getId());
            try {
                eventPublisher.mobilePositionEventPublish(mobilePosition);
            }catch (Exception e) {
                log.error("[向上级转发移动位置失败] ", e);
            }
            // 发送redis消息。 通知位置信息的变化
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("time", DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(mobilePosition.getTime()));
            jsonObject.put("serial", mobilePosition.getDeviceId());
            jsonObject.put("code", channel.getDeviceId());
            jsonObject.put("longitude", mobilePosition.getLongitude());
            jsonObject.put("latitude", mobilePosition.getLatitude());
            jsonObject.put("altitude", mobilePosition.getAltitude());
            jsonObject.put("direction", mobilePosition.getDirection());
            jsonObject.put("speed", mobilePosition.getSpeed());
            redisCatchStorage.sendMobilePositionMsg(jsonObject);
        }
    }

    @Override
    public void startPlay(Integer channelId, String stream) {
        channelMapper.startPlay(channelId, stream);
    }

    @Override
    public void stopPlay(Integer channelId) {
        channelMapper.stopPlayById(channelId);
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
    public boolean resetChannels(int deviceDbId, List<DeviceChannel> deviceChannelList) {
        if (CollectionUtils.isEmpty(deviceChannelList)) {
            return false;
        }
        List<DeviceChannel> allChannels = channelMapper.queryAllChannelsForRefresh(deviceDbId);
        Map<String,DeviceChannel> allChannelMap = new HashMap<>();
        if (!allChannels.isEmpty()) {
            for (DeviceChannel deviceChannel : allChannels) {
                allChannelMap.put(deviceChannel.getDataDeviceId() + deviceChannel.getDeviceId(), deviceChannel);
            }
        }
        // 数据去重
        List<DeviceChannel> channels = new ArrayList<>();

        List<DeviceChannel> updateChannels = new ArrayList<>();
        List<DeviceChannel> addChannels = new ArrayList<>();
        List<DeviceChannel> deleteChannels = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        Map<String, Integer> subContMap = new HashMap<>();

        for (DeviceChannel deviceChannel : deviceChannelList) {
            DeviceChannel channelInDb = allChannelMap.get(deviceChannel.getDataDeviceId() + deviceChannel.getDeviceId());
            if (channelInDb != null) {
                deviceChannel.setStreamId(channelInDb.getStreamId());
                deviceChannel.setHasAudio(channelInDb.isHasAudio());
                deviceChannel.setId(channelInDb.getId());
                if (channelInDb.getStatus() != null && channelInDb.getStatus().equalsIgnoreCase(deviceChannel.getStatus())){
                    List<Platform> platformList = platformChannelMapper.queryParentPlatformByChannelId(deviceChannel.getDeviceId());
                    if (!CollectionUtils.isEmpty(platformList)){
                        platformList.forEach(platform->{
                            eventPublisher.catalogEventPublish(platform, deviceChannel.buildCommonGBChannelForStatus(), deviceChannel.getStatus().equals("ON")? CatalogEvent.ON:CatalogEvent.OFF);
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
            allChannelMap.remove(deviceChannel.getDataDeviceId() + deviceChannel.getDeviceId());
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
        int limitCount = 500;
        if (!addChannels.isEmpty()) {
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
        if (!updateChannels.isEmpty()) {
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
            // 不对收到的通道做比较，已确定是否真的发生变化，所以不发送更新通知

        }
        if (!deleteChannels.isEmpty()) {
            try {
                // 这些通道可能关联了，上级平台需要删除同时发送消息
                List<Integer> ids = new ArrayList<>();
                deleteChannels.stream().forEach(deviceChannel -> {
                    ids.add(deviceChannel.getId());
                });
                platformChannelService.removeChannels(ids);
            }catch (Exception e) {
                log.error("[移除通道国标级联共享失败]", e);
            }
            if (deleteChannels.size() > limitCount) {
                for (int i = 0; i < deleteChannels.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > deleteChannels.size()) {
                        toIndex = deleteChannels.size();
                    }
                    channelMapper.batchDel(deleteChannels.subList(i, toIndex));
                }
            }else {
                channelMapper.batchDel(deleteChannels);
            }
        }
        return true;

    }

    @Override
    public PageInfo<DeviceChannel> getSubChannels(int deviceDbId, String channelId, String query, Boolean channelType, Boolean online, int page, int count) {
        PageHelper.startPage(page, count);
        String civilCode = null;
        String parentId = null;
        String businessGroupId = null;
        if (channelId.length() <= 8) {
            civilCode = channelId;
        }else {
            GbCode decode = GbCode.decode(channelId);
            if (Integer.parseInt(decode.getTypeCode()) == 215) {
                businessGroupId = channelId;
            }else {
                parentId = channelId;
            }
        }
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<DeviceChannel> all = channelMapper.queryChannels(deviceDbId, civilCode, businessGroupId, parentId, query, channelType, online,null);
        return new PageInfo<>(all);
    }

    @Override
    public List<DeviceChannelExtend> queryChannelExtendsByDeviceId(String deviceId, List<String> channelIds, Boolean online) {
        return channelMapper.queryChannelsWithDeviceInfo(deviceId, null,null, null, online,channelIds);
    }

    @Override
    public PageInfo queryChannelsByDeviceId(String deviceId, String query, Boolean hasSubChannel, Boolean online, int page, int count) {
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到设备：" + deviceId);
        }
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        PageHelper.startPage(page, count);
        List<DeviceChannel> all = channelMapper.queryChannels(device.getId(), null,null, null, query, hasSubChannel, online,null);
        return new PageInfo<>(all);
    }

    @Override
    public List<Device> queryDeviceWithAsMessageChannel() {
        return deviceMapper.queryDeviceWithAsMessageChannel();
    }

    @Override
    public DeviceChannel getRawChannel(int id) {
        return deviceMapper.getRawChannel(id);
    }

    @Override
    public DeviceChannel getOneById(Integer channelId) {
        return channelMapper.getOne(channelId);
    }

    @Override
    public DeviceChannel getOneForSourceById(Integer channelId) {
        return channelMapper.getOneForSource(channelId);
    }

    @Override
    public DeviceChannel getBroadcastChannel(int deviceDbId) {
        List<DeviceChannel> channels = channelMapper.queryChannelsByDeviceDbId(deviceDbId);
        if (channels.size() == 1) {
            return channels.get(0);
        }
        for (DeviceChannel channel : channels) {
            // 获取137类型的
            if (SipUtils.isFrontEnd(channel.getDeviceId())) {
                return channel;
            }
        }
        return null;
    }

    @Override
    public void changeAudio(Integer channelId, Boolean audio) {
        channelMapper.changeAudio(channelId, audio);
    }

    @Override
    public void updateChannelStatus(DeviceChannel channel) {
        channelMapper.updateStatus(channel);
    }

    @Override
    public void addChannel(DeviceChannel channel) {
        channel.setDataType(ChannelDataType.GB28181.value);
        channel.setDataDeviceId(channel.getDataDeviceId());
        channelMapper.add(channel);
    }

    @Override
    public void updateChannelForNotify(DeviceChannel channel) {
        channelMapper.updateChannelForNotify(channel);
    }

    @Override
    public void queryRecordInfo(Device device, DeviceChannel channel, String startTime, String endTime, ErrorCallback<RecordInfo> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())){
            redisRpcPlayService.queryRecordInfo(device.getServerId(), channel.getId(), startTime, endTime, callback);
            return;
        }
        try {
            int sn  =  (int)((Math.random()*9+1)*100000);
            commander.recordInfoQuery(device, channel.getDeviceId(), startTime, endTime, sn, null, null, eventResult -> {
                try {
                    // 消息发送成功, 监听等待数据到来
                    SynchronousQueue<RecordInfo> queue = new SynchronousQueue<>();
                    topicSubscribers.put("record" + sn, queue);
                    RecordInfo recordInfo = queue.poll(userSetting.getRecordInfoTimeout(), TimeUnit.MILLISECONDS);
                    if (recordInfo != null) {
                        callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), recordInfo);
                    }else {
                        callback.run(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg(), recordInfo);
                    }
                } catch (InterruptedException e) {
                    callback.run(ErrorCode.ERROR100.getCode(), e.getMessage(), null);
                } finally {
                    this.topicSubscribers.remove("record" + sn);
                }

            }, (eventResult -> {
                callback.run(ErrorCode.ERROR100.getCode(), "查询录像失败, status: " +  eventResult.statusCode + ", message: " + eventResult.msg, null);
            }));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 查询录像: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " +  e.getMessage());
        }
    }

    @Override
    public void queryRecordInfo(CommonGBChannel channel, String startTime, String endTime, ErrorCallback<RecordInfo> callback) {
        if (channel.getDataType() != ChannelDataType.GB28181.value){
            // 只支持国标的语音喊话
            log.warn("[INFO 消息] 非国标设备， 通道ID： {}", channel.getGbId());
            callback.run(ErrorCode.ERROR100.getCode(), "非国标设备", null);
            return;
        }
        Device device = deviceMapper.query(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[点播] 未找到通道{}的设备信息", channel);
            callback.run(ErrorCode.ERROR100.getCode(), "设备不存在", null);
            return;
        }
        DeviceChannel deviceChannel = getOneForSourceById(channel.getGbId());
        queryRecordInfo(device, deviceChannel, startTime, endTime, callback);

    }
}
