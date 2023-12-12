package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.*;
import com.genersoft.iot.vmp.conf.CivilCodeFileConf;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Gb28181CodeType;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.utils.Coordtransform;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.service.ICommonGbChannelService;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.service.bean.Group;
import com.genersoft.iot.vmp.service.bean.Region;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
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
    private CommonChannelMapper commonGbChannelMapper;

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
    public int updateChannelsForCatalog(String deviceId, List<DeviceChannel> channels) {
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
        List<CommonGbChannel> commonGbChannelList = new ArrayList<>();
        channels.stream().forEach(channel->{
            commonGbChannelList.add(CommonGbChannel.getInstance(null, channel));
        });

        channelMapper.batchAdd(channels);
        for (DeviceChannel channel : channels) {
            if (channel.getParentId() != null) {
                channelMapper.updateChannelSubCount(channel.getDeviceId(), channel.getParentId());
            }
        }

    }

    @Override
    @Transactional
    public boolean updateChannelsForCatalog(Device device, List<DeviceChannel> deviceChannelList) {
        if (CollectionUtils.isEmpty(deviceChannelList)) {
            return false;
        }
        Map<String, DeviceChannel> allChannelMap = channelMapper.queryAllChannelsForMap(device.getDeviceId());
        Map<String, CommonGbChannel> allCommonChannelMap = commonGbChannelMapper.queryAllChannelsForMap();

        // 存储数据，方便对数据去重
        List<DeviceChannel> channels = new ArrayList<>();

        // 存储需要需要新增的国标通道
        List<DeviceChannel> addChannelList = new ArrayList<>();
        // 存储需要更新的国标通道
        List<DeviceChannel> updateChannelList = new ArrayList<>();

        // 存储需要需要新增的通用通道
        List<CommonGbChannel> addCommonChannelList = new ArrayList<>();
        // 存储需要更新的通用通道
        List<CommonGbChannel> updateCommonChannelList = new ArrayList<>();

        // 存储需要需要新增的分组
        List<Group> addGroupList = new ArrayList<>();
        // 存储需要更新的分组
        List<Group> updateGroupList = new ArrayList<>();

        // 存储需要需要新增的行政区划
        List<Region> addRegionList = new ArrayList<>();
        // 存储需要更新的行政区划
        List<Region> updateRegionList = new ArrayList<>();

        Map<String, Integer> subContMap = new HashMap<>();

        // 存储得到的10到13位为215的业务分组数据, 默认加载数据库中的所有业务分组
        Map<String, Group> businessGroupMap = groupMapper.queryTopGroupForMap();
        // 存储得到的10到13位为216的虚拟组织 数据, 默认加载数据库中的所有虚拟组织
        Map<String, Group> virtuallyGroupMap = groupMapper.queryNotTopGroupForMap();
        // 存储得到的行政区划数据, 默认加载数据库中的所有行政区划
        Map<String, Region> regionMap = regionMapper.getAllForMap();
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
            Gb28181CodeType channelIdType = SipUtils.getChannelIdType(deviceChannel.getChannelId());
            // 处理国标通道相关的判断
            if (allChannelMap.containsKey(deviceChannel.getChannelId())) {
                DeviceChannel channelInDb = allChannelMap.get(deviceChannel.getChannelId());
                deviceChannel.setId(channelInDb.getId());
                deviceChannel.setStreamId(channelInDb.getStreamId());
                deviceChannel.setHasAudio(channelInDb.isHasAudio());
                deviceChannel.setCommonGbChannelId(channelInDb.getCommonGbChannelId());
                deviceChannel.setUpdateTime(DateUtil.getNow());
                // 同步时发现状态变化
                updateChannelList.add(deviceChannel);
                // 将需要更新的移除，剩下的都是需要删除的了
                allChannelMap.remove(deviceChannel.getChannelId());
            }else {
                deviceChannel.setCreateTime(DateUtil.getNow());
                deviceChannel.setUpdateTime(DateUtil.getNow());
                addChannelList.add(deviceChannel);
            }
            if (channelIdType == null) {
                if (allCommonChannelMap.containsKey(deviceChannel.getChannelId())) {
                    updateCommonChannelList.add(CommonGbChannel.getInstance(null, deviceChannel));
                }else {
                    addCommonChannelList.add(CommonGbChannel.getInstance(null, deviceChannel));
                }
            }


            if (channelIdType != null) {
                if (isRegion(channelIdType)) {
                    Region region;
                    if (!regionMap.containsKey(deviceChannel.getChannelId())) {
                        // 区域不存在记录并新增
                        CivilCodePo parentCivilCodePo = civilCodeFileConf.getParentCode(deviceChannel.getChannelId());
                        String civilCode = null;
                        if (parentCivilCodePo != null) {
                            civilCode = parentCivilCodePo.getCode();
                        }
                        // 行政区划条目
                        region = Region.getInstance(deviceChannel.getChannelId(), deviceChannel.getName(),
                                civilCode);
                        addRegionList.add(region);
                    }else {
                        // 区域存在记录并检查是否需要更新
                        region = regionMap.get(deviceChannel.getChannelId());
                        if (region.getCommonRegionName().equals(deviceChannel.getName())) {
                            // 对于行政区划，父节点是不会变化的，所以只需要判断名称变化，执行更新就可以
                            region.setCommonRegionName(deviceChannel.getName());
                            updateRegionList.add(region);
                        }
                    }
                    regionMap.put(region.getCommonRegionDeviceId(), region);

                }else if (channelIdType == Gb28181CodeType.BUSINESS_GROUP) {
                    Group group;
                    if (!businessGroupMap.containsKey(deviceChannel.getChannelId())) {
                        group = Group.getInstance(deviceChannel.getChannelId(), deviceChannel.getName(),
                                null, deviceChannel.getChannelId());
                        addGroupList.add(group);
                    }else {
                        // 对于业务分组，因为它本身即使顶级节点，所以不能父节点变化，所以只需要考虑名称变化的情况
                        group = businessGroupMap.get(deviceChannel.getChannelId());
                        if (!group.getCommonGroupName().equals(deviceChannel.getName())) {
                            group.setCommonGroupName(deviceChannel.getName());
                        }
                        updateGroupList.add(group);
                    }
                    businessGroupMap.put(group.getCommonGroupDeviceId(), group);

                }else if (channelIdType == Gb28181CodeType.VIRTUAL_ORGANIZATION) {
                    Group group;
                    if (!virtuallyGroupMap.containsKey(deviceChannel.getChannelId())) {
                        group = Group.getInstance(deviceChannel.getChannelId(), deviceChannel.getName(), deviceChannel.getParentId(), deviceChannel.getBusinessGroupId());
                        addGroupList.add(group);
                    }else {
                        // 对于虚拟组织的变化，需要考虑三点， 名称， 顶级父节点（所属业务分组）或者 父节点
                        group = virtuallyGroupMap.get(deviceChannel.getChannelId());
                        if (!group.getCommonGroupName().equals(device.getName())
                                || !group.getCommonGroupParentId().equals(deviceChannel.getParentId())
                                || !group.getCommonGroupTopId().equals(deviceChannel.getBusinessGroupId())
                        ) {
                            group.setCommonGroupName(deviceChannel.getName());
                            group.setCommonGroupTopId(deviceChannel.getBusinessGroupId());
                            group.setCommonGroupParentId(deviceChannel.getParentId());
                            updateGroupList.add(group);
                        }
                    }
                    virtuallyGroupMap.put(group.getCommonGroupDeviceId(), group);
                }
            }

            channels.add(deviceChannel);
            // 统计每个节点的子节点个数
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
                for (int i = 0; i < addGroupList.size(); i++) {
                    Group group = addGroupList.get(i);
                    if (ObjectUtils.isEmpty(group.getCommonGroupTopId())) {
                        if (businessGroupMap.containsKey(group.getCommonGroupParentId())) {
                            group.setCommonGroupTopId(group.getCommonGroupParentId());
                        }else {
                            addGroupList.remove(i);
                            i--;
                            continue;
                        }
                    }else {
                        if (!businessGroupMap.containsKey(group.getCommonGroupTopId())) {
                            addGroupList.remove(i);
                            i--;
                            continue;
                        }
                    }

                    if (!ObjectUtils.isEmpty(group.getCommonGroupParentId())
                            && !virtuallyGroupMap.containsKey(group.getCommonGroupParentId())) {
                        addGroupList.remove(i);
                        i--;
                        continue;
                    }
                    if (!businessGroupMap.containsKey(group.getCommonGroupTopId())) {
                        group.setCommonGroupTopId(null);
                    }
                }
            }
        }
        // 对于只发送了行政区划编号，没有发送行政区划的情况进行兼容，自动为通道创建一个行政区划。
        for (String civilCode : civilCodeSet) {
            if (!regionMap.containsKey(civilCode)) {
                logger.warn("[通道信息中缺少地区信息]补充地区信息 civilCode： {}", civilCode );
                if (civilCode.length() == 8) {
                    Region parentRegion = civilCodeFileConf.createRegion(civilCode.substring(0, 6));
                    if (parentRegion != null) {
                        Region region = Region.getInstance(civilCode,
                                parentRegion.getCommonRegionName() + "的基层组织",
                                parentRegion.getCommonRegionDeviceId());
                        regionMap.put(region.getCommonRegionDeviceId(), region);
                        addRegionList.add(region);
                    }else {
                        logger.warn("[获取地区信息]失败 civilCode： {}", civilCode );
                    }
                }else if (civilCode.length() <= 6) {
                    Region region = civilCodeFileConf.createRegion(civilCode);
                    if (region != null) {
                        regionMap.put(region.getCommonRegionDeviceId(), region);
                        addRegionList.add(region);
                    }else {
                        logger.warn("[获取地区信息]失败 civilCode： {}", civilCode );
                    }
                }
            }
        }
        // 对待写入的数据做处理
        if (!addCommonChannelList.isEmpty()) {
            addCommonChannelList.stream().forEach(commonGbChannel -> {
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
            if (!addChannelList.isEmpty()) {
                // 对于新增的部分需要先添加通用通道，拿到ID后再添加国标通道
                commonGbChannelService.batchAdd(addCommonChannelList);
                for (int j = 0; j < addCommonChannelList.size(); j++) {
                    addChannelList.get(j).setCommonGbChannelId(addCommonChannelList.get(j).getCommonGbId());
                }
                addChannelHandler(addChannelList);

            }
            if (!updateChannelList.isEmpty()) {
                commonGbChannelService.batchUpdate(updateCommonChannelList);
                updateChannelHandler(updateChannelList);
            }
            // 写入分组数据
            if (!addGroupList.isEmpty()) {
                addGroupHandler(addGroupList);
            }
            if (!updateGroupList.isEmpty()) {
                updateGroupHandler(updateGroupList);
            }

            // 写入地区
            if (!addRegionList.isEmpty()) {
                // 如果下级未发送完整的区域树，则通过自动探查补全
                addRegionList.stream().forEach((region -> {
                    if (!regionMap.containsKey(region.getCommonRegionParentId())
                            && !ObjectUtils.isEmpty(region.getCommonRegionParentId())
                            && region.getCommonRegionParentId().length() > 2) {
                        Region parentRegion = civilCodeFileConf.createRegion(region.getCommonRegionParentId());
                        addRegionList.add(parentRegion);
                        String parentDeviceId = parentRegion.getCommonRegionParentId();
                        if (parentDeviceId.length() == 6) {
                            CivilCodePo parentCode = civilCodeFileConf.getParentCode(region.getCommonRegionDeviceId());
                            if (parentCode == null) {
                                return;
                            }
                            parentDeviceId = parentCode.getParentCode();
                            if (regionMap.containsKey(region.getCommonRegionDeviceId())) {
                                addRegionList.add(Region.getInstance(parentCode.getCode(),
                                        parentCode.getCode(), parentCode.getParentCode()));
                            }
                        }
                        if (parentDeviceId.length() == 4) {
                            CivilCodePo parentCode = civilCodeFileConf.getParentCode(region.getCommonRegionDeviceId());
                            if (parentCode == null) {
                                return;
                            }
                            addRegionList.add(Region.getInstance(parentCode.getCode(),
                                    parentCode.getCode(), parentCode.getParentCode()));
                        }
                    }
                }));
                addRegionHandler(addRegionList);
            }
            if (!updateRegionList.isEmpty()) {
                updateRegionHandler(updateRegionList);
            }
            return true;
        }catch (Exception e) {
            logger.error("未处理的异常 ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    private boolean isRegion(Gb28181CodeType channelIdType) {
        return channelIdType == Gb28181CodeType.CIVIL_CODE_PROVINCE
                || channelIdType == Gb28181CodeType.CIVIL_CODE_CITY
                || channelIdType == Gb28181CodeType.CIVIL_CODE_COUNTY
                || channelIdType == Gb28181CodeType.CIVIL_CODE_GRASS_ROOTS;
    }

    private void addChannelHandler(List<DeviceChannel> addChannels) {
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

    private void updateChannelHandler(List<DeviceChannel> updateChannels) {
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

    private void addRegionHandler(List<Region> regionList) {
        if (regionList.size() <= BatchLimit.count) {
            if (regionMapper.addAll(regionList) <= 0) {
                regionMapper.addAll(regionList);
            }
        } else {
            for (int i = 0; i < regionList.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > regionList.size()) {
                    toIndex = regionList.size();
                }
                List<Region> allRegionSub = regionList.subList(i, toIndex);
                regionMapper.addAll(allRegionSub);
            }
        }
    }

    private void updateRegionHandler(List<Region> regionList) {
        if (regionList.size() <= BatchLimit.count) {
            regionMapper.updateAllForName(regionList);
        } else {
            for (int i = 0; i < regionList.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > regionList.size()) {
                    toIndex = regionList.size();
                }
                List<Region> allRegionSub = regionList.subList(i, toIndex);
                regionMapper.updateAllForName(allRegionSub);
            }
        }
    }

    private void addGroupHandler(List<Group> groupList) {
        if (groupList.size() <= BatchLimit.count) {
            groupMapper.addAll(groupList);
        } else {
            for (int i = 0; i < groupList.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > groupList.size()) {
                    toIndex = groupList.size();
                }
                groupMapper.addAll(groupList.subList(i, toIndex));
            }
        }
    }

    private void updateGroupHandler(List<Group> groupList) {
        if (groupList.size() <= BatchLimit.count) {
            groupMapper.updateAll(groupList);
        } else {
            for (int i = 0; i < groupList.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > groupList.size()) {
                    toIndex = groupList.size();
                }
                groupMapper.updateAll(groupList.subList(i, toIndex));
            }
        }
    }
}
