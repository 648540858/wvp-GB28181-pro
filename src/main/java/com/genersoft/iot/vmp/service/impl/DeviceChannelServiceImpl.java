package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.*;
import com.genersoft.iot.vmp.conf.CivilCodeFileConf;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Gb28181CodeType;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.utils.Coordtransform;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.service.ICommonGbChannelService;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.service.bean.Group;
import com.genersoft.iot.vmp.service.bean.Region;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import org.apache.commons.lang3.StringUtils;
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
    private IInviteStreamService inviteStreamService;

    @Autowired
    private DeviceChannelMapper channelMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    EventPublisher eventPublisher;

    @Autowired
    ICommonGbChannelService commonGbChannelService;

    @Autowired
    private CivilCodeFileConf civilCodeFileConf;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private RegionMapper regionMapper;


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
            if (!addChannels.isEmpty()) {
                if (addChannels.size() > BatchLimit.count) {
                    for (int i = 0; i < addChannels.size(); i += BatchLimit.count) {
                        int toIndex = i + BatchLimit.count;
                        if (i + BatchLimit.count > addChannels.size()) {
                            toIndex = addChannels.size();
                        }
                        channelMapper.batchAdd(addChannels.subList(i, toIndex));
                    }
                }else {
                    channelMapper.batchAdd(addChannels);
                }
            }
            if (!updateChannels.isEmpty()) {
                if (updateChannels.size() > BatchLimit.count) {
                    for (int i = 0; i < updateChannels.size(); i += BatchLimit.count) {
                        int toIndex = i + BatchLimit.count;
                        if (i + BatchLimit.count > updateChannels.size()) {
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
        if (result.size() > BatchLimit.count) {
            for (int i = 0; i < result.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > result.size()) {
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
    public boolean updateChannels(Device device, List<DeviceChannel> deviceChannelList) {
        if (CollectionUtils.isEmpty(deviceChannelList)) {
            return false;
        }
        Map<String, DeviceChannel> allChannelMap = channelMapper.queryAllChannelsForMap(device.getDeviceId());

        // 存储数据，方便对数据去重
        List<DeviceChannel> channels = new ArrayList<>();

        // 存储需要更新的数据
        List<DeviceChannel> updateChannelsForInfo = new ArrayList<>();
        List<CommonGbChannel> updateCommonChannelsForInfo = new ArrayList<>();
        // 存储需要需要新增的数据库
        List<DeviceChannel> addChannels = new ArrayList<>();
        List<CommonGbChannel> addCommonChannels = new ArrayList<>();

        Map<String, Integer> subContMap = new HashMap<>();

        // 存储得到的10到13位为215的业务分组数据
        Map<String, Group> businessGroupMap = new HashMap<>();
        // 存储得到的10到13位为216的虚拟组织 数据
        Map<String, Group> virtuallyGroupMap = new HashMap<>();
        // 存储得到的行政区划数据
        Map<String, Region> regionMap = new HashMap<>();
        // 存储得到的所有行政区划, 后续检验civilCode是否已传输对应的行政区划数据，从而确定是否需要自动创建节点。
        Set<String> civilCodeSet = new HashSet<>();
        List<String> clearChannels = new ArrayList<>();

        Set<String> gbIdSet = new HashSet<>();
        for (DeviceChannel deviceChannel : deviceChannelList) {
            // 数据去重
            if (gbIdSet.contains(deviceChannel.getChannelId())) {
                logger.info("[目录查询]收到的数据存在重复： {}" , deviceChannel.getChannelId());
                continue;
            }
            gbIdSet.add(deviceChannel.getChannelId());
            if (allChannelMap.containsKey(deviceChannel.getChannelId())) {
                DeviceChannel channelInDb = allChannelMap.get(deviceChannel.getChannelId());
                deviceChannel.setId(channelInDb.getId());
                deviceChannel.setStreamId(channelInDb.getStreamId());
                deviceChannel.setHasAudio(channelInDb.isHasAudio());
                deviceChannel.setCommonGbChannelId(channelInDb.getCommonGbChannelId());
                deviceChannel.setUpdateTime(DateUtil.getNow());
                // 同步时发现状态变化
                updateChannelsForInfo.add(deviceChannel);
                updateCommonChannelsForInfo.add(CommonGbChannel.getInstance(null, deviceChannel));
                // 将需要更新的移除，剩下的都是需要删除的了
                allChannelMap.remove(deviceChannel.getChannelId());
            }else {
                deviceChannel.setCreateTime(DateUtil.getNow());
                deviceChannel.setUpdateTime(DateUtil.getNow());
                addChannels.add(deviceChannel);

                Gb28181CodeType channelIdType = SipUtils.getChannelIdType(deviceChannel.getChannelId());
                if (channelIdType != null) {
                    if (
                            (
                                    channelIdType == Gb28181CodeType.CIVIL_CODE_PROVINCE
                                            || channelIdType == Gb28181CodeType.CIVIL_CODE_CITY
                                            || channelIdType == Gb28181CodeType.CIVIL_CODE_COUNTY
                                            || channelIdType == Gb28181CodeType.CIVIL_CODE_GRASS_ROOTS
                            )
                                    &&
                                    !regionMap.containsKey(deviceChannel.getChannelId())
                    ) {
                        CivilCodePo parentCivilCodePo = civilCodeFileConf.getParentCode(deviceChannel.getChannelId());
                        String civilCode = null;
                        if (parentCivilCodePo != null) {
                            civilCode = parentCivilCodePo.getCode();
                        }
                        // 行政区划条目
                        Region region = Region.getInstance(deviceChannel.getChannelId(), deviceChannel.getName(),
                                civilCode);
                        regionMap.put(deviceChannel.getChannelId(), region);
                    }
                    if (channelIdType == Gb28181CodeType.BUSINESS_GROUP
                            && !businessGroupMap.containsKey(deviceChannel.getChannelId())) {
                        Group group = Group.getInstance(deviceChannel.getChannelId(), deviceChannel.getName(),
                                null, deviceChannel.getChannelId());
                        businessGroupMap.put(deviceChannel.getChannelId(), group);
                    }
                    if (channelIdType == Gb28181CodeType.VIRTUAL_ORGANIZATION
                            && !virtuallyGroupMap.containsKey(deviceChannel.getChannelId())) {
                        Group group = Group.getInstance(deviceChannel.getChannelId(), deviceChannel.getName(), deviceChannel.getParentId(), deviceChannel.getBusinessGroupId());
                        virtuallyGroupMap.put(deviceChannel.getChannelId(), group);
                    }
                }else {
                    if (!StringUtils.isEmpty(deviceChannel.getCivilCode())) {
                        civilCodeSet.add(deviceChannel.getCivilCode());
                    }
                    addCommonChannels.add(CommonGbChannel.getInstance(null, deviceChannel));
                }

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
        if (!channels.isEmpty()) {
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

        // 检测分组境况
        if (businessGroupMap.isEmpty()) {
            virtuallyGroupMap.clear();
        }else {
            // 检查业务分组与虚拟组织
            if (!virtuallyGroupMap.isEmpty()) {
                for (String key : virtuallyGroupMap.keySet()) {
                    Group virtuallyGroup = virtuallyGroupMap.get(key);
                    if (virtuallyGroup.getCommonGroupTopId() == null
                            || !businessGroupMap.containsKey(virtuallyGroup.getCommonGroupTopId())
                    ) {
                        virtuallyGroupMap.remove(key);
                        continue;
                    }
                    if (virtuallyGroup.getCommonGroupParentId() != null && !virtuallyGroupMap.containsKey(virtuallyGroup.getCommonGroupParentId())) {
                        virtuallyGroup.setCommonGroupParentId(null);
                    }
                }
                if (virtuallyGroupMap.isEmpty()) {
                    businessGroupMap.clear();
                }
            }
        }
        // 检测行政区划信息是否完整
        for (String civilCode : civilCodeSet) {
            if (!regionMap.containsKey(civilCode)) {
                logger.warn("[通道信息中缺少地区信息]补充地区信息 civilCode： {}", civilCode );
                Region region = civilCodeFileConf.createRegion(civilCode);
                if (region != null) {
                    regionMap.put(region.getCommonRegionDeviceId(), region);
                }else {
                    logger.warn("[获取地区信息]失败 civilCode： {}", civilCode );
                }
            }
        }
        // 对待写入的数据做处理
        if (!addCommonChannels.isEmpty()) {
            addCommonChannels.stream().forEach(commonGbChannel -> {
                if (commonGbChannel.getCommonGbParentID() != null
                        && !virtuallyGroupMap.containsKey(commonGbChannel.getCommonGbParentID())) {
                    commonGbChannel.setCommonGbParentID(null);
                }
                if (commonGbChannel.getCommonGbBusinessGroupID() != null
                        && !businessGroupMap.containsKey(commonGbChannel.getCommonGbBusinessGroupID())) {
                    commonGbChannel.setCommonGbBusinessGroupID(null);
                }
                if (commonGbChannel.getCommonGbCivilCode() != null
                        && !regionMap.containsKey(commonGbChannel.getCommonGbCivilCode())) {
                    commonGbChannel.setCommonGbCivilCode(null);
                }
            });
        }


        if(CollectionUtils.isEmpty(channels)){
            logger.info("通道重设，数据为空={}" , deviceChannelList);
            return false;
        }
        try {
            // 此时allChannelMap剩余的就是需要移除的
            if (!allChannelMap.isEmpty()) {
                if (allChannelMap.size() > BatchLimit.count) {
                    for (int i = 0; i < allChannelMap.size(); i += BatchLimit.count) {
                        int toIndex = i + BatchLimit.count;
                        if (i + BatchLimit.count > allChannelMap.size()) {
                            toIndex = allChannelMap.size();
                        }
                        channelMapper.cleanChannelsInList(device.getDeviceId(),
                                new ArrayList<>(allChannelMap.values()).subList(i, toIndex));
                    }
                } else {
                    channelMapper.cleanChannelsInList(device.getDeviceId(), new ArrayList<>(allChannelMap.values()));
                }
                List<Integer> allCommonChannelsForDelete = new ArrayList<>();
                allChannelMap.values().stream().forEach((deviceChannel) -> {
                    allCommonChannelsForDelete.add(deviceChannel.getCommonGbChannelId());
                });
                // 通知通用通道批量移除
                commonGbChannelService.batchDelete(allCommonChannelsForDelete);
            }
            // addChannels 与 addCommonChannels 数量一致，这里使用同一个循环处理
            if (!addChannels.isEmpty()) {
                // 对于新增的部分需要先添加通用通道，拿到ID后再添加国标通道
                commonGbChannelService.batchAdd(addCommonChannels);
                for (int j = 0; j < addCommonChannels.size(); j++) {
                    addChannels.get(j).setCommonGbChannelId(addCommonChannels.get(j).getCommonGbId());
                }
                if (addChannels.size() > BatchLimit.count) {
                    for (int i = 0; i < addChannels.size(); i += BatchLimit.count) {
                        int toIndex = i + BatchLimit.count;
                        if (i + BatchLimit.count > addChannels.size()) {
                            toIndex = addChannels.size();
                        }
                        channelMapper.batchAdd(addChannels.subList(i, toIndex));
                    }
                }else {
                    channelMapper.batchAdd(addChannels);
                }
            }
            if (!updateChannelsForInfo.isEmpty()) {
                if (updateChannelsForInfo.size() > BatchLimit.count) {
                    for (int i = 0; i < updateChannelsForInfo.size(); i += BatchLimit.count) {
                        int toIndex = i + BatchLimit.count;
                        if (i + BatchLimit.count > updateChannelsForInfo.size()) {
                            toIndex = updateChannelsForInfo.size();
                        }
                        channelMapper.batchUpdate(updateChannelsForInfo.subList(i, toIndex));
                    }
                }else {
                    channelMapper.batchUpdate(updateChannelsForInfo);
                }
                commonGbChannelService.batchUpdate(updateCommonChannelsForInfo);
            }
            // 写入分组数据
            List<Group> allGroup = new ArrayList<>(businessGroupMap.values());
            allGroup.addAll(virtuallyGroupMap.values());
            if (!allGroup.isEmpty()) {
                // 这里也采取只插入新数据的方式
                List<Group> groupInDBList = groupMapper.queryInList(allGroup);
                if (!groupInDBList.isEmpty()) {
                    groupInDBList.stream().forEach(groupInDB -> {
                        for (int i = 0; i < allGroup.size(); i++) {
                            if (groupInDB.getCommonGroupDeviceId().equalsIgnoreCase(allGroup.get(i).getCommonGroupDeviceId())) {
                                allGroup.remove(i);
                                break;
                            }
                        }
                    });
                }
                if (!allGroup.isEmpty()) {
                    if (allGroup.size() <= BatchLimit.count) {
                        groupMapper.addAll(allGroup);
                    } else {
                        for (int i = 0; i < allGroup.size(); i += BatchLimit.count) {
                            int toIndex = i + BatchLimit.count;
                            if (i + BatchLimit.count > allGroup.size()) {
                                toIndex = allGroup.size();
                            }
                            groupMapper.addAll(allGroup.subList(i, toIndex));
                        }
                    }
                }
            }
            // 写入地区
            List<Region> allRegion = new ArrayList<>(regionMap.values());

            if (!allRegion.isEmpty()) {
                // 这里也采取只插入新数据的方式
                List<Region> regionInDBList = regionMapper.queryInList(allRegion);
                List<Region> regionInForUpdate = new ArrayList<>();
                if (!regionInDBList.isEmpty()) {
                    regionInDBList.stream().forEach(regionInDB -> {
                        for (int i = 0; i < allRegion.size(); i++) {
                            if (regionInDB.getCommonRegionDeviceId().equalsIgnoreCase(allRegion.get(i).getCommonRegionDeviceId())) {
                                if (!regionInDB.getCommonRegionName().equals(allRegion.get(i).getCommonRegionName())) {
                                    regionInForUpdate.add(allRegion.get(i));
                                }
                                allRegion.remove(i);
                                break;
                            }
                        }
                    });
                }
                if (!allRegion.isEmpty()) {
                    if (allRegion.size() <= BatchLimit.count) {
                        if (regionMapper.addAll(allRegion) <= 0) {
                            regionMapper.addAll(allRegion);
                        }
                    } else {
                        for (int i = 0; i < allRegion.size(); i += BatchLimit.count) {
                            int toIndex = i + BatchLimit.count;
                            if (i + BatchLimit.count > allRegion.size()) {
                                toIndex = allRegion.size();
                            }
                            List<Region> allRegionSub = allRegion.subList(i, toIndex);
                            regionMapper.addAll(allRegionSub);
                        }
                    }
                }
                // 对于名称变化的地区进行修改
                if (!regionInForUpdate.isEmpty()) {
                    regionMapper.updateAllForName(regionInForUpdate);
                }
            }
            return true;
        }catch (Exception e) {
            logger.error("未处理的异常 ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }
}
