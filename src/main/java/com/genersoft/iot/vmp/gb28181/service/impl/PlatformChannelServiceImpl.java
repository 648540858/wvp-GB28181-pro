package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelListForRpcParam;
import com.genersoft.iot.vmp.gb28181.dao.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.channel.ChannelEvent;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lin
 */
@Slf4j
@Service
public class PlatformChannelServiceImpl implements IPlatformChannelService {

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private GroupMapper groupMapper;


    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private CommonGBChannelMapper commonGBChannelMapper;

    @Autowired
    private PlatformMapper platformMapper;

    @Autowired
    private ISIPCommanderForPlatform sipCommanderFroPlatform;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisRpcService redisRpcService;

    // 监听通道信息变化
    @EventListener
    public void onApplicationEvent(ChannelEvent event) {
        if (event.getChannels().isEmpty()) {
            log.info("[国标级联-处理通道变化事件] 通道数量为空");
            return;
        }
        String deviceIds = event.getChannels().stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
        log.info("[国标级联-处理通道变化事件] 类型： {}, 通道: {}", event.getMessageType(), deviceIds);
        // 获取通道所关联的平台
        List<Platform> allPlatform = platformMapper.queryByServerId(userSetting.getServerId());
        if (allPlatform.isEmpty()) {
            log.info("[国标级联-处理通道变化事件] 没有当前服务负责的平台");
            return;
        }
        // 获取所用订阅
        List<String> platforms = subscribeHolder.getAllCatalogSubscribePlatform(allPlatform);

        Map<String, List<Platform>> platformMap = new HashMap<>();
        Map<String, CommonGBChannel> channelMap = new HashMap<>();
        if (platforms.isEmpty()) {
            log.info("[国标级联-处理通道变化事件] 没有关联的平台的目录订阅");
            return;
        }
        for (CommonGBChannel deviceChannel : event.getChannels()) {
            List<Platform> parentPlatformsForGB = queryPlatFormListByChannelDeviceId(
                    deviceChannel.getGbId(), platforms);
            platformMap.put(deviceChannel.getGbDeviceId(), parentPlatformsForGB);
            channelMap.put(deviceChannel.getGbDeviceId(), deviceChannel);
        }
        if (platformMap.isEmpty()) {
            log.info("[国标级联-处理通道变化事件] 开启订阅的平台都没有关联通道： {}", deviceIds);
            return;
        }
        switch (event.getMessageType()) {
            case ON:
            case OFF:
            case DEL:
                for (String serverGbId : platformMap.keySet()) {
                    List<Platform> platformList = platformMap.get(serverGbId);
                    if (platformList != null && !platformList.isEmpty()) {
                        for (Platform platform : platformList) {
                            SubscribeInfo subscribeInfo = subscribeHolder.getCatalogSubscribe(platform.getServerGBId());
                            if (subscribeInfo == null) {
                                continue;
                            }
                            log.info("[Catalog事件: {}]平台：{}，影响通道{}", event.getMessageType(), platform.getServerGBId(), serverGbId);
                            List<CommonGBChannel> deviceChannelList = new ArrayList<>();
                            CommonGBChannel deviceChannel = new CommonGBChannel();
                            deviceChannel.setGbDeviceId(serverGbId);
                            deviceChannelList.add(deviceChannel);
                            try {
                                sipCommanderFroPlatform.sendNotifyForCatalogOther(event.getMessageType().name(), platform, deviceChannelList, subscribeInfo, null);
                            } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                                     IllegalAccessException e) {
                                log.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                            }
                        }
                    }else {
                        log.info("[Catalog事件: {}] 没有需要通知的上级平台： {}", event.getMessageType(), serverGbId);
                    }
                }
                break;
            case VLOST:
                break;
            case DEFECT:
                break;
            case ADD:
            case UPDATE:
                for (String gbId : platformMap.keySet()) {
                    List<Platform> parentPlatforms = platformMap.get(gbId);
                    if (parentPlatforms != null && !parentPlatforms.isEmpty()) {
                        for (Platform platform : parentPlatforms) {
                            SubscribeInfo subscribeInfo = subscribeHolder.getCatalogSubscribe(platform.getServerGBId());
                            if (subscribeInfo == null) {
                                continue;
                            }
                            log.info("[Catalog事件: {}]平台：{}，影响通道{}", event.getMessageType(), platform.getServerGBId(), gbId);
                            List<CommonGBChannel> channelList = new ArrayList<>();
                            CommonGBChannel deviceChannel = channelMap.get(gbId);
                            channelList.add(deviceChannel);
                            try {
                                sipCommanderFroPlatform.sendNotifyForCatalogAddOrUpdate(event.getMessageType().name(), platform, channelList, subscribeInfo, null);
                            } catch (InvalidArgumentException | ParseException | NoSuchFieldException |
                                     SipException | IllegalAccessException e) {
                                log.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    @EventListener
    public void onApplicationEvent(CatalogEvent event) {
        String deviceIds = event.getChannels().stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
        log.info("[Catalog事件: {}] 通道： {}", event.getType(), deviceIds);
        Platform platform = event.getPlatform();
        if (platform == null || platform.getServerGBId() == null) {
            log.info("[Catalog事件: {}] 缺少通道或通道数据异常： {}", event.getType(), deviceIds);
            return;
        }
        SubscribeInfo subscribe = subscribeHolder.getCatalogSubscribe(platform.getServerGBId());
        if (subscribe == null) {
            log.info("[Catalog事件: {}] 平台未被目录订阅，取消发送： {}", event.getType(), deviceIds);
            return;
        }
        switch (event.getType()) {
            case CatalogEvent.ON:
            case CatalogEvent.OFF:
            case CatalogEvent.DEL:
                List<CommonGBChannel> channels = new ArrayList<>();
                if (event.getChannels() != null) {
                    channels.addAll(event.getChannels());
                }
                if (!channels.isEmpty()) {
                    log.info("[Catalog事件: {}]平台：{}，影响通道{}", event.getType(), platform.getServerGBId(), deviceIds);
                    try {
                        sipCommanderFroPlatform.sendNotifyForCatalogOther(event.getType(), platform, channels, subscribe, null);
                    } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                             IllegalAccessException e) {
                        log.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                    }
                }
                break;
            case CatalogEvent.VLOST:
                break;
            case CatalogEvent.DEFECT:
                break;
            case CatalogEvent.ADD:
            case CatalogEvent.UPDATE:
                List<CommonGBChannel> deviceChannelList = new ArrayList<>();
                if (event.getChannels() != null) {
                    deviceChannelList.addAll(event.getChannels());
                }
                if (!deviceChannelList.isEmpty()) {
                    log.info("[Catalog事件: {}]平台：{}，影响通道{}", event.getType(), platform.getServerGBId(), deviceIds);
                    try {
                        sipCommanderFroPlatform.sendNotifyForCatalogAddOrUpdate(event.getType(), platform, deviceChannelList, subscribe, null);
                    } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                             IllegalAccessException e) {
                        log.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                    }
                }
                break;
            default:
                break;
        }
    }


    @Override
    public PageInfo<PlatformChannel> queryChannelList(int page, int count, String query, Integer channelType, Boolean online, Integer platformId, Boolean hasShare) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<PlatformChannel> all = platformChannelMapper.queryForPlatformForWebList(platformId, query, channelType, online, hasShare);
        return new PageInfo<>(all);
    }

    /**
     * 获取通道使用的分组中未分享的
     */
    @Transactional
    public Set<Group> getGroupNotShareByChannelList(List<CommonGBChannel> channelList, Integer platformId) {
        // 获取分组中未分享的节点
        Set<Group> groupList = groupMapper.queryNotShareGroupForPlatformByChannelList(channelList, platformId);
        // 获取这些节点的所有父节点
        if (groupList.isEmpty()) {
            return new HashSet<>();
        }
        Set<Group> allGroup = getAllGroup(groupList);
        allGroup.addAll(groupList);
        // 获取全部节点中未分享的
        return groupMapper.queryNotShareGroupForPlatformByGroupList(allGroup, platformId);
    }

    /**
     * 获取通道使用的分组中未分享的
     */
    private Set<Region> getRegionNotShareByChannelList(List<CommonGBChannel> channelList, Integer platformId) {
        // 获取分组中未分享的节点
        Set<Region> regionSet = regionMapper.queryNotShareRegionForPlatformByChannelList(channelList, platformId);
        // 获取这些节点的所有父节点
        if (regionSet.isEmpty()) {
            return new HashSet<>();
        }
        Set<Region> allRegion = getAllRegion(regionSet);
        allRegion.addAll(regionSet);
        // 获取全部节点中未分享的
        return regionMapper.queryNotShareRegionForPlatformByRegionList(allRegion, platformId);
    }

    /**
     * 移除空的共享，并返回移除的分组
     */
    @Transactional
    public Set<Group> deleteEmptyGroup(Set<Group> groupSet, Integer platformId) {
        Iterator<Group> iterator = groupSet.iterator();
        while (iterator.hasNext()) {
            Group group = iterator.next();
            // groupSet 为当前通道直接使用的分组，如果已经没有子分组与其他的通道，则可以移除
            // 获取分组子节点
            Set<Group> children = platformChannelMapper.queryShareChildrenGroup(group.getId(), platformId);
            if (!children.isEmpty()) {
                iterator.remove();
                continue;
            }
            // 获取分组关联的通道
            List<CommonGBChannel> channelList = commonGBChannelMapper.queryShareChannelByParentId(group.getDeviceId(), platformId);
            if (!channelList.isEmpty()) {
                iterator.remove();
                continue;
            }
            platformChannelMapper.removePlatformGroupById(group.getId(), platformId);
        }
        // 如果空了，说明没有通道需要处理了
        if (groupSet.isEmpty()) {
            return new HashSet<>();
        }
        Set<Group> parent =  platformChannelMapper.queryShareParentGroupByGroupSet(groupSet, platformId);
        if (parent.isEmpty()) {
            return groupSet;
        }else {
            Set<Group> parentGroupSet = deleteEmptyGroup(parent, platformId);
            groupSet.addAll(parentGroupSet);
            return groupSet;
        }
    }

    /**
     * 移除空的共享，并返回移除的行政区划
     */
    private Set<Region> deleteEmptyRegion(Set<Region> regionSet, Integer platformId) {
        Iterator<Region> iterator = regionSet.iterator();
        while (iterator.hasNext()) {
            Region region = iterator.next();
            // groupSet 为当前通道直接使用的分组，如果已经没有子分组与其他的通道，则可以移除
            // 获取分组子节点
            Set<Region> children = platformChannelMapper.queryShareChildrenRegion(region.getDeviceId(), platformId);
            if (!children.isEmpty()) {
                iterator.remove();
                continue;
            }
            // 获取分组关联的通道
            List<CommonGBChannel> channelList = commonGBChannelMapper.queryShareChannelByCivilCode(region.getDeviceId(), platformId);
            if (!channelList.isEmpty()) {
                iterator.remove();
                continue;
            }
            platformChannelMapper.removePlatformRegionById(region.getId(), platformId);
        }
        // 如果空了，说明没有通道需要处理了
        if (regionSet.isEmpty()) {
            return new HashSet<>();
        }
        Set<Region> parent =  platformChannelMapper.queryShareParentRegionByRegionSet(regionSet, platformId);
        if (parent.isEmpty()) {
            return regionSet;
        }else {
            Set<Region> parentGroupSet = deleteEmptyRegion(parent, platformId);
            regionSet.addAll(parentGroupSet);
            return regionSet;
        }
    }

    private Set<Group> getAllGroup(Set<Group> groupList ) {
        if (groupList.isEmpty()) {
            return new HashSet<>();
        }
        Set<Group> channelList = groupMapper.queryParentInChannelList(groupList);
        if (channelList.isEmpty()) {
            return channelList;
        }
        Set<Group> allParentRegion = getAllGroup(channelList);
        channelList.addAll(allParentRegion);
        return channelList;
    }

    private Set<Region> getAllRegion(Set<Region> regionSet ) {
        if (regionSet.isEmpty()) {
            return new HashSet<>();
        }

        Set<Region> channelList = regionMapper.queryParentInChannelList(regionSet);
        if (channelList.isEmpty()) {
            return channelList;
        }
        Set<Region> allParentRegion = getAllRegion(channelList);
        channelList.addAll(allParentRegion);
        return channelList;
    }

    @Override
    @Transactional
    public int addAllChannel(Integer platformId) {
        List<CommonGBChannel> channelListNotShare = platformChannelMapper.queryNotShare(platformId, null);
        Assert.notEmpty(channelListNotShare, "所有通道已共享");
        return addChannelList(platformId, channelListNotShare);
    }

    @Override
    @Transactional
    public int addChannels(Integer platformId, List<Integer> channelIds) {
        List<CommonGBChannel> channelListNotShare = platformChannelMapper.queryNotShare(platformId, channelIds);
        Assert.notEmpty(channelListNotShare, "通道已共享");
        return addChannelList(platformId, channelListNotShare);
    }

    @Transactional
    public int addChannelList(Integer platformId, List<CommonGBChannel> channelList) {
        Platform platform = platformMapper.query(platformId);
        Assert.notNull(platform, "平台不存在");
        String channelDeviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));

        log.info("[共享通道] 平台：{}， 通道：{}", platform.getServerGBId(), channelDeviceIds);
        if (!userSetting.getServerId().equals(platform.getServerId())) {

            List<Integer> channelIdList = channelList.stream().map(CommonGBChannel::getGbId).toList();
            int result = redisRpcService.addPlatformChannelList(platform.getServerId(), new ChannelListForRpcParam(channelIdList, platformId));
            if (result > 0) {
                log.info("[跨平台-共享通道] 成功， 平台：{}， 通道：{}", platform.getServerGBId(), channelDeviceIds);
            }else {
                log.info("[跨平台-共享通道] 失败， 平台：{}， 通道：{}", platform.getServerGBId(), channelDeviceIds);
            }
            return result;
        }
        int result = platformChannelMapper.addChannels(platformId, channelList);
        if (result > 0) {
            // 查询通道相关的行政区划信息是否共享，如果没共享就添加
            Set<Region> regionListNotShare =  getRegionNotShareByChannelList(channelList, platformId);
            if (!regionListNotShare.isEmpty()) {
                int addGroupResult = platformChannelMapper.addPlatformRegion(new ArrayList<>(regionListNotShare), platformId);
                if (addGroupResult > 0) {
                    for (Region region : regionListNotShare) {
                        // 分组信息排序时需要将顶层排在最后
                        channelList.add(0, CommonGBChannel.build(region));
                    }
                }
            }

            // 查询通道相关的分组信息是否共享，如果没共享就添加
            Set<Group> groupListNotShare =  getGroupNotShareByChannelList(channelList, platformId);
            if (!groupListNotShare.isEmpty()) {
                int addGroupResult = platformChannelMapper.addPlatformGroup(new ArrayList<>(groupListNotShare), platformId);
                if (addGroupResult > 0) {
                    for (Group group : groupListNotShare) {
                        // 分组信息排序时需要将顶层排在最后
                        channelList.add(0, CommonGBChannel.build(group));
                    }
                }
            }
            // 发送消息
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(platform, channelList, CatalogEvent.ADD);
            } catch (Exception e) {
                log.warn("[关联通道] 发送失败，数量：{}", channelList.size(), e);
            }
        }
        return result;
    }

    @Override
    public int removeAllChannel(Integer platformId) {
        Platform platform = platformMapper.query(platformId);
        if (platform == null) {
            return 0;
        }
        log.info("[取消共享通道] 平台：{}， 通道：全部", platform.getServerGBId());
        if (!userSetting.getServerId().equals(platform.getServerId())) {

            int result = redisRpcService.removeAllPlatformChannel(platform.getServerId(), platformId);
            if (result > 0) {
                log.info("[跨平台-取消共享通道] 成功， 平台：{}， 通道：全部", platform.getServerGBId());
            }else {
                log.info("[跨平台-取消共享通道] 失败， 平台：{}， 通道：全部", platform.getServerGBId());
            }
            return result;
        }
        List<CommonGBChannel> channelListShare = platformChannelMapper.queryShare(platformId,  null);
        Assert.notEmpty(channelListShare, "未共享任何通道");
        int result = platformChannelMapper.removeChannelsWithPlatform(platformId, channelListShare);
        if (result > 0) {
            // 查询通道相关的分组信息
            Set<Region> regionSet = regionMapper.queryByChannelList(channelListShare);
            Set<Region> deleteRegion = deleteEmptyRegion(regionSet, platformId);
            if (!deleteRegion.isEmpty()) {
                for (Region region : deleteRegion) {
                    channelListShare.add(0, CommonGBChannel.build(region));
                }
            }

            // 查询通道相关的分组信息
            Set<Group> groupSet = groupMapper.queryByChannelList(channelListShare);
            Set<Group> deleteGroup = deleteEmptyGroup(groupSet, platformId);
            if (!deleteGroup.isEmpty()) {
                for (Group group : deleteGroup) {
                    channelListShare.add(0, CommonGBChannel.build(group));
                }
            }
            // 发送消息
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(platform, channelListShare, CatalogEvent.DEL);
            } catch (Exception e) {
                log.warn("[移除全部关联通道] 发送失败，数量：{}", channelListShare.size(), e);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public void addChannelByDevice(Integer platformId, List<Integer> deviceIds) {
        List<Integer> channelList = commonGBChannelMapper.queryByGbDeviceIdsForIds(ChannelDataType.GB28181, deviceIds);
        addChannels(platformId, channelList);
    }

    @Override
    @Transactional
    public void removeChannelByDevice(Integer platformId, List<Integer> deviceIds) {
        List<Integer> channelList = commonGBChannelMapper.queryByGbDeviceIdsForIds(ChannelDataType.GB28181, deviceIds);
        removeChannels(platformId, channelList);
    }

    @Transactional
    public int removeChannelList(Integer platformId, List<CommonGBChannel> channelList) {
        Platform platform = platformMapper.query(platformId);
        if (platform == null) {
            log.info("[移除关联通道] 平台{}未查询到", platformId);
            return 0;
        }
        String channelDeviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
        log.info("[取消共享通道] 平台：{}， 通道： {}", platform.getServerGBId(), channelDeviceIds);
        if (!userSetting.getServerId().equals(platform.getServerId())) {
            List<Integer> channelIds = channelList.stream().map(CommonGBChannel::getGbId).toList();
            int result = redisRpcService.removePlatformChannelList(platform.getServerId(), new ChannelListForRpcParam(channelIds, platformId));
            if (result > 0) {
                log.info("[跨平台-取消共享通道] 成功， 平台：{}， 通道： {}", platform.getServerGBId(), channelDeviceIds);
            }else {
                log.info("[跨平台-取消共享通道] 失败， 平台：{}， 通道： {}", platform.getServerGBId(), channelDeviceIds);
            }
            return result;
        }
        String deviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
        int result = platformChannelMapper.removeChannelsWithPlatform(platformId, channelList);
        if (result <= 0) {
            log.info("[取消共享通道] 平台{}未关联通道： {}", platformId, deviceIds);
            return 0;
        }
        // 查询通道相关的分组信息
        Set<Region> regionSet = regionMapper.queryByChannelList(channelList);
        Set<Region> deleteRegion = deleteEmptyRegion(regionSet, platformId);
        if (!deleteRegion.isEmpty()) {
            for (Region region : deleteRegion) {
                channelList.add(0, CommonGBChannel.build(region));
            }
        }

        // 查询通道相关的分组信息
        Set<Group> groupSet = groupMapper.queryByChannelList(channelList);
        Set<Group> deleteGroup = deleteEmptyGroup(groupSet, platformId);
        if (!deleteGroup.isEmpty()) {
            for (Group group : deleteGroup) {
                channelList.add(0, CommonGBChannel.build(group));
            }
        }
        // 发送消息
        try {
            // 发送catalog
            eventPublisher.catalogEventPublish(platform, channelList, CatalogEvent.DEL);
        } catch (Exception e) {
            log.warn("[取消共享通道] 发送失败，数量：{}", channelList.size(), e);
        }
        return result;
    }

    @Override
    @Transactional
    public int removeChannels(Integer platformId, List<Integer> channelIds) {
        List<CommonGBChannel> channelList = platformChannelMapper.queryShare(platformId, channelIds);
        if (channelList.isEmpty()) {
            log.info("[移除通道] 通道列表为空");
            return 0;
        }
        return removeChannelList(platformId, channelList);
    }

    @Override
    @Transactional
    public void removeChannels(List<Integer> ids) {
        List<Platform> platformList = platformChannelMapper.queryPlatFormListByChannelList(ids);
        if (platformList.isEmpty()) {
            log.info("[移除多个通道] 未查询到通道关联的平台");
            return;
        }

        for (Platform platform : platformList) {
            removeChannels(platform.getId(), ids);
        }
    }

    @Override
    @Transactional
    public void removeChannel(int channelId) {
        List<Platform> platformList = platformChannelMapper.queryPlatFormListByChannelId(channelId);
        if (platformList.isEmpty()) {
            log.info("[移除多个通道] 未查询到通道：{} 关联的平台", channelId);
            return;
        }
        for (Platform platform : platformList) {
            ArrayList<Integer> ids = new ArrayList<>();
            ids.add(channelId);
            removeChannels(platform.getId(), ids);
        }
    }

    @Override
    public List<CommonGBChannel> queryByPlatform(Platform platform) {
        if (platform == null) {
            log.info("[查询通道所属平台] 平台参数为NULL");
            return null;
        }
        List<CommonGBChannel> commonGBChannelList = commonGBChannelMapper.queryWithPlatform(platform.getId());
        if (commonGBChannelList.isEmpty()) {
            return new ArrayList<>();
        }
        List<CommonGBChannel> channelList = new ArrayList<>();
        // 是否包含平台信息
        if (platform.getCatalogWithPlatform() > 0) {
            CommonGBChannel channel = CommonGBChannel.build(platform);
            channelList.add(channel);
        }
        // 关联的行政区划信息
        if (platform.getCatalogWithRegion() > 0) {
            // 查询关联平台的行政区划信息
            List<CommonGBChannel> regionChannelList = regionMapper.queryByPlatform(platform.getId());
            if (!regionChannelList.isEmpty()) {
                channelList.addAll(regionChannelList);
            }
        }
        if (platform.getCatalogWithGroup() > 0) {
            // 关联的分组信息
            List<CommonGBChannel> groupChannelList =  groupMapper.queryForPlatform(platform.getId());
            if (!groupChannelList.isEmpty()) {
                channelList.addAll(groupChannelList);
            }
        }

        channelList.addAll(commonGBChannelList);
        return channelList;
    }

    @Override
    public void pushChannel(Integer platformId) {
        Platform platform = platformMapper.query(platformId);
        Assert.notNull(platform, "平台不存在");
        if (!userSetting.getServerId().equals(platform.getServerId())) {
            boolean result = redisRpcService.pushPlatformChannel(platform.getServerId(), platformId);
            if (result) {
                log.info("[跨平台-主动推送通道] 成功， 平台：{}", platform.getServerGBId());
            }else {
                log.info("[跨平台-主动推送通道] 失败， 平台：{}", platform.getServerGBId());
            }
            return;
        }

        List<CommonGBChannel> channelList = queryByPlatform(platform);
        if (channelList.isEmpty()){
            log.info("[推送通道] 平台：{} 未查询到通道信息", platform.getServerGBId());
            return;
        }
        SubscribeInfo subscribeInfo = SubscribeInfo.buildSimulated(platform.getServerGBId(), platform.getServerIp());

        try {
            sipCommanderFroPlatform.sendNotifyForCatalogAddOrUpdate(CatalogEvent.ADD, platform, channelList, subscribeInfo, null);
        } catch (InvalidArgumentException | ParseException | NoSuchFieldException |
                 SipException | IllegalAccessException e) {
            log.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
        }
    }

    @Override
    public void updateCustomChannel(PlatformChannel channel) {
        Platform platform = platformMapper.query(channel.getPlatformId());
        Assert.notNull(platform, "平台不存在");
        log.info("[国标级联-自定义共享通道] 平台：{}， 通道：{}", platform.getServerGBId(), channel);
        if (!userSetting.getServerId().equals(platform.getServerId())) {
            boolean result = redisRpcService.updateCustomPlatformChannel(platform.getServerId(), channel);
            if (result) {
                log.info("[国标级联-自定义共享通道] 成功， 平台：{}， 通道：{}", platform.getServerGBId(), channel);
            }else {
                log.info("[国标级联-自定义共享通道] 失败， 平台：{}， 通道：{}", platform.getServerGBId(), channel);
            }
            return;
        }

        platformChannelMapper.updateCustomChannel(channel);

        CommonGBChannel commonGBChannel = platformChannelMapper.queryShareChannel(channel.getPlatformId(), channel.getGbId());
        // 发送消息
        try {
            // 发送catalog
            eventPublisher.catalogEventPublish(platform, commonGBChannel, CatalogEvent.UPDATE);
        } catch (Exception e) {
            log.warn("[国标级联-自定义共享通道] 发送失败， 平台ID： {}， 通道： {}（{}）", channel.getPlatformId(),
                    channel.getGbName(), channel.getId(), e);
        }
    }

    @Override
    @Transactional
    public void checkGroupRemove(List<CommonGBChannel> channelList, List<Group> groupList) {

        List<Integer> channelIds = new ArrayList<>();
        channelList.stream().forEach(commonGBChannel -> {
            channelIds.add(commonGBChannel.getGbId());
        });
        // 获取关联这些通道的平台
        List<Platform> platformList = platformChannelMapper.queryPlatFormListByChannelList(channelIds);
        if (platformList.isEmpty()) {
            String deviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
            log.info("[获取关联这些通道的平台] 未查询到通道关联的平台, 通道如下 {}", deviceIds);
            return;
        }
        for (Platform platform : platformList) {
            Set<Group> groupSet;
            if (groupList == null || groupList.isEmpty()) {
                groupSet = platformChannelMapper.queryShareGroup(platform.getId());
            }else {
                groupSet = new HashSet<>(groupList);
            }
            // 清理空的分组并发送消息
            Set<Group> deleteGroup = deleteEmptyGroup(groupSet, platform.getId());

            List<CommonGBChannel> channelListForEvent = new ArrayList<>();
            if (!deleteGroup.isEmpty()) {
                for (Group group : deleteGroup) {
                    channelListForEvent.add(0, CommonGBChannel.build(group));
                }
            }
            // 发送消息
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(platform, channelListForEvent, CatalogEvent.DEL);
            } catch (Exception e) {
                log.warn("[移除关联通道] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    @Transactional
    public void checkRegionRemove(List<CommonGBChannel> channelList, List<Region> regionList) {
        List<Integer> channelIds = new ArrayList<>();
        channelList.stream().forEach(commonGBChannel -> {
            channelIds.add(commonGBChannel.getGbId());
        });
        // 获取关联这些通道的平台
        List<Platform> platformList = platformChannelMapper.queryPlatFormListByChannelList(channelIds);
        if (platformList.isEmpty()) {
            String deviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
            log.info("[获取关联这些通道的平台] 未查询到通道关联的平台, 通道如下 {}", deviceIds);
            return;
        }
        for (Platform platform : platformList) {
            Set<Region> regionSet;
            if (regionList == null || regionList.isEmpty()) {
                regionSet = platformChannelMapper.queryShareRegion(platform.getId());
            }else {
                regionSet = new HashSet<>(regionList);
            }
            // 清理空的分组并发送消息
            Set<Region> deleteRegion = deleteEmptyRegion(regionSet, platform.getId());

            List<CommonGBChannel> channelListForEvent = new ArrayList<>();
            if (!deleteRegion.isEmpty()) {
                for (Region region : deleteRegion) {
                    channelListForEvent.add(0, CommonGBChannel.build(region));
                }
            }
            // 发送消息
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(platform, channelListForEvent, CatalogEvent.DEL);
            } catch (Exception e) {
                log.warn("[移除关联通道] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    @Transactional
    public void checkGroupAdd(List<CommonGBChannel> channelList) {
        List<Integer> channelIds = new ArrayList<>();
        channelList.stream().forEach(commonGBChannel -> {
            channelIds.add(commonGBChannel.getGbId());
        });
        List<Platform> platformList = platformChannelMapper.queryPlatFormListByChannelList(channelIds);
        if (platformList.isEmpty()) {
            String deviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
            log.info("[获取关联这些通道的平台] 未查询到通道关联的平台, 通道如下 {}", deviceIds);
            return;
        }
        for (Platform platform : platformList) {

            Set<Group> addGroup =  getGroupNotShareByChannelList(channelList, platform.getId());

            List<CommonGBChannel> channelListForEvent = new ArrayList<>();
            if (!addGroup.isEmpty()) {
                for (Group group : addGroup) {
                    channelListForEvent.add(0, CommonGBChannel.build(group));
                }
                platformChannelMapper.addPlatformGroup(addGroup, platform.getId());
                // 发送消息
                try {
                    // 发送catalog
                    eventPublisher.catalogEventPublish(platform, channelListForEvent, CatalogEvent.ADD);
                } catch (Exception e) {
                    log.warn("[移除关联通道] 发送失败，数量：{}", channelList.size(), e);
                }
            }
        }
    }

    @Override
    public void checkRegionAdd(List<CommonGBChannel> channelList) {
        List<Integer> channelIds = new ArrayList<>();
        channelList.stream().forEach(commonGBChannel -> {
            channelIds.add(commonGBChannel.getGbId());
        });
        List<Platform> platformList = platformChannelMapper.queryPlatFormListByChannelList(channelIds);
        if (platformList.isEmpty()) {
            String deviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
            log.info("[获取关联这些通道的平台] 未查询到通道关联的平台, 通道如下 {}", deviceIds);
            return;
        }
        for (Platform platform : platformList) {

            Set<Region> addRegion =  getRegionNotShareByChannelList(channelList, platform.getId());
            List<CommonGBChannel> channelListForEvent = new ArrayList<>();
            if (!addRegion.isEmpty()) {
                for (Region region : addRegion) {
                    channelListForEvent.add(0, CommonGBChannel.build(region));
                }
                platformChannelMapper.addPlatformRegion(new ArrayList<>(addRegion), platform.getId());
                // 发送消息
                try {
                    // 发送catalog
                    eventPublisher.catalogEventPublish(platform, channelListForEvent, CatalogEvent.ADD);
                } catch (Exception e) {
                    log.warn("[移除关联通道] 发送失败，数量：{}", channelList.size(), e);
                }
            }
        }
    }

    @Override
    public List<Platform> queryPlatFormListByChannelDeviceId(Integer channelId, List<String> platforms) {
        return platformChannelMapper.queryPlatFormListForGBWithGBId(channelId, platforms);
    }

    @Override
    public CommonGBChannel queryChannelByPlatformIdAndChannelId(Integer platformId, Integer channelId) {
        return platformChannelMapper.queryShareChannel(platformId, channelId);
    }

    @Override
    public List<CommonGBChannel> queryChannelByPlatformIdAndChannelIds(Integer platformId, List<Integer> channelIds) {
        return platformChannelMapper.queryShare(platformId, channelIds);
    }

    @Override
    public List<Platform> queryByPlatformBySharChannelId(String channelDeviceId) {
        List<CommonGBChannel> commonGBChannels = commonGBChannelMapper.queryByDeviceId(channelDeviceId);
        ArrayList<Integer> ids = new ArrayList<>();
        for (CommonGBChannel commonGBChannel : commonGBChannels) {
            ids.add(commonGBChannel.getGbId());
        }
        return platformChannelMapper.queryPlatFormListByChannelList(ids);
    }
}
