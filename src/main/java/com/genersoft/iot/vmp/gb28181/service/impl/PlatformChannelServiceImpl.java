package com.genersoft.iot.vmp.gb28181.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.PlatformChannel;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * @author lin
 */
@Slf4j
@Service
@DS("master")
public class PlatformChannelServiceImpl implements IPlatformChannelService {

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    EventPublisher eventPublisher;


    @Override
    public PageInfo<PlatformChannel> queryChannelList(int page, int count, String query, Boolean online, Integer platformId, Boolean hasShare) {
        PageHelper.startPage(page, count);
        List<PlatformChannel> all = platformChannelMapper.queryForPlatformSearch(platformId, query, online, hasShare);
        return new PageInfo<>(all);
    }

    @Override
    public int addAllChannel(Integer platformId) {
        List<CommonGBChannel> channelListNotShare = platformChannelMapper.queryNotShare(platformId, null);
        Assert.notEmpty(channelListNotShare, "所有通道已共享");
        int result = platformChannelMapper.addChannels(platformId, channelListNotShare);
        if (result > 0) {
            // 发送消息
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(platformId, channelListNotShare, CatalogEvent.ADD);
            } catch (Exception e) {
                log.warn("[关联全部通道] 发送失败，数量：{}", channelListNotShare.size(), e);
            }
        }
        return result;
    }

    @Override
    public int addChannels(Integer platformId, List<Integer> channelIds) {
        List<CommonGBChannel> channelListNotShare = platformChannelMapper.queryNotShare(platformId, channelIds);
        Assert.notEmpty(channelListNotShare, "通道已共享");
        int result = platformChannelMapper.addChannels(platformId, channelListNotShare);
        if (result > 0) {
            // 发送消息
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(platformId, channelListNotShare, CatalogEvent.ADD);
            } catch (Exception e) {
                log.warn("[关联通道] 发送失败，数量：{}", channelListNotShare.size(), e);
            }
        }
        return result;
    }

    @Override
    public int removeAllChannel(Integer platformId) {
        List<CommonGBChannel> channelListNotShare = platformChannelMapper.queryNotShare(platformId,  null);
        Assert.notEmpty(channelListNotShare, "未共享任何通道");
        int result = platformChannelMapper.removeChannels(platformId, channelListNotShare);
        if (result > 0) {
            // 发送消息
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(platformId, channelListNotShare, CatalogEvent.DEL);
            } catch (Exception e) {
                log.warn("[移除全部关联通道] 发送失败，数量：{}", channelListNotShare.size(), e);
            }
        }
        return result;
    }

    @Override
    public int removeChannels(Integer platformId, List<Integer> channelIds) {
        List<CommonGBChannel> channelList = platformChannelMapper.queryShare(platformId, channelIds);
        Assert.notEmpty(channelList, "所选通道未共享");
        int result = platformChannelMapper.removeChannels(platformId, channelList);
        if (result > 0) {
            // 发送消息
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(platformId, channelList, CatalogEvent.DEL);
            } catch (Exception e) {
                log.warn("[移除关联通道] 发送失败，数量：{}", channelList.size(), e);
            }
        }
        return result;
    }
}
