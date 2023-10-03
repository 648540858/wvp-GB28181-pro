package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.conf.CivilCodeFileConf;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Gb28181CodeType;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.jt1078.proc.request.Re;
import com.genersoft.iot.vmp.service.ICommonGbChannelService;
import com.genersoft.iot.vmp.service.bean.Group;
import com.genersoft.iot.vmp.service.bean.CommonGbChannelType;
import com.genersoft.iot.vmp.service.bean.Region;
import com.genersoft.iot.vmp.storager.dao.CommonGbChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.GroupMapper;
import com.genersoft.iot.vmp.storager.dao.RegionMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.*;

@Service
public class CommonGbChannelServiceImpl implements ICommonGbChannelService {

    private final static Logger logger = LoggerFactory.getLogger(CommonGbChannelServiceImpl.class);

    @Autowired
    private CommonGbChannelMapper commonGbChannelMapper;

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    private TransactionDefinition transactionDefinition;

    @Autowired
    private CivilCodeFileConf civilCodeFileConf;


    @Override
    public CommonGbChannel getChannel(String channelId) {
        return commonGbChannelMapper.queryByDeviceID(channelId);
    }

    @Override
    public int add(CommonGbChannel channel) {
        return commonGbChannelMapper.add(channel);
    }

    @Override
    public int addFromGbChannel(DeviceChannel channel) {
        CommonGbChannel commonGbChannel = commonGbChannelMapper.queryByDeviceID(channel.getChannelId());
        logger.info("[添加通用通道]来自国标通道，国标编号: {}, 同步所有字段", channel.getChannelId());
        if (commonGbChannel != null) {
            logger.info("[添加通用通道]来自国标通道，失败，已存在。国标编号: {}", channel.getChannelId());
            return 0;
        }
        CommonGbChannel commonChannelFromDeviceChannel = getCommonChannelFromDeviceChannel(channel, null);
        return commonGbChannelMapper.add(commonChannelFromDeviceChannel);
    }

    @Override
    public int delete(String channelId) {
        return commonGbChannelMapper.deleteByDeviceID(channelId);
    }

    @Override
    public int update(CommonGbChannel channel) {
        return commonGbChannelMapper.update(channel);
    }

    @Override
    public boolean checkChannelInPlatform(String channelId, String platformServerId) {
        return commonGbChannelMapper.checkChannelInPlatform(channelId, platformServerId) > 0;
    }

