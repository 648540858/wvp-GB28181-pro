package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.GroupMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.RegionMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.sip.message.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class GbChannelServiceImpl implements IGbChannelService {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private CommonGBChannelMapper commonGBChannelMapper;

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Override
    public CommonGBChannel queryByDeviceId(String gbDeviceId) {
        return commonGBChannelMapper.queryByDeviceId(gbDeviceId);
    }

    @Override
    public int add(CommonGBChannel commonGBChannel) {
        if (commonGBChannel.getDataType() == null || commonGBChannel.getDataDeviceId() == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "缺少通道数据类型或通道数据关联设备ID");
        }
        CommonGBChannel commonGBChannelInDb =  commonGBChannelMapper.queryByDataId(commonGBChannel.getDataType(), commonGBChannel.getDataDeviceId());
        if (commonGBChannelInDb != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "此推流已经关联通道");
        }
        commonGBChannel.setCreateTime(DateUtil.getNow());
        commonGBChannel.setUpdateTime(DateUtil.getNow());
        return commonGBChannelMapper.insert(commonGBChannel);
    }

    @Override
    @Transactional
    public int delete(int gbId) {
        // 移除国标级联关联的信息
        try {
            platformChannelService.removeChannel(gbId);
        }catch (Exception e) {
            log.error("[移除通道国标级联共享失败]", e);
        }

        CommonGBChannel channel = commonGBChannelMapper.queryById(gbId);
        if (channel != null) {
            commonGBChannelMapper.delete(gbId);
            try {
                // 发送通知
                eventPublisher.catalogEventPublish(null, channel, CatalogEvent.DEL);
            } catch (Exception e) {
                log.warn("[通道移除通知] 发送失败，{}", channel.getGbDeviceId(), e);
            }
        }
        return 1;
    }

    @Override
    @Transactional
    public void delete(Collection<Integer> ids) {
        // 移除国标级联关联的信息
        try {
            platformChannelService.removeChannels(new ArrayList<>(ids));
        }catch (Exception e) {
            log.error("[移除通道国标级联共享失败]", e);
        }
        List<CommonGBChannel> channelListInDb = commonGBChannelMapper.queryByIds(ids);
        if (channelListInDb.isEmpty()) {
            return;
        }
        commonGBChannelMapper.batchDelete(channelListInDb);
    }

    @Override
    public int update(CommonGBChannel commonGBChannel) {
        log.info("[更新通道] 通道ID: {}, ", commonGBChannel.getGbId());
        if (commonGBChannel.getGbId() <= 0) {
            log.warn("[更新通道] 未找到数据库ID，更新失败， {}({})", commonGBChannel.getGbName(), commonGBChannel.getGbDeviceId());
            return 0;
        }
        commonGBChannel.setUpdateTime(DateUtil.getNow());
        int result = commonGBChannelMapper.update(commonGBChannel);
        if (result > 0) {
            try {
                // 发送通知
                eventPublisher.catalogEventPublish(null, commonGBChannel, CatalogEvent.UPDATE);
            } catch (Exception e) {
                log.warn("[更新通道通知] 发送失败，{}", commonGBChannel.getGbDeviceId(), e);
            }
        }
        return result;
    }

    @Override
    public int offline(CommonGBChannel commonGBChannel) {
        if (commonGBChannel.getGbId() <= 0) {
            log.warn("[通道离线] 未找到数据库ID，更新失败， {}({})", commonGBChannel.getGbName(), commonGBChannel.getGbDeviceId());
            return 0;
        }
        int result = commonGBChannelMapper.updateStatusById(commonGBChannel.getGbId(), "OFF");
        if (result > 0) {
            try {
                // 发送通知
                eventPublisher.catalogEventPublish(null, commonGBChannel, CatalogEvent.OFF);
            } catch (Exception e) {
                log.warn("[通道离线通知] 发送失败，{}", commonGBChannel.getGbDeviceId(), e);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public int offline(List<CommonGBChannel> commonGBChannelList) {
        if (commonGBChannelList.isEmpty()) {
            log.warn("[多个通道离线] 通道数量为0，更新失败");
            return 0;
        }
        List<CommonGBChannel> onlineChannelList = commonGBChannelMapper.queryInListByStatus(commonGBChannelList, "ON");
        if (onlineChannelList.isEmpty()) {
            log.info("[多个通道离线] 更新失败, 参数内通道已经离线, 无需更新");
            return 0;
        }
        int limitCount = 1000;
        int result = 0;
        if (onlineChannelList.size() > limitCount) {
            for (int i = 0; i < onlineChannelList.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > onlineChannelList.size()) {
                    toIndex = onlineChannelList.size();
                }
                result += commonGBChannelMapper.updateStatusForListById(onlineChannelList.subList(i, toIndex), "OFF");
            }
        } else {
            result += commonGBChannelMapper.updateStatusForListById(onlineChannelList, "OFF");
        }
        if (result > 0) {
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(null, onlineChannelList, CatalogEvent.OFF);
            } catch (Exception e) {
                log.warn("[多个通道离线] 发送失败，数量：{}", onlineChannelList.size(), e);
            }
        }
        return result;
    }

    @Override
    public int online(CommonGBChannel commonGBChannel) {
        if (commonGBChannel.getGbId() <= 0) {
            log.warn("[通道上线] 未找到数据库ID，更新失败， {}({})", commonGBChannel.getGbName(), commonGBChannel.getGbDeviceId());
            return 0;
        }
        int result = commonGBChannelMapper.updateStatusById(commonGBChannel.getGbId(), "ON");
        if (result > 0) {
            try {
                // 发送通知
                eventPublisher.catalogEventPublish(null, commonGBChannel, CatalogEvent.ON);
            } catch (Exception e) {
                log.warn("[通道上线通知] 发送失败，{}", commonGBChannel.getGbDeviceId(), e);
            }
        }
        return 0;
    }

    @Override
    @Transactional
    public int online(List<CommonGBChannel> commonGBChannelList) {
        if (commonGBChannelList.isEmpty()) {
            log.warn("[多个通道上线] 通道数量为0，更新失败");
            return 0;
        }
        List<CommonGBChannel> offlineChannelList = commonGBChannelMapper.queryInListByStatus(commonGBChannelList, "OFF");
        if (offlineChannelList.isEmpty()) {
            log.warn("[多个通道上线] 更新失败, 参数内通道已经上线");
            return 0;
        }
        // 批量更新
        int limitCount = 1000;
        int result = 0;
        if (offlineChannelList.size() > limitCount) {
            for (int i = 0; i < offlineChannelList.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > offlineChannelList.size()) {
                    toIndex = offlineChannelList.size();
                }
                result += commonGBChannelMapper.updateStatusForListById(offlineChannelList.subList(i, toIndex), "ON");
            }
        } else {
            result += commonGBChannelMapper.updateStatusForListById(offlineChannelList, "ON");
        }
        if (result > 0) {
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(null, offlineChannelList, CatalogEvent.ON);
            } catch (Exception e) {
                log.warn("[多个通道上线] 发送失败，数量：{}", offlineChannelList.size(), e);
            }
        }

        return result;
    }

    @Override
    @Transactional
    public void batchAdd(List<CommonGBChannel> commonGBChannels) {
        if (commonGBChannels.isEmpty()) {
            log.warn("[新增多个通道] 通道数量为0，更新失败");
            return;
        }
        // 批量保存
        int limitCount = 1000;
        int result = 0;
        if (commonGBChannels.size() > limitCount) {
            for (int i = 0; i < commonGBChannels.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > commonGBChannels.size()) {
                    toIndex = commonGBChannels.size();
                }
                result += commonGBChannelMapper.batchAdd(commonGBChannels.subList(i, toIndex));
            }
        } else {
            result += commonGBChannelMapper.batchAdd(commonGBChannels);
        }
        log.warn("[新增多个通道] 通道数量为{}，成功保存：{}", commonGBChannels.size(), result);
    }

    @Override
    public void batchUpdate(List<CommonGBChannel> commonGBChannels) {
        if (commonGBChannels.isEmpty()) {
            log.warn("[更新多个通道] 通道数量为0，更新失败");
            return;
        }
        // 批量保存
        int limitCount = 1000;
        int result = 0;
        if (commonGBChannels.size() > limitCount) {
            for (int i = 0; i < commonGBChannels.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > commonGBChannels.size()) {
                    toIndex = commonGBChannels.size();
                }
                result += commonGBChannelMapper.batchUpdate(commonGBChannels.subList(i, toIndex));
            }
        } else {
            result += commonGBChannelMapper.batchUpdate(commonGBChannels);
        }
        log.info("[更新多个通道] 通道数量为{}，成功保存：{}", commonGBChannels.size(), result);
        // 发送通过更新通知
        try {
            // 发送通知
            eventPublisher.catalogEventPublish(null, commonGBChannels, CatalogEvent.UPDATE);
        } catch (Exception e) {
            log.warn("[更新多个通道] 发送失败，{}个", commonGBChannels.size(), e);
        }
    }

    @Override
    @Transactional
    public void updateStatus(List<CommonGBChannel> commonGBChannels) {
        if (commonGBChannels.isEmpty()) {
            log.warn("[更新多个通道状态] 通道数量为0，更新失败");
            return;
        }
        int limitCount = 1000;
        int result = 0;
        if (commonGBChannels.size() > limitCount) {
            for (int i = 0; i < commonGBChannels.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > commonGBChannels.size()) {
                    toIndex = commonGBChannels.size();
                }
                result += commonGBChannelMapper.updateStatus(commonGBChannels.subList(i, toIndex));
            }
        } else {
            result += commonGBChannelMapper.updateStatus(commonGBChannels);
        }
        log.warn("[更新多个通道状态] 通道数量为{}，成功保存：{}", commonGBChannels.size(), result);
        // 发送通过更新通知
        try {
            // 发送通知
            eventPublisher.catalogEventPublish(null, commonGBChannels, CatalogEvent.UPDATE);
        } catch (Exception e) {
            log.warn("[更新多个通道] 发送失败，{}个", commonGBChannels.size(), e);
        }
    }



    @Override
    public CommonGBChannel getOne(int id) {
        return commonGBChannelMapper.queryById(id);
    }

    @Override
    public List<IndustryCodeType> getIndustryCodeList() {
        IndustryCodeTypeEnum[] values = IndustryCodeTypeEnum.values();
        List<IndustryCodeType> result = new ArrayList<>(values.length);
        for (IndustryCodeTypeEnum value : values) {
            result.add(IndustryCodeType.getInstance(value));
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public List<DeviceType> getDeviceTypeList() {
        DeviceTypeEnum[] values = DeviceTypeEnum.values();
        List<DeviceType> result = new ArrayList<>(values.length);
        for (DeviceTypeEnum value : values) {
            result.add(DeviceType.getInstance(value));
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public List<NetworkIdentificationType> getNetworkIdentificationTypeList() {
        NetworkIdentificationTypeEnum[] values = NetworkIdentificationTypeEnum.values();
        List<NetworkIdentificationType> result = new ArrayList<>(values.length);
        for (NetworkIdentificationTypeEnum value : values) {
            result.add(NetworkIdentificationType.getInstance(value));
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public void reset(int id) {
        log.info("[重置国标通道] id: {}", id);
        CommonGBChannel channel = getOne(id);
        if (channel == null) {
            log.warn("[重置国标通道] 未找到对应Id的通道: id: {}", id);
            throw new ControllerException(ErrorCode.ERROR400);
        }
        if (channel.getDataType() != ChannelDataType.GB28181.value) {
            log.warn("[重置国标通道] 非国标下级通道无法重置: id: {}", id);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "非国标下级通道无法重置");
        }
        // 这个多加一个参数,为了防止将非国标的通道通过此方法清空内容,导致意外发生
        commonGBChannelMapper.reset(id, ChannelDataType.GB28181.value, channel.getDataDeviceId(), DateUtil.getNow());
        CommonGBChannel channelNew = getOne(id);
        // 发送通过更新通知
        try {
            // 发送通知
            eventPublisher.catalogEventPublish(null, channelNew, CatalogEvent.UPDATE);
        } catch (Exception e) {
            log.warn("[通道移除通知] 发送失败，{}", channelNew.getGbDeviceId(), e);
        }
    }

    @Override
    public PageInfo<CommonGBChannel> queryListByCivilCode(int page, int count, String query, Boolean online, Integer channelType, String civilCode) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<CommonGBChannel> all = commonGBChannelMapper.queryListByCivilCode(query, online, channelType, civilCode);
        return new PageInfo<>(all);
    }

    @Override
    public PageInfo<CommonGBChannel> queryListByParentId(int page, int count, String query, Boolean online, Integer channelType, String groupDeviceId) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<CommonGBChannel> all = commonGBChannelMapper.queryListByParentId(query, online, channelType, groupDeviceId);
        return new PageInfo<>(all);
    }

    @Override
    public void removeCivilCode(List<Region> allChildren) {
        commonGBChannelMapper.removeCivilCode(allChildren);
        // TODO 是否需要通知上级, 或者等添加新的行政区划时发送更新通知

    }

    @Override
    public void addChannelToRegion(String civilCode, List<Integer> channelIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByIds(channelIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        for (CommonGBChannel channel : channelList) {
            channel.setGbCivilCode(civilCode);
        }
        int result = commonGBChannelMapper.updateRegion(civilCode, channelList);
        // 发送通知
        if (result > 0) {
            platformChannelService.checkRegionAdd(channelList);
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(null, channelList, CatalogEvent.UPDATE);
            } catch (Exception e) {
                log.warn("[多个通道添加行政区划] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    @Transactional
    public void deleteChannelToRegion(String civilCode, List<Integer> channelIds) {
        if (!ObjectUtils.isEmpty(civilCode)) {
            deleteChannelToRegionByCivilCode(civilCode);
        }
        if (!ObjectUtils.isEmpty(channelIds)) {
            deleteChannelToRegionByChannelIds(channelIds);
        }
    }

    @Override
    public void deleteChannelToRegionByCivilCode(String civilCode) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByCivilCode(civilCode);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        int result = commonGBChannelMapper.removeCivilCodeByChannels(channelList);
        Region region = regionMapper.queryByDeviceId(civilCode);
        if (region == null) {
            platformChannelService.checkRegionRemove(channelList, null);
        }else {
            List<Region> regionList = new ArrayList<>();
            regionList.add(region);
            platformChannelService.checkRegionRemove(channelList, regionList);
        }
        // TODO 发送通知
//        if (result > 0) {
//            try {
//                // 发送catalog
//                eventPublisher.catalogEventPublish(null, channelList, CatalogEvent.UPDATE);
//            }catch (Exception e) {
//                log.warn("[多个通道添加行政区划] 发送失败，数量：{}", channelList.size(), e);
//            }
//        }
    }

    @Override
    public void deleteChannelToRegionByChannelIds(List<Integer> channelIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByIds(channelIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        int result = commonGBChannelMapper.removeCivilCodeByChannels(channelList);

        platformChannelService.checkRegionRemove(channelList, null);
        // TODO 发送通知
//        if (result > 0) {
//            try {
//                // 发送catalog
//                eventPublisher.catalogEventPublish(null, channelList, CatalogEvent.UPDATE);
//            }catch (Exception e) {
//                log.warn("[多个通道添加行政区划] 发送失败，数量：{}", channelList.size(), e);
//            }
//        }
    }

    @Override
    public void addChannelToRegionByGbDevice(String civilCode, List<Integer> deviceIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByGbDeviceIds(ChannelDataType.GB28181.value, deviceIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        for (CommonGBChannel channel : channelList) {
            channel.setGbCivilCode(civilCode);
        }
        int result = commonGBChannelMapper.updateRegion(civilCode, channelList);
        // 发送通知
        if (result > 0) {
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(null, channelList, CatalogEvent.UPDATE);
            } catch (Exception e) {
                log.warn("[多个通道添加行政区划] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    public void deleteChannelToRegionByGbDevice(List<Integer> deviceIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByGbDeviceIds(ChannelDataType.GB28181.value, deviceIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        int result = commonGBChannelMapper.removeCivilCodeByChannels(channelList);
        platformChannelService.checkRegionRemove(channelList, null);
    }

    @Override
    @Transactional
    public void removeParentIdByBusinessGroup(String businessGroup) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByBusinessGroup(businessGroup);
        if (channelList.isEmpty()) {
            return;
        }
        int result = commonGBChannelMapper.removeParentIdByChannels(channelList);
        List<Group> groupList = groupMapper.queryByBusinessGroup(businessGroup);
        platformChannelService.checkGroupRemove(channelList, groupList);

    }

    @Override
    public void removeParentIdByGroupList(List<Group> groupList) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByGroupList(groupList);
        if (channelList.isEmpty()) {
            return;
        }
        commonGBChannelMapper.removeParentIdByChannels(channelList);
        platformChannelService.checkGroupRemove(channelList, groupList);
    }

    @Override
    public void updateBusinessGroup(String oldBusinessGroup, String newBusinessGroup) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByBusinessGroup(oldBusinessGroup);
        Assert.notEmpty(channelList, "旧的业务分组的通道不存在");

        int result = commonGBChannelMapper.updateBusinessGroupByChannelList(newBusinessGroup, channelList);
        if (result > 0) {
            for (CommonGBChannel channel : channelList) {
                channel.setGbBusinessGroupId(newBusinessGroup);
            }
            // 发送catalog
            try {
                eventPublisher.catalogEventPublish(null, channelList, CatalogEvent.UPDATE);
            } catch (Exception e) {
                log.warn("[多个通道业务分组] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    public void updateParentIdGroup(String oldParentId, String newParentId) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByParentId(oldParentId);
        if (channelList.isEmpty()) {
            return;
        }

        int result = commonGBChannelMapper.updateParentIdByChannelList(newParentId, channelList);
        if (result > 0) {
            for (CommonGBChannel channel : channelList) {
                channel.setGbParentId(newParentId);
            }
            // 发送catalog
            try {
                eventPublisher.catalogEventPublish(null, channelList, CatalogEvent.UPDATE);
            } catch (Exception e) {
                log.warn("[多个通道业务分组] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    @Transactional
    public void addChannelToGroup(String parentId, String businessGroup, List<Integer> channelIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByIds(channelIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        int result = commonGBChannelMapper.updateGroup(parentId, businessGroup, channelList);
        for (CommonGBChannel commonGBChannel : channelList) {
            commonGBChannel.setGbParentId(parentId);
            commonGBChannel.setGbBusinessGroupId(businessGroup);
        }

        // 发送通知
        if (result > 0) {
            platformChannelService.checkGroupAdd(channelList);
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(null, channelList, CatalogEvent.UPDATE);
            } catch (Exception e) {
                log.warn("[多个通道添加行政区划] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    public void deleteChannelToGroup(String parentId, String businessGroup, List<Integer> channelIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByIds(channelIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        commonGBChannelMapper.removeParentIdByChannels(channelList);

        Group group = groupMapper.queryOneByDeviceId(parentId, businessGroup);
        if (group == null) {
            platformChannelService.checkGroupRemove(channelList, null);
        }else {
            List<Group> groupList = new ArrayList<>();
            groupList.add(group);
            platformChannelService.checkGroupRemove(channelList, groupList);
        }
    }

    @Override
    @Transactional
    public void addChannelToGroupByGbDevice(String parentId, String businessGroup, List<Integer> deviceIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByGbDeviceIds(ChannelDataType.GB28181.value, deviceIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        for (CommonGBChannel channel : channelList) {
            channel.setGbParentId(parentId);
            channel.setGbBusinessGroupId(businessGroup);
        }
        int result = commonGBChannelMapper.updateGroup(parentId, businessGroup, channelList);

        for (CommonGBChannel commonGBChannel : channelList) {
            commonGBChannel.setGbParentId(parentId);
            commonGBChannel.setGbBusinessGroupId(businessGroup);
        }
        // 发送通知
        if (result > 0) {
            platformChannelService.checkGroupAdd(channelList);
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(null, channelList, CatalogEvent.UPDATE);
            } catch (Exception e) {
                log.warn("[多个通道添加行政区划] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    public void deleteChannelToGroupByGbDevice(List<Integer> deviceIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByGbDeviceIds(ChannelDataType.GB28181.value, deviceIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        commonGBChannelMapper.removeParentIdByChannels(channelList);
        platformChannelService.checkGroupRemove(channelList, null);
    }

    @Override
    public CommonGBChannel queryOneWithPlatform(Integer platformId, String channelDeviceId) {
        // 防止共享的通道编号重复
        List<CommonGBChannel> channelList = platformChannelMapper.queryOneWithPlatform(platformId, channelDeviceId);
        if (!channelList.isEmpty()) {
            return channelList.get(channelList.size() - 1);
        }else {
            return null;
        }
    }

    @Override
    public void updateCivilCode(String oldCivilCode, String newCivilCode) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByCivilCode(oldCivilCode);
        if (channelList.isEmpty()) {
            return;
        }

        int result = commonGBChannelMapper.updateCivilCodeByChannelList(newCivilCode, channelList);
        if (result > 0) {
            for (CommonGBChannel channel : channelList) {
                channel.setGbCivilCode(newCivilCode);
            }
            // 发送catalog
            try {
                eventPublisher.catalogEventPublish(null, channelList, CatalogEvent.UPDATE);
            } catch (Exception e) {
                log.warn("[多个通道业务分组] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    public List<CommonGBChannel> queryListByStreamPushList(List<StreamPush> streamPushList) {
        return commonGBChannelMapper.queryListByStreamPushList(ChannelDataType.STREAM_PUSH.value, streamPushList);
    }

    @Override
    public PageInfo<CommonGBChannel> queryList(int page, int count, String query, Boolean online, Boolean hasRecordPlan, Integer channelType) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<CommonGBChannel> all = commonGBChannelMapper.queryList(query, online,  hasRecordPlan, channelType);
        return new PageInfo<>(all);
    }

    @Override
    public void queryRecordInfo(CommonGBChannel channel, String startTime, String endTime, ErrorCallback<RecordInfo> callback) {
        if (channel.getDataType() == ChannelDataType.GB28181.value) {
            deviceChannelService.queryRecordInfo(channel, startTime, endTime, callback);
        } else if (channel.getDataType() == ChannelDataType.STREAM_PROXY.value) {
            // 拉流代理
            log.warn("[下载通用通道录像] 不支持下载拉流代理的录像： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.FORBIDDEN, "forbidden");
        } else if (channel.getDataType() == ChannelDataType.STREAM_PUSH.value) {
            // 推流
            log.warn("[下载通用通道录像] 不支持下载推流的录像： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.FORBIDDEN, "forbidden");
        } else {
            // 通道数据异常
            log.error("[回放通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public PageInfo<CommonGBChannel> queryListByCivilCodeForUnusual(int page, int count, String query, Boolean online, Integer channelType) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<CommonGBChannel> all = commonGBChannelMapper.queryListByCivilCodeForUnusual(query, online, channelType);
        return new PageInfo<>(all);
    }

    @Override
    public void clearChannelCivilCode(Boolean all, List<Integer> channelIds) {

        List<Integer> channelIdsForClear;
        if (all != null && all) {
            channelIdsForClear = commonGBChannelMapper.queryAllForUnusualCivilCode();
        }else {
            channelIdsForClear = channelIds;
        }
        commonGBChannelMapper.removeCivilCodeByChannelIds(channelIdsForClear);
    }

    @Override
    public PageInfo<CommonGBChannel> queryListByParentForUnusual(int page, int count, String query, Boolean online, Integer channelType) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<CommonGBChannel> all = commonGBChannelMapper.queryListByParentForUnusual(query, online, channelType);
        return new PageInfo<>(all);
    }

    @Override
    public void clearChannelParent(Boolean all, List<Integer> channelIds) {
        List<Integer> channelIdsForClear;
        if (all != null && all) {
            channelIdsForClear = commonGBChannelMapper.queryAllForUnusualParent();
        }else {
            channelIdsForClear = channelIds;
        }
        commonGBChannelMapper.removeParentIdByChannelIds(channelIdsForClear);
    }

    @Override
    public void updateGPSFromGPSMsgInfo(List<GPSMsgInfo> gpsMsgInfoList) {
        if (gpsMsgInfoList == null || gpsMsgInfoList.isEmpty()) {
            return;
        }
        commonGBChannelMapper.updateGpsByDeviceId(gpsMsgInfoList);
    }
}
