package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.BatchLimit;
import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEventType;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.service.bean.*;
import com.genersoft.iot.vmp.storager.dao.CommonChannelMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.UpdateCommonChannelToGroup;
import com.genersoft.iot.vmp.vmanager.bean.UpdateCommonChannelToRegion;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.*;

@Service
public class CommonGbChannelServiceImpl implements ICommonGbChannelService {

    private final static Logger logger = LoggerFactory.getLogger(CommonGbChannelServiceImpl.class);

    @Autowired
    private CommonChannelMapper commonGbChannelMapper;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private Map<String, IResourceService> resourceServiceMap;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Autowired
    private ISIPCommanderForPlatform sipCommanderForPlatform;


    @Override
    public CommonGbChannel getChannel(String channelId) {
        return commonGbChannelMapper.queryByDeviceID(channelId);
    }

    /**
     * 发送catalog消息
     */
    private void sendCatalogEvent(List<CommonGbChannel> channelList, CatalogEventType catalogEventType) {
        // 获取开启了目录订阅且关联了这些通道的
        List<Integer> allCatalogSubscribePlatformList = subscribeHolder.getAllCatalogSubscribePlatform();
        // 获取所有开启了共享所有通道的上级与订阅通道的上级平台
        List<ParentPlatform> platformList = platformService.querySharePlatform(channelList, allCatalogSubscribePlatformList);

        for (ParentPlatform platform : platformList) {
            SubscribeInfo catalogSubscribe = subscribeHolder.getCatalogSubscribe(platform.getId());
            if (catalogSubscribe == null) {
                catalogSubscribe = SipUtils.buildVirtuallyCatalogSubSubscribe(platform);
            }
            // 获取关联的通道
            List<CommonGbChannel> channelListForShare = platformChannelService.queryChannelListInRange(platform.getId(), channelList);
            logger.warn("[发送Catalog事件] 类型： {}， 平台：{}， 通道个数： {}",
                    catalogEventType.getVal(), platform.getServerGBId(), channelListForShare.size());
            try {
                if (catalogEventType.equals(CatalogEventType.ADD) || catalogEventType.equals(CatalogEventType.UPDATE)) {
                    sipCommanderForPlatform.sendNotifyForCatalogAddOrUpdate(catalogEventType.getVal(), platform, channelListForShare, catalogSubscribe, 0);
                }else {
                    sipCommanderForPlatform.sendNotifyForCatalogOther(catalogEventType.getVal(), platform, channelListForShare, catalogSubscribe, 0);
                }
            } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendCatalogEvent(CommonGbChannel channel, CatalogEventType catalogEventType) {
        List<CommonGbChannel> commonGbChannelList = new ArrayList<>();
        commonGbChannelList.add(channel);
        sendCatalogEvent(commonGbChannelList, catalogEventType);
    }

    @Override
    public int add(CommonGbChannel channel) {
        int result = commonGbChannelMapper.add(channel);
        if (result == 0) {
            return 0;
        }
        sendCatalogEvent(channel, CatalogEventType.ADD);
        return result;
    }

    @Override
    public int update(CommonGbChannel channel) {
        assert channel.getCommonGbId() >= 0;
        assert channel.getCommonGbDeviceID() != null;
        assert channel.getCommonGbName() != null;
        channel.setUpdateTime(DateUtil.getNow());
        int result = commonGbChannelMapper.update(channel);
        if (result == 0) {
            return 0;
        }
        sendCatalogEvent(channel, CatalogEventType.UPDATE);
        return result;
    }

    @Override
    public int updateForForm(CommonGbChannel channel) {
        assert channel.getCommonGbId() >= 0;
        assert channel.getCommonGbDeviceID() != null;
        assert channel.getCommonGbName() != null;
        channel.setUpdateTime(DateUtil.getNow());
        CommonGbChannel commonGbChannel = commonGbChannelMapper.queryByDeviceID(channel.getCommonGbDeviceID());
        if (commonGbChannel != null && commonGbChannel.getCommonGbId() != channel.getCommonGbId()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "失败： 编号 " + channel.getCommonGbDeviceID() + " 已存在");
        }

        int result = commonGbChannelMapper.updateForForm(channel);
        if (result == 0) {
            return 0;
        }
        sendCatalogEvent(channel, CatalogEventType.UPDATE);
        return result;
    }

    @Override
    public void deleteGbChannelsFromList(List<DeviceChannel> channelList) {
        if (channelList.isEmpty()) {
            return;
        }
        List<String> channelIdList = new ArrayList<>(channelList.size());
        for (DeviceChannel deviceChannel : channelList) {
            channelIdList.add(deviceChannel.getChannelId());
        }
        commonGbChannelMapper.deleteByDeviceIDs(channelIdList);

    }

    @Override
    public PageInfo<CommonGbChannel> getChannelsInRegion(String regionDeviceId, String query, int page, int count) {
        assert regionDeviceId != null;
        PageHelper.startPage(page, count);
        List<CommonGbChannel> all = commonGbChannelMapper.getChannelsInRegion(regionDeviceId, query);
        return new PageInfo<>(all);
    }

    @Override
    public PageInfo<CommonGbChannel> queryChannelListInGroup(int page, int count, String query, String groupDeviceId,
                                                             String regionDeviceId, Boolean inGroup, Boolean inRegion,
                                                             String type, String ptzType, Boolean online) {
        PageHelper.startPage(page, count);
        if (query != null && ObjectUtils.isEmpty(query.trim())) {
            query = null;
        }
        if (groupDeviceId != null && ObjectUtils.isEmpty(groupDeviceId.trim())) {
            inGroup = null;
        }
        if (regionDeviceId != null && ObjectUtils.isEmpty(regionDeviceId.trim())) {
            inRegion = null;
        }
        if (type != null && ObjectUtils.isEmpty(type.trim())) {
            type = null;
        }
        if (ptzType != null && ObjectUtils.isEmpty(ptzType.trim())) {
            ptzType = null;
        }
        List<CommonGbChannel> all = commonGbChannelMapper.queryChannelListInGroup(query, groupDeviceId,
                regionDeviceId, inGroup, inRegion, type);
        return new PageInfo<>(all);
    }

    @Override
    public PageInfo<CommonGbChannel> queryChannelList(String query, int page, int count) {
        PageHelper.startPage(page, count);
        List<CommonGbChannel> all = commonGbChannelMapper.query(query);
        return new PageInfo<>(all);
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
    public void updateChannelToGroup(UpdateCommonChannelToGroup updateCommonChannelToGroup) {
        commonGbChannelMapper.updateChannelToGroup(updateCommonChannelToGroup);
    }

    @Override
    public void removeFromGroup(UpdateCommonChannelToGroup params) {
        if (!params.getCommonGbIds().isEmpty()) {
            commonGbChannelMapper.removeFromGroupByIds(params.getCommonGbIds());
        }
        if (!ObjectUtils.isEmpty(params.getCommonGbBusinessGroupID().trim())){
            commonGbChannelMapper.removeFromGroupByGroupId(params.getCommonGbBusinessGroupID());
        }
    }

    @Override
    public void removeFromRegion(UpdateCommonChannelToRegion params) {
        if (!params.getCommonGbIds().isEmpty()) {
            commonGbChannelMapper.removeRegionGroupByIds(params.getCommonGbIds());
        }
        if (!ObjectUtils.isEmpty(params.getCommonGbCivilCode().trim())){
            commonGbChannelMapper.removeFromRegionByRegionId(params.getCommonGbCivilCode());
        }
    }

    @Override
    public void updateChannelToRegion(UpdateCommonChannelToRegion params) {
        commonGbChannelMapper.updateChannelToRegion(params);
    }

    @Override
    public void startPlay(CommonGbChannel channel, IResourcePlayCallback callback) {
        IResourceService resourceService = resourceServiceMap.get(channel.getType());
        assert resourceService != null;
        resourceService.startPlay(channel, ((commonGbChannel, mediaServerItem, code, message, streamInfo) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
               // 记录到数据库
            }
            callback.call(commonGbChannel, mediaServerItem, code, message, streamInfo);
        }));
    }