    @Override
    public boolean syncChannelFromGb28181Device(String gbDeviceId, List<String> syncKeys, Boolean syncGroup, Boolean syncRegion) {
        logger.info("[同步通用通道]来自国标设备，国标编号: {}", gbDeviceId);
        List<DeviceChannel> deviceChannels = deviceChannelMapper.queryAllChannels(gbDeviceId);
        if (deviceChannels.isEmpty()) {
            logger.info("[同步通用通道]来自国标设备，结束， 通道数为0, 国标编号: {}", gbDeviceId);
            return false;
        }
        List<CommonGbChannel> commonGbChannelList = new ArrayList<>();
        // 存储得到的10到13位为215的业务分组数据
        Map<String, Group> businessGroupMap = new HashMap<>();
        // 存储得到的10到13位为216的虚拟组织 数据
        Map<String, Group> virtuallyGroupMap = new HashMap<>();
        // 存储得到的行政区划数据
        Map<String, Region> regionMap = new HashMap<>();
        // 存储得到的所有parentId, 后续检验parentId是否已传输对应的分组/行政区划数据，从而确定是否需要自动创建节点。
        Set<String> parentIdSet = new HashSet<>();
        // 存储得到的所有行政区划, 后续检验civilCode是否已传输对应的行政区划数据，从而确定是否需要自动创建节点。
        Set<String> civilCodeSet = new HashSet<>();
        List<DeviceChannel> clearChannels = new ArrayList<>();
        deviceChannels.stream().forEach(deviceChannel -> {
            if (deviceChannel.getCommonGbChannelId() > 0) {
                clearChannels.add(deviceChannel);
            }
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
                    // 行政区划条目
                    Region region = Region.getInstance(deviceChannel.getChannelId(), deviceChannel.getName(),
                            civilCodeFileConf.getParentCode(deviceChannel.getChannelId()).getCode());
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
                    Group group = Group.getInstance(deviceChannel.getChannelId(), deviceChannel.getName(), deviceChannel.getParentId(), null);
                    virtuallyGroupMap.put(deviceChannel.getChannelId(), group);
                }
            }else {
                if (!StringUtils.isEmpty(deviceChannel.getParentId())) {
                    parentIdSet.add(deviceChannel.getParentId());
                }
                if (!StringUtils.isEmpty(deviceChannel.getCivilCode())) {
                    civilCodeSet.add(deviceChannel.getCivilCode());
                }
                CommonGbChannel commonGbChannel = getCommonChannelFromDeviceChannel(deviceChannel, syncKeys);
                commonGbChannelList.add(commonGbChannel);
            }
        });
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        int limit = 50;
        if (!clearChannels.isEmpty()) {
            if (clearChannels.size() <= limit) {
                commonGbChannelMapper.deleteByDeviceIDs(clearChannels);
            } else {
                for (int i = 0; i < clearChannels.size(); i += limit) {
                    int toIndex = i + limit;
                    if (i + limit > clearChannels.size()) {
                        toIndex = clearChannels.size();
                    }
                    List<DeviceChannel> clearChannelsSun = clearChannels.subList(i, toIndex);
                    int currentResult = commonGbChannelMapper.deleteByDeviceIDs(clearChannelsSun);
                    if (currentResult <= 0) {
                        dataSourceTransactionManager.rollback(transactionStatus);
                        return false;
                    }
                }
            }
        }
        boolean result;
        if (commonGbChannelList.size() <= limit) {
            result = commonGbChannelMapper.addAll(commonGbChannelList) > 0;
        } else {
            for (int i = 0; i < commonGbChannelList.size(); i += limit) {
                int toIndex = i + limit;
                if (i + limit > commonGbChannelList.size()) {
                    toIndex = commonGbChannelList.size();
                }
                List<CommonGbChannel> commonGbChannelListSub = commonGbChannelList.subList(i, toIndex);
                int currentResult = commonGbChannelMapper.addAll(commonGbChannelListSub);
                if (currentResult <= 0) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    logger.info("[同步通用通道]来自国标设备，失败， 写入数据库失败, 国标编号: {}", gbDeviceId);
                    return false;
                }
            }
            result = true;
        }
        deviceChannelMapper.updateCommonChannelId(gbDeviceId);

        // 为虚拟组织数据补充业务分组ID
        if (!virtuallyGroupMap.isEmpty()) {
            for (Group virtuallyGroup : virtuallyGroupMap.values()) {
                String topGroupId = getTopGroupId(businessGroupMap, virtuallyGroupMap,
                        virtuallyGroup.getCommonGroupDeviceId(), 0);
                if (topGroupId == null) {
                    virtuallyGroupMap.remove(virtuallyGroup.getCommonGroupDeviceId());
                }else {
                    virtuallyGroup.setCommonGroupTopId(topGroupId);
                }

            }
        }

        List<String> errorParentIdList = new ArrayList<>();
        // 检测ParentId字段数据是否不完整
        for (String parentId : parentIdSet) {
            Gb28181CodeType channelIdType = SipUtils.getChannelIdType(parentId);
            if (channelIdType == null) {
                logger.warn("[不规范的ParentId设置]parentId不是虚拟组织编号，无法自动添加分组信息。 " +
                        "国标编号: {}， parentId： {}", gbDeviceId, parentId );
                continue;
            }
            if (channelIdType == Gb28181CodeType.CIVIL_CODE_PROVINCE
                    || channelIdType == Gb28181CodeType.CIVIL_CODE_CITY
                    || channelIdType == Gb28181CodeType.CIVIL_CODE_COUNTY
                    || channelIdType == Gb28181CodeType.CIVIL_CODE_GRASS_ROOTS
            ){
                logger.warn("[不规范的ParentId设置]错误的将行政区划编号写入ParentId字段中，尝试纠正。 " +
                        "国标编号: {}， parentId： {}", gbDeviceId, parentId );
                if (!regionMap.containsKey(parentId)) {
                    Region region = civilCodeFileConf.createRegion(parentId);
                    regionMap.put(region.getCommonRegionDeviceId(), region);
                }
            }else if (channelIdType == Gb28181CodeType.BUSINESS_GROUP) {
                logger.warn("[不规范的ParentId设置]错误的将通道的ParentId设置为业务分组，应该放在虚拟组织下，尝试纠正。 " +
                        "国标编号: {}， parentId： {}", gbDeviceId, parentId );
                // 注：纠正的方式为将parentId置空，这样可以在分组列表的<未分组>中找到这些通道，然后进行手动处理，
                //    代码在getCommonChannelFromDeviceChannel中体现，这里只是做个日志提示下
            }else if (channelIdType == Gb28181CodeType.VIRTUAL_ORGANIZATION){
                Group virtuallyGroup = virtuallyGroupMap.get(parentId);
                if (virtuallyGroup == null) {
                    // 如果下级同步的通道不包括这个虚拟组织的信息
                    errorParentIdList.add(parentId);
                }else {
                    String commonGroupTopId = virtuallyGroup.getCommonGroupTopId();
                    // 如果下级同步的通道包括这个虚拟组织的信息， 但是没有对应的业务分组的信息
                    if (!businessGroupMap.containsKey(commonGroupTopId)) {
                        errorParentIdList.add(parentId);
                    }
                }
            }
        }
        // 处理存在错误的parentId
        if (!errorParentIdList.isEmpty()) {
            if (errorParentIdList.size() <= limit) {
                if (commonGbChannelMapper.clearParentIds(errorParentIdList) <= 0) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    logger.info("[同步通用通道]来自国标设备，失败， 处理错误的ParentId失败, 国标编号: {}", gbDeviceId);
                    return false;
                }
            } else {
                for (int i = 0; i < errorParentIdList.size(); i += limit) {
                    int toIndex = i + limit;
                    if (i + limit > errorParentIdList.size()) {
                        toIndex = errorParentIdList.size();
                    }
                    List<String> errorParentIdListSub = errorParentIdList.subList(i, toIndex);
                    if (commonGbChannelMapper.clearParentIds(errorParentIdListSub) <= 0) {
                        dataSourceTransactionManager.rollback(transactionStatus);
                        logger.info("[同步通用通道]来自国标设备，失败， 处理错误的ParentId失败, 国标编号: {}", gbDeviceId);
                        return false;
                    }
                }
            }
        }
        // 分组信息写入数据库
        List<Group> allGroup = new ArrayList<>(businessGroupMap.values());
        allGroup.addAll(virtuallyGroupMap.values());
        if (allGroup.size() <= limit) {
            if (groupMapper.addAll(allGroup) <= 0) {
                dataSourceTransactionManager.rollback(transactionStatus);
                logger.info("[同步通用通道]来自国标设备，失败，添加分组信息失败, 国标编号: {}", gbDeviceId);
                return false;
            }
        } else {
            for (int i = 0; i < allGroup.size(); i += limit) {
                int toIndex = i + limit;
                if (i + limit > allGroup.size()) {
                    toIndex = allGroup.size();
                }
                List<Group> allGroupSub = allGroup.subList(i, toIndex);
                if (groupMapper.addAll(allGroupSub) <= 0) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    logger.info("[同步通用通道]来自国标设备，失败，添加分组信息失败, 国标编号: {}", gbDeviceId);
                    return false;
                }
            }
        }

        // 检测行政区划信息是否完整
        for (String civilCode : civilCodeSet) {
            if (!regionMap.containsKey(civilCode)) {
                logger.warn("[通道信息中缺少地区信息]补充地区信息 国标编号: {}， civilCode： {}", gbDeviceId, civilCode );
                Region region = civilCodeFileConf.createRegion(civilCode);
                regionMap.put(region.getCommonRegionDeviceId(), region);
            }
        }
        // 行政区划信息写入数据库
        List<Region> allRegion = new ArrayList<>(regionMap.values());
        if (!allRegion.isEmpty()) {
            if (allRegion.size() <= limit) {
                if (regionMapper.addAll(allRegion) <= 0) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    logger.info("[同步通用通道]来自国标设备，失败，添加行政区划信息失败, 国标编号: {}", gbDeviceId);
                    return false;
                }
            } else {
                for (int i = 0; i < allRegion.size(); i += limit) {
                    int toIndex = i + limit;
                    if (i + limit > allRegion.size()) {
                        toIndex = allRegion.size();
                    }
                    List<Region> allRegionSub = allRegion.subList(i, toIndex);
                    if (regionMapper.addAll(allRegionSub) <= 0) {
                        dataSourceTransactionManager.rollback(transactionStatus);
                        logger.info("[同步通用通道]来自国标设备，失败，添加行政区划信息失败, 国标编号: {}", gbDeviceId);
                        return false;
                    }
                }
            }
        }
        dataSourceTransactionManager.commit(transactionStatus);
        return result;
    }

    private String getTopGroupId(Map<String, Group> businessGroupMap, Map<String, Group> virtuallyGroupMap, String commonGroupId, int depth) {
        if (depth >= 16) {
            return null;
        }
        Group group = virtuallyGroupMap.get(commonGroupId);
        if (group == null) {
            return null;
        }
        Gb28181CodeType channelIdType = SipUtils.getChannelIdType(group.getCommonGroupParentId());
        if (channelIdType == Gb28181CodeType.BUSINESS_GROUP) {
            if (businessGroupMap.containsKey(group.getCommonGroupParentId())) {
                return group.getCommonGroupParentId();
            }else {
                return null;
            }
        }
        depth ++;
        return getTopGroupId(businessGroupMap, virtuallyGroupMap, group.getCommonGroupParentId(), depth);
    }

    @Override
    public CommonGbChannel getCommonChannelFromDeviceChannel(DeviceChannel deviceChannel, List<String> syncKeys) {
        if (deviceChannel == null) {
            return null;
        }
        CommonGbChannel commonGbChannel = new CommonGbChannel();
        commonGbChannel.setCommonGbDeviceID(deviceChannel.getChannelId());
        commonGbChannel.setCommonGbStatus(deviceChannel.isStatus());
        commonGbChannel.setType(CommonGbChannelType.GB28181);
        commonGbChannel.setCreateTime(DateUtil.getNow());
        commonGbChannel.setUpdateTime(DateUtil.getNow());
        if (syncKeys == null || syncKeys.isEmpty()) {
            commonGbChannel.setCommonGbName(deviceChannel.getName());
            commonGbChannel.setCommonGbManufacturer(deviceChannel.getManufacture());
            commonGbChannel.setCommonGbModel(deviceChannel.getModel());
            commonGbChannel.setCommonGbOwner(deviceChannel.getOwner());
            Gb28181CodeType channelIdType = SipUtils.getChannelIdType(deviceChannel.getCivilCode());
            if (channelIdType == Gb28181CodeType.CIVIL_CODE_PROVINCE
                        || channelIdType == Gb28181CodeType.CIVIL_CODE_CITY
                        || channelIdType == Gb28181CodeType.CIVIL_CODE_COUNTY
                        || channelIdType == Gb28181CodeType.CIVIL_CODE_GRASS_ROOTS
            ){
                commonGbChannel.setCommonGbCivilCode(deviceChannel.getCivilCode());
            }else {
                logger.warn("[不规范的CivilCode]，deviceId: {}, channel: {}, civilCode: {}",
                        deviceChannel.getDeviceId(),
                        deviceChannel.getChannelId(),
                        deviceChannel.getCivilCode());
            }

            commonGbChannel.setCommonGbCivilCode(deviceChannel.getCivilCode());
            commonGbChannel.setCommonGbBlock(deviceChannel.getBlock());
            commonGbChannel.setCommonGbAddress(deviceChannel.getAddress());
            commonGbChannel.setCommonGbParental(0);
            // 不符合国标的parentId，可以在未分组中找到并重新设置分组信息
            Gb28181CodeType parentIdIdType = SipUtils.getChannelIdType(deviceChannel.getParentId());
            if (parentIdIdType == Gb28181CodeType.VIRTUAL_ORGANIZATION) {
                commonGbChannel.setCommonGbParentID(deviceChannel.getParentId());
            }

            commonGbChannel.setCommonGbSafetyWay(deviceChannel.getSafetyWay());
            commonGbChannel.setCommonGbRegisterWay(deviceChannel.getRegisterWay());
            commonGbChannel.setCommonGbCertNum(deviceChannel.getCertNum());
            commonGbChannel.setCommonGbCertifiable(deviceChannel.getCertifiable());
            commonGbChannel.setCommonGbErrCode(deviceChannel.getErrCode());
            commonGbChannel.setCommonGbEndTime(deviceChannel.getEndTime());
            if (NumberUtils.isParsable(deviceChannel.getSecrecy())) {
                commonGbChannel.setCommonGbSecrecy(Integer.parseInt(deviceChannel.getSecrecy()));
            }
            commonGbChannel.setCommonGbIPAddress(deviceChannel.getIpAddress());
            commonGbChannel.setCommonGbPort(deviceChannel.getPort());
            commonGbChannel.setCommonGbPassword(deviceChannel.getPassword());
            commonGbChannel.setCommonGbLongitude(deviceChannel.getLongitude());
            commonGbChannel.setCommonGbLatitude(deviceChannel.getLatitude());
            commonGbChannel.setCommonGbPtzType(deviceChannel.getPTZType());
//            commonGbChannel.setCommonGbPositionType(deviceChannel.getCommonGbPositionType());
            commonGbChannel.setCommonGbBusinessGroupID(deviceChannel.getBusinessGroupId());
        } else {
            for (String key : syncKeys) {
                switch (key) {
                    case "commonGbName":
                        commonGbChannel.setCommonGbName(deviceChannel.getName());
                        break;
                    case "commonGbManufacturer":
                        commonGbChannel.setCommonGbManufacturer(deviceChannel.getManufacture());
                        break;
                    case "commonGbModel":
                        commonGbChannel.setCommonGbModel(deviceChannel.getModel());
                        break;
                    case "commonGbOwner":
                        commonGbChannel.setCommonGbOwner(deviceChannel.getOwner());
                        break;
                    case "commonGbCivilCode":
                        Gb28181CodeType channelIdType = SipUtils.getChannelIdType(deviceChannel.getCivilCode());
                        if (channelIdType == Gb28181CodeType.CIVIL_CODE_PROVINCE
                                || channelIdType == Gb28181CodeType.CIVIL_CODE_CITY
                                || channelIdType == Gb28181CodeType.CIVIL_CODE_COUNTY
                                || channelIdType == Gb28181CodeType.CIVIL_CODE_GRASS_ROOTS
                        ){
                            commonGbChannel.setCommonGbCivilCode(deviceChannel.getCivilCode());
                        }else {
                            logger.warn("[不规范的CivilCode]，deviceId: {}, channel: {}, civilCode: {}",
                                    deviceChannel.getDeviceId(),
                                    deviceChannel.getChannelId(),
                                    deviceChannel.getCivilCode());
                        }
                        commonGbChannel.setCommonGbCivilCode(deviceChannel.getCivilCode());
                        break;
                    case "commonGbBlock":
                        commonGbChannel.setCommonGbBlock(deviceChannel.getBlock());
                        break;
                    case "commonGbAddress":
                        commonGbChannel.setCommonGbAddress(deviceChannel.getAddress());
                        break;
                    case "commonGbParental":
                        commonGbChannel.setCommonGbParental(deviceChannel.getParental());
                        break;
                    case "commonGbParentID":
                        commonGbChannel.setCommonGbParentID(deviceChannel.getParentId());
                        break;
                    case "commonGbSafetyWay":
                        commonGbChannel.setCommonGbSafetyWay(deviceChannel.getSafetyWay());
                        break;
                    case "commonGbRegisterWay":
                        commonGbChannel.setCommonGbRegisterWay(deviceChannel.getRegisterWay());
                        break;
                    case "commonGbCertNum":
                        commonGbChannel.setCommonGbCertNum(deviceChannel.getCertNum());
                        break;
                    case "commonGbCertifiable":
                        commonGbChannel.setCommonGbCertifiable(deviceChannel.getCertifiable());
                        break;
                    case "commonGbErrCode":
                        commonGbChannel.setCommonGbErrCode(deviceChannel.getErrCode());
                        break;
                    case "commonGbEndTime":
                        commonGbChannel.setCommonGbEndTime(deviceChannel.getEndTime());
                        break;
                    case "commonGbSecrecy":
                        if (NumberUtils.isParsable(deviceChannel.getSecrecy())) {
                            commonGbChannel.setCommonGbSecrecy(Integer.parseInt(deviceChannel.getSecrecy()));
                        }
                        break;
                    case "commonGbIPAddress":
                        commonGbChannel.setCommonGbIPAddress(deviceChannel.getIpAddress());
                        break;
                    case "commonGbPort":
                        commonGbChannel.setCommonGbPort(deviceChannel.getPort());
                        break;
                    case "commonGbPassword":
                        commonGbChannel.setCommonGbPassword(deviceChannel.getPassword());
                        break;
                    case "commonGbLongitude":
                        commonGbChannel.setCommonGbLongitude(deviceChannel.getLongitude());
                        break;
                    case "commonGbLatitude":
                        commonGbChannel.setCommonGbLatitude(deviceChannel.getLatitude());
                        break;
                    case "commonGbPtzType":
                        commonGbChannel.setCommonGbPtzType(deviceChannel.getPTZType());
                        break;
                    case "commonGbPositionType":
//                        commonGbChannel.setCommonGbPositionType(deviceChannel.getCommonGbPositionType());
                        break;
                    case "commonGbRoomType":
                        break;
                    case "commonGbUseType":
                        break;
                    case "commonGbSupplyLightType":
                        break;
                    case "commonGbDirectionType":
                        break;
                    case "commonGbResolution":
                        break;
                    case "commonGbBusinessGroupID":
                        commonGbChannel.setCommonGbBusinessGroupID(deviceChannel.getBusinessGroupId());
                        break;
                    case "commonGbDownloadSpeed":
                        break;
                    case "commonGbSVCTimeSupportMode":
                        break;

                }
            }
        }

        return commonGbChannel;
    }

    @Override
    public List<CommonGbChannel> getChannelsInRegion(String civilCode) {
        return null;
    }

    @Override
    public List<CommonGbChannel> getChannelsInBusinessGroup(String businessGroupID) {
        return null;
    }

    @Override
    public void updateChannelFromGb28181DeviceInList(Device device, List<DeviceChannel> deviceChannels) {

    }

    @Override
    public void addChannelFromGb28181DeviceInList(Device device, List<DeviceChannel> deviceChannels) {

    }

    @Override
    public void deleteGbChannelsFromList(List<DeviceChannel> channelList) {
        commonGbChannelMapper.deleteByDeviceIDs(channelList);
    }

    @Override
    public void channelsOnlineFromList(List<DeviceChannel> channelList) {
        commonGbChannelMapper.channelsOnlineFromList(channelList);
    }

    @Override
    public void channelsOfflineFromList(List<DeviceChannel> channelList) {
        commonGbChannelMapper.channelsOfflineFromList(channelList);
    }
}
