package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.ParentPlatformMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformCatalogMapper;
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
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

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
    private PlatformCatalogMapper catalogMapper;

    @Autowired
    private EventPublisher eventPublisher;

    @Override
    public PageInfo<GbStream> getAll(Integer page, Integer count, String platFormId, String catalogId, String query, String mediaServerId) {
        PageHelper.startPage(page, count);
        List<GbStream> all = gbStreamMapper.selectAll(platFormId, catalogId, query, mediaServerId);
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
        if (catalogId == null) {
            catalogId = parentPlatform.getCatalogId();
        }
        try {
            List<DeviceChannel> deviceChannelList = new ArrayList<>();
            for (GbStream gbStream : gbStreams) {
                gbStream.setCatalogId(catalogId);
                gbStream.setPlatformId(platformId);
                // TODO 修改为批量提交
                platformGbStreamMapper.add(gbStream);
                DeviceChannel deviceChannelListByStream = getDeviceChannelListByStreamWithStatus(gbStream, catalogId, parentPlatform);
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
    public DeviceChannel getDeviceChannelListByStream(GbStream gbStream, String catalogId, ParentPlatform platform) {
        DeviceChannel deviceChannel = new DeviceChannel();
        deviceChannel.setChannelId(gbStream.getGbId());
        deviceChannel.setName(gbStream.getName());
        deviceChannel.setLongitude(gbStream.getLongitude());
        deviceChannel.setLatitude(gbStream.getLatitude());
        deviceChannel.setDeviceId(platform.getDeviceGBId());
        deviceChannel.setManufacture("wvp-pro");
        deviceChannel.setStatus(gbStream.isStatus()?1:0);

        deviceChannel.setRegisterWay(1);
        deviceChannel.setCivilCode(platform.getAdministrativeDivision());

        if (platform.getTreeType().equals(TreeType.CIVIL_CODE)){
            deviceChannel.setCivilCode(catalogId);
        }else if (platform.getTreeType().equals(TreeType.BUSINESS_GROUP)){
            PlatformCatalog catalog = catalogMapper.select(catalogId);
            if (catalog == null) {
                deviceChannel.setParentId(platform.getDeviceGBId());
                deviceChannel.setBusinessGroupId(null);
            }else {
                deviceChannel.setParentId(catalog.getId());
                deviceChannel.setBusinessGroupId(catalog.getBusinessGroupId());
            }

        }

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
            platformGbStreamMapper.delByAppAndStreamsByPlatformId(gbStreams, platformId);
            for (GbStream gbStream : gbStreams) {
                DeviceChannel deviceChannel = new DeviceChannel();
                deviceChannel.setChannelId(gbStream.getGbId());
                deviceChannelList.add(deviceChannel);
            }

            eventPublisher.catalogEventPublish(platformId, deviceChannelList, CatalogEvent.DEL);
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
            GbStream gbStreamIndb  = gbStreamMapper.selectOne(gbStream.getApp(), gbStream.getStream());
            if (gbStreamIndb != null && gbStreamIndb.getGbId() != null){
                gbStreams.add(gbStreamIndb);
            }
        }
        sendCatalogMsgs(gbStreams, type);
    }

    @Override
    public void sendCatalogMsgs(List<GbStream> gbStreams, String type) {
        if (gbStreams.size() > 0) {
            for (GbStream gs : gbStreams) {
                if (ObjectUtils.isEmpty(gs.getGbId())){
                    continue;
                }
                List<ParentPlatform> parentPlatforms = platformGbStreamMapper.selectByAppAndStream(gs.getApp(), gs.getStream());
                if (parentPlatforms.size() > 0) {
                    for (ParentPlatform parentPlatform : parentPlatforms) {
                        if (parentPlatform != null) {
                            eventPublisher.catalogEventPublishForStream(parentPlatform.getServerGBId(), gs, type);
                        }
                    }
                }
            }
        }
    }

    @Override
    public int updateGbIdOrName(List<StreamPushItem> streamPushItemForUpdate) {
        return gbStreamMapper.updateGbIdOrName(streamPushItemForUpdate);
    }

    @Override
    public DeviceChannel getDeviceChannelListByStreamWithStatus(GbStream gbStream, String catalogId, ParentPlatform platform) {
        DeviceChannel deviceChannel = new DeviceChannel();
        deviceChannel.setChannelId(gbStream.getGbId());
        deviceChannel.setName(gbStream.getName());
        deviceChannel.setLongitude(gbStream.getLongitude());
        deviceChannel.setLatitude(gbStream.getLatitude());
        deviceChannel.setDeviceId(platform.getDeviceGBId());
        deviceChannel.setManufacture("wvp-pro");
        // todo 目前是每一条查询一次，需要优化
        Boolean status = null;
        if ("proxy".equals(gbStream.getStreamType())) {
            status = gbStreamMapper.selectStatusForProxy(gbStream.getApp(), gbStream.getStream());
        }else {
            status = gbStreamMapper.selectStatusForPush(gbStream.getApp(), gbStream.getStream());
        }
        deviceChannel.setStatus((status != null && status )?1:0);

        deviceChannel.setRegisterWay(1);
        deviceChannel.setCivilCode(platform.getAdministrativeDivision());

        if (platform.getTreeType().equals(TreeType.CIVIL_CODE)){
            deviceChannel.setCivilCode(catalogId);
        }else if (platform.getTreeType().equals(TreeType.BUSINESS_GROUP)){
            PlatformCatalog catalog = catalogMapper.select(catalogId);
            if (catalog == null) {
                deviceChannel.setParentId(platform.getDeviceGBId());
                deviceChannel.setBusinessGroupId(null);
            }else {
                deviceChannel.setParentId(catalog.getId());
                deviceChannel.setBusinessGroupId(catalog.getBusinessGroupId());
            }

        }

        deviceChannel.setModel("live");
        deviceChannel.setOwner("wvp-pro");
        deviceChannel.setParental(0);
        deviceChannel.setSecrecy("0");
        return deviceChannel;
    }
}