    @Override
    public void stopPlay(CommonGbChannel channel, IResourcePlayCallback callback) {
        IResourceService resourceService = resourceServiceMap.get(channel.getType());
        assert resourceService != null;
        resourceService.stopPlay(channel,((commonGbChannel, mediaServerItem, code, message, streamInfo) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                // 记录到数据库
            }
            if (callback == null) {
                return;
            }
            callback.call(commonGbChannel, mediaServerItem, code, message, streamInfo);
        }));
    }

    @Override
    public void batchAdd(List<CommonGbChannel> commonGbChannels) {
        if (commonGbChannels.isEmpty()) {
            return;
        }
        if (commonGbChannels.size() > BatchLimit.count) {
            for (int i = 0; i < commonGbChannels.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > commonGbChannels.size()) {
                    toIndex = commonGbChannels.size();
                }
                commonGbChannelMapper.batchAdd(commonGbChannels.subList(i, toIndex));
                System.out.println(11);
            }
        }else {
            commonGbChannelMapper.batchAdd(commonGbChannels);
            System.out.println(11);
        }
    }

    @Override
    public void batchUpdate(List<CommonGbChannel> commonGbChannels) {
        for (CommonGbChannel commonGbChannel : commonGbChannels) {
            if (commonGbChannel.getCommonGbDeviceID().equals("34020000001310000002")) {
                System.out.println("34020000001310000002====" + commonGbChannel.getCommonGbStatus());
            }
        }
        if (commonGbChannels.isEmpty()) {
            return;
        }
        if (commonGbChannels.size() > BatchLimit.count) {
            for (int i = 0; i < commonGbChannels.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > commonGbChannels.size()) {
                    toIndex = commonGbChannels.size();
                }
                if (commonGbChannelMapper.batchUpdate(commonGbChannels.subList(i, toIndex)) < 0) {
                    throw new RuntimeException("batch update commonGbChannel fail");
                }
            }
        }else {
            if (commonGbChannelMapper.batchUpdate(commonGbChannels) < 0) {
                throw new RuntimeException("batch update commonGbChannel fail");
            }
        }
        sendCatalogEvent(commonGbChannels, CatalogEventType.UPDATE);
    }

    @Override
    public void batchDelete(List<Integer> channelsForDelete) {
        if (channelsForDelete.isEmpty()) {
            return;
        }
        List<CommonGbChannel> channelList = commonGbChannelMapper.queryInIdList(channelsForDelete);
        if (channelList.isEmpty()) {
            return;
        }
        if (channelsForDelete.size() > BatchLimit.count) {
            for (int i = 0; i < channelsForDelete.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > channelsForDelete.size()) {
                    toIndex = channelsForDelete.size();
                }
                if (commonGbChannelMapper.batchDelete(channelList.subList(i, toIndex)) < 0) {
                    throw new RuntimeException("batch update commonGbChannel fail");
                }
            }
        }else {
            if (commonGbChannelMapper.batchDelete(channelList) < 0) {
                throw new RuntimeException("batch update commonGbChannel fail");
            }
        }
        sendCatalogEvent(channelList, CatalogEventType.DEL);
    }

    @Override
    public void deleteById(int commonGbChannelId) {
        CommonGbChannel commonGbChannel = commonGbChannelMapper.getOne(commonGbChannelId);
        if (commonGbChannel == null) {
            return;
        }
        commonGbChannelMapper.delete(commonGbChannelId);
        sendCatalogEvent(commonGbChannel, CatalogEventType.DEL);
    }

    @Override
    public void deleteByIdList(List<Integer> commonChannelIdList) {
        List<CommonGbChannel> commonGbChannelList = commonGbChannelMapper.queryInIdList(commonChannelIdList);
        if (commonGbChannelList.isEmpty()) {
            return;
        }
        commonGbChannelMapper.batchDelete(commonGbChannelList);
        sendCatalogEvent(commonGbChannelList, CatalogEventType.DEL);
    }

    @Override
    public void offlineForList(List<Integer> commonChannelIdList) {
        List<CommonGbChannel> commonGbChannelList = commonGbChannelMapper.queryInIdList(commonChannelIdList);
        if (commonGbChannelList.isEmpty()) {
            return;
        }
        commonGbChannelMapper.channelsOfflineFromList(commonGbChannelList);
        sendCatalogEvent(commonGbChannelList, CatalogEventType.OFF);
    }

    @Override
    public void onlineForList(List<Integer> commonChannelIdList) {
        List<CommonGbChannel> commonGbChannelList = commonGbChannelMapper.queryInIdList(commonChannelIdList);
        if (commonGbChannelList.isEmpty()) {
            return;
        }
        commonGbChannelMapper.channelsOnlineFromList(commonGbChannelList);
        sendCatalogEvent(commonGbChannelList, CatalogEventType.ON);
    }
}
