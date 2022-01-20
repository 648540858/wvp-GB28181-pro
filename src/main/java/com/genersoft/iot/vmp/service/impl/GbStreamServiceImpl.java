package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.ParentPlatformMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.ArrayList;
import java.util.List;

@Service
public class GbStreamServiceImpl implements IGbStreamService {

    private final static Logger logger = LoggerFactory.getLogger(GbStreamServiceImpl.class);

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    private GbStreamMapper gbStreamMapper;

    @Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

    @Autowired
    private ParentPlatformMapper platformMapper;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private EventPublisher eventPublisher;

    @Override
    public PageInfo<GbStream> getAll(Integer page, Integer count, String platFormId) {
        PageHelper.startPage(page, count);
        List<GbStream> all = gbStreamMapper.selectAll(platFormId);
        return new PageInfo<>(all);
    }

    @Override
    public void del(String app, String stream) {
        gbStreamMapper.del(app, stream);
    }


    @Override
    public boolean addPlatformInfo(List<GbStream> gbStreams, String platformId, String catalogId) {
        // 放在事务内执行
        boolean result = false;
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        ParentPlatform parentPlatform = platformMapper.getParentPlatByServerGBId(platformId);
        try {
            List<DeviceChannel> deviceChannelList = new ArrayList<>();
            for (GbStream gbStream : gbStreams) {
                gbStream.setCatalogId(catalogId);
                gbStream.setPlatformId(platformId);
                // TODO 修改为批量提交
                platformGbStreamMapper.add(gbStream);
                DeviceChannel deviceChannelListByStream = getDeviceChannelListByStream(gbStream, catalogId, parentPlatform.getDeviceGBId());
                deviceChannelList.add(deviceChannelListByStream);
            }
            dataSourceTransactionManager.commit(transactionStatus);     //手动提交
            eventPublisher.catalogEventPublish(platformId, deviceChannelList, CatalogEvent.ADD);
            result = true;
        }catch (Exception e) {
            logger.error("批量保存流与平台的关系时错误", e);
            dataSourceTransactionManager.rollback(transactionStatus);
        }
        return result;
    }

    @Override
    public DeviceChannel getDeviceChannelListByStream(GbStream gbStream, String catalogId, String deviceGBId) {
        DeviceChannel deviceChannel = new DeviceChannel();
        deviceChannel.setChannelId(gbStream.getGbId());
        deviceChannel.setName(gbStream.getName());
        deviceChannel.setLongitude(gbStream.getLongitude());
        deviceChannel.setLatitude(gbStream.getLatitude());
        deviceChannel.setDeviceId(deviceGBId);
        deviceChannel.setManufacture("wvp-pro");
        deviceChannel.setStatus(gbStream.isStatus()?1:0);
        deviceChannel.setParentId(catalogId ==null?gbStream.getCatalogId():catalogId);
        deviceChannel.setRegisterWay(1);
        deviceChannel.setCivilCode(sipConfig.getDomain());
        deviceChannel.setModel("live");
        deviceChannel.setOwner("wvp-pro");
        deviceChannel.setParental(0);
        deviceChannel.setSecrecy("0");
        return deviceChannel;
    }

    @Override
    public boolean delPlatformInfo(String platformId, List<GbStream> gbStreams) {
        // 放在事务内执行
        boolean result = false;
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        try {
            List<DeviceChannel> deviceChannelList = new ArrayList<>();
            for (GbStream gbStream : gbStreams) {
                platformGbStreamMapper.delByAppAndStream(gbStream.getApp(), gbStream.getStream());
                DeviceChannel deviceChannel = new DeviceChannel();
                deviceChannel.setChannelId(gbStream.getGbId());
                deviceChannelList.add(deviceChannel);
                eventPublisher.catalogEventPublish(platformId, deviceChannel, CatalogEvent.DEL);
            }

            dataSourceTransactionManager.commit(transactionStatus);     //手动提交
            result = true;
        }catch (Exception e) {
            logger.error("批量移除流与平台的关系时错误", e);
            dataSourceTransactionManager.rollback(transactionStatus);
        }
        return result;
    }

    @Override
    public void sendCatalogMsg(GbStream gbStream, String type) {
        List<GbStream> gbStreams = new ArrayList<>();
        if (gbStream.getGbId() != null) {
            gbStreams.add(gbStream);
        }else {
            StreamProxyItem streamProxyItem = gbStreamMapper.selectOne(gbStream.getApp(), gbStream.getStream());
            if (streamProxyItem != null && streamProxyItem.getGbId() != null){
                gbStreams.add(streamProxyItem);
            }
        }
        if (gbStreams.size() > 0) {
            for (GbStream gs : gbStreams) {
                List<ParentPlatform> parentPlatforms = platformGbStreamMapper.selectByAppAndStream(gs.getApp(), gs.getStream());
                if (parentPlatforms.size() > 0) {
                    for (ParentPlatform parentPlatform : parentPlatforms) {
                        eventPublisher.catalogEventPublishForStream(parentPlatform.getServerGBId(), gs, type);
                    }
                }
            }
        }
    }
}
