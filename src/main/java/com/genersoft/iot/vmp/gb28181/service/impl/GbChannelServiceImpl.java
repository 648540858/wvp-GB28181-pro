package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.GroupMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.RegionMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
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
    private RegionMapper regionMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Override
    public CommonGBChannel queryByDeviceId(String gbDeviceId) {
        return commonGBChannelMapper.queryByDeviceId(gbDeviceId);
    }

    @Override
    public int add(CommonGBChannel commonGBChannel) {
        if (commonGBChannel.getStreamPushId() != null && commonGBChannel.getStreamPushId() > 0) {
            CommonGBChannel commonGBChannelInDb = commonGBChannelMapper.queryByStreamPushId(commonGBChannel.getStreamPushId());
            if (commonGBChannelInDb != null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "此推流已经关联通道");
            }
        }
        if (commonGBChannel.getStreamProxyId() != null && commonGBChannel.getStreamProxyId() > 0) {
            CommonGBChannel commonGBChannelInDb = commonGBChannelMapper.queryByStreamProxyId(commonGBChannel.getStreamProxyId());
            if (commonGBChannelInDb != null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "此代理已经关联通道");
            }
        }
        return commonGBChannelMapper.insert(commonGBChannel);
    }

    @Override
    public int delete(int gbId) {
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
    public void delete(Collection<Integer> ids) {
        List<CommonGBChannel> channelListInDb = commonGBChannelMapper.queryByIds(ids);
        if (channelListInDb.isEmpty()) {
            return;
        }
        commonGBChannelMapper.batchDelete(channelListInDb);
        try {
            // 发送通知
            eventPublisher.catalogEventPublish(null, channelListInDb, CatalogEvent.DEL);
        } catch (Exception e) {
            log.warn("[通道移除通知] 发送失败，{}条", channelListInDb.size(), e);
        }
    }

    @Override
    public int update(CommonGBChannel commonGBChannel) {
        log.info("[更新通道] 通道ID: {}, ", commonGBChannel.getGbId());
        if (commonGBChannel.getGbId() <= 0) {
            log.warn("[更新通道] 未找到数据库ID，更新失败， {}", commonGBChannel.getGbDeviceDbId());
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
            log.warn("[通道离线] 未找到数据库ID，更新失败， {}", commonGBChannel.getGbDeviceDbId());
            return 0;
        }
        int result = commonGBChannelMapper.updateStatusById(commonGBChannel.getGbId(), 0);
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
        List<CommonGBChannel> onlineChannelList = commonGBChannelMapper.queryInListByStatus(commonGBChannelList, 1);
        if (onlineChannelList.isEmpty()) {
            log.warn("[多个通道离线] 更新失败, 参数内通道已经离线");
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
                result += commonGBChannelMapper.updateStatusForListById(onlineChannelList.subList(i, toIndex), 0);
            }
        } else {
            result += commonGBChannelMapper.updateStatusForListById(onlineChannelList, 0);
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
            log.warn("[通道上线] 未找到数据库ID，更新失败， {}", commonGBChannel.getGbDeviceDbId());
            return 0;
        }
        int result = commonGBChannelMapper.updateStatusById(commonGBChannel.getGbId(), 1);
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
        List<CommonGBChannel> offlineChannelList = commonGBChannelMapper.queryInListByStatus(commonGBChannelList, 0);
        if (offlineChannelList.isEmpty()) {
            log.warn("[多个通道上线] 更新失败, 参数内通道已经上线线");
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
                result += commonGBChannelMapper.updateStatusForListById(offlineChannelList.subList(i, toIndex), 1);
            }
        } else {
            result += commonGBChannelMapper.updateStatusForListById(offlineChannelList, 1);
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
        log.warn("[更新多个通道] 通道数量为{}，成功保存：{}", commonGBChannels.size(), result);
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
    public List<CommonGBChannel> queryByPlatform(Platform platform) {
        if (platform == null) {
            return null;
        }
        List<CommonGBChannel> commonGBChannelList = commonGBChannelMapper.queryWithPlatform(platform.getId());
        if (commonGBChannelList.isEmpty()) {
            return new ArrayList<>();
        }
        List<CommonGBChannel> channelList = new ArrayList<>();
        // 是否包含平台信息
        if (platform.getCatalogWithPlatform()) {
            CommonGBChannel channel = CommonGBChannel.build(platform);
            channelList.add(channel);
        }

        // 是否包含行政区划信息
        if (platform.getCatalogWithRegion()) {
            List<Region> regionChannelList = regionMapper.queryInChannelList(commonGBChannelList);
            if (!regionChannelList.isEmpty()) {
                // 获取这些节点的所有父节点
                List<Region> allRegion = getAllRegion(regionChannelList);
                for (Region region : allRegion) {
                    channelList.add(CommonGBChannel.build(region));
                }
            }
        }
        // 是否包含分组信息
        if (platform.getCatalogWithGroup()) {
            List<CommonGBChannel> groupChannelList = groupMapper.queryInChannelList(commonGBChannelList);
            if (!groupChannelList.isEmpty()) {
                // 获取这些节点的所有父节点
                channelList.addAll(groupChannelList);
            }
        }
        channelList.addAll(commonGBChannelList);
        return channelList;
    }

    private List<Region> getAllRegion(List<Region> regionChannelList ) {
        if (regionChannelList.isEmpty()) {
            return new ArrayList<>();
        }
        List<Region> channelList = regionMapper.queryParentInChannelList(regionChannelList);
        if (channelList.isEmpty()) {
            return channelList;
        }
        List<Region> allParentRegion = getAllRegion(channelList);
        channelList.addAll(allParentRegion);
        return channelList;
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
        if (channel.getGbDeviceDbId() <= 0) {
            log.warn("[重置国标通道] 非国标下级通道无法重置: id: {}", id);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "非国标下级通道无法重置");
        }
        // 这个多加一个参数,为了防止将非国标的通道通过此方法清空内容,导致意外发生
        commonGBChannelMapper.reset(id, channel.getGbDeviceDbId(), DateUtil.getNow());
    }

    @Override
    public PageInfo<CommonGBChannel> queryList(int page, int count, String query, Boolean online, Boolean hasCivilCode,
                                               Boolean hasGroup) {
        PageHelper.startPage(page, count);
        List<CommonGBChannel> all = commonGBChannelMapper.queryList(query, online, hasCivilCode, hasGroup);
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
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByGbDeviceIds(deviceIds);
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
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByGbDeviceIds(deviceIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        int result = commonGBChannelMapper.removeCivilCodeByChannels(channelList);
    }

    @Override
    public void removeParentIdByBusinessGroup(String businessGroup) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByBusinessGroup(businessGroup);
        if (channelList.isEmpty()) {
            return;
        }
        int result = commonGBChannelMapper.removeParentIdByChannels(channelList);

    }

    @Override
    public void removeParentIdByGroupList(List<Group> groupList) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByGroupList(groupList);
        if (channelList.isEmpty()) {
            return;
        }
        commonGBChannelMapper.removeParentIdByChannels(channelList);
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
    public void addChannelToGroup(String parentId, String businessGroup, List<Integer> channelIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByIds(channelIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        int result = commonGBChannelMapper.updateGroup(parentId, businessGroup, channelList);
        // 发送通知
        if (result > 0) {
            for (CommonGBChannel channel : channelList) {
                channel.setGbBusinessGroupId(businessGroup);
                channel.setGbParentId(parentId);
            }
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
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByGbDeviceIds(channelIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        int result = commonGBChannelMapper.removeParentIdByChannels(channelList);
    }

    @Override
    public void addChannelToGroupByGbDevice(String parentId, String businessGroup, List<Integer> deviceIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByGbDeviceIds(deviceIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        for (CommonGBChannel channel : channelList) {
            channel.setGbParentId(parentId);
            channel.setGbBusinessGroupId(businessGroup);
        }
        int result = commonGBChannelMapper.updateGroup(parentId, businessGroup, channelList);
        // 发送通知
        if (result > 0) {
            for (CommonGBChannel channel : channelList) {
                channel.setGbBusinessGroupId(businessGroup);
                channel.setGbParentId(parentId);
            }
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
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByGbDeviceIds(deviceIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        commonGBChannelMapper.removeParentIdByChannels(channelList);
    }

    @Override
    public CommonGBChannel queryOneWithPlatform(Integer platformId, String channelDeviceId) {
        return platformChannelMapper.queryOneWithPlatform(platformId, channelDeviceId);
    }
}
