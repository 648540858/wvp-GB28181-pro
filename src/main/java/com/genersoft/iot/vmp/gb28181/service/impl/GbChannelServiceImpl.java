package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class GbChannelServiceImpl implements IGbChannelService {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private CommonGBChannelMapper commonGBChannelMapper;

    @Override
    public CommonGBChannel queryByDeviceId(String gbDeviceId) {
        return commonGBChannelMapper.queryByDeviceId(gbDeviceId);
    }

    @Override
    public int add(CommonGBChannel commonGBChannel) {
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
            }catch (Exception e) {
                log.warn("[通道移除通知] 发送失败，{}", channel.getGbDeviceId(), e);
            }
        }
        return 1;
    }

    @Override
    public int update(CommonGBChannel commonGBChannel) {
        if (commonGBChannel.getGbId() <= 0) {
            log.warn("[更新通道] 未找到数据库ID，更新失败， {}", commonGBChannel.getGbDeviceDbId());
            return 0;
        }
        int result = commonGBChannelMapper.update(commonGBChannel);
        if (result > 0) {
            try {
                // 发送通知
                eventPublisher.catalogEventPublish(null, commonGBChannel, CatalogEvent.UPDATE);
            }catch (Exception e) {
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
            }catch (Exception e) {
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
        }else {
            result += commonGBChannelMapper.updateStatusForListById(onlineChannelList, 0);
        }
        if (result > 0) {
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(null, onlineChannelList, CatalogEvent.OFF);
            }catch (Exception e) {
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
            }catch (Exception e) {
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
        }else {
            result += commonGBChannelMapper.updateStatusForListById(offlineChannelList, 1);
        }
        if (result > 0) {
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(null, offlineChannelList, CatalogEvent.ON);
            }catch (Exception e) {
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
        }else {
            result += commonGBChannelMapper.batchAdd(commonGBChannels);
        }
        log.warn("[新增多个通道] 通道数量为{}，成功保存：{}", commonGBChannels.size(), result);
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
        }else {
            result += commonGBChannelMapper.updateStatus(commonGBChannels);
        }
        log.warn("[更新多个通道状态] 通道数量为{}，成功保存：{}", commonGBChannels.size(), result);
    }

    @Override
    public List<CommonGBChannel> queryByPlatformId(Integer platformId) {

        return commonGBChannelMapper.queryByPlatformId(platformId);
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
}
