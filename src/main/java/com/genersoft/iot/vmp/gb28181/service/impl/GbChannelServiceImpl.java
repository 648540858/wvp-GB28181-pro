package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        int result = commonGBChannelMapper.updateStatus(commonGBChannel.getGbId(), 0);
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
        int result = 0;
        for (CommonGBChannel channel : commonGBChannelList) {
            result += offline(channel);
        }
        return result;
    }

    @Override
    public int online(CommonGBChannel commonGBChannel) {
        if (commonGBChannel.getGbId() <= 0) {
            log.warn("[通道上线] 未找到数据库ID，更新失败， {}", commonGBChannel.getGbDeviceDbId());
            return 0;
        }
        int result = commonGBChannelMapper.updateStatus(commonGBChannel.getGbId(), 1);
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
        int result = 0;
        for (CommonGBChannel channel : commonGBChannelList) {
            result += online(channel);
        }
        return result;
    }

    @Override
    public void closeSend(CommonGBChannel commonGBChannel) {

    }

    @Override
    public void batchAdd(List<CommonGBChannel> commonGBChannels) {

    }

    @Override
    @Transactional
    public void updateStatus(List<CommonGBChannel> channelList) {
        if (channelList.isEmpty()) {
            log.warn("[更新多个通道状态] 通道数量为0，更新失败");
            return;
        }
        for (CommonGBChannel channel : channelList) {
            if  (channel.getGbStatus() == 1) {
                online(channel);
            }else {
                offline(channel);
            }

        }
    }

    @Override
    public List<CommonGBChannel> queryByPlatformId(Integer platformId) {
        return Collections.emptyList();
    }
}
