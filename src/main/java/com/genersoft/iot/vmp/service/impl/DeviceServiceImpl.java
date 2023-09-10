package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.CatalogSubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.MobilePositionSubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd.CatalogResponseMessageHandler;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.BaseTree;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 设备业务（目录订阅）
 */
@Service
public class DeviceServiceImpl implements IDeviceService {

    private final static Logger logger = LoggerFactory.getLogger(DeviceServiceImpl.class);

    @Autowired
    private SIPCommander cmder;
    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private ISIPCommander sipCommander;

    @Autowired
    private CatalogResponseMessageHandler catalogResponseMessageHandler;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ISIPCommander commander;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private IMediaServerService mediaServerService;

    @Override
    public void online(Device device, SipTransactionInfo sipTransactionInfo) {
        logger.info("[设备上线] deviceId：{}->{}:{}", device.getDeviceId(), device.getIp(), device.getPort());
        Device deviceInRedis = redisCatchStorage.getDevice(device.getDeviceId());
        Device deviceInDb = deviceMapper.getDeviceByDeviceId(device.getDeviceId());

        String now = DateUtil.getNow();
        if (deviceInRedis != null && deviceInDb == null) {
            // redis 存在脏数据
            inviteStreamService.clearInviteInfo(device.getDeviceId());
        }
        device.setUpdateTime(now);
        device.setKeepaliveTime(now);
        if (device.getKeepaliveIntervalTime() == 0) {
            // 默认心跳间隔60
            device.setKeepaliveIntervalTime(60);
        }
        if (sipTransactionInfo != null) {
            device.setSipTransactionInfo(sipTransactionInfo);
        }else {
            if (deviceInRedis != null) {
                device.setSipTransactionInfo(deviceInRedis.getSipTransactionInfo());
            }
        }

        // 第一次上线 或则设备之前是离线状态--进行通道同步和设备信息查询
        if (deviceInDb == null) {
            device.setOnLine(true);
            device.setCreateTime(now);
            device.setUpdateTime(now);
            logger.info("[设备上线,首次注册]: {}，查询设备信息以及通道信息", device.getDeviceId());
            deviceMapper.add(device);
            redisCatchStorage.updateDevice(device);
            try {
                commander.deviceInfoQuery(device);
            } catch (InvalidArgumentException | SipException | ParseException e) {
                logger.error("[命令发送失败] 查询设备信息: {}", e.getMessage());
            }
            sync(device);
        }else {

            if (deviceInDb != null) {
                device.setSwitchPrimarySubStream(deviceInDb.isSwitchPrimarySubStream());
            }
            if(!device.isOnLine()){
                device.setOnLine(true);
                device.setCreateTime(now);
                deviceMapper.update(device);
                redisCatchStorage.updateDevice(device);
                if (userSetting.getSyncChannelOnDeviceOnline()) {
                    logger.info("[设备上线,离线状态下重新注册]: {}，查询设备信息以及通道信息", device.getDeviceId());
                    try {
                        commander.deviceInfoQuery(device);
                    } catch (InvalidArgumentException | SipException | ParseException e) {
                        logger.error("[命令发送失败] 查询设备信息: {}", e.getMessage());
                    }
                    sync(device);
                    // TODO 如果设备下的通道级联到了其他平台，那么需要发送事件或者notify给上级平台
                }
            }else {
                if (deviceChannelMapper.queryAllChannels(device.getDeviceId()).size() == 0) {
                    logger.info("[设备上线]: {}，通道数为0,查询通道信息", device.getDeviceId());
                    sync(device);
                }

                deviceMapper.update(device);
                redisCatchStorage.updateDevice(device);
            }

        }

        // 上线添加订阅
        if (device.getSubscribeCycleForCatalog() > 0) {
            // 查询在线设备那些开启了订阅，为设备开启定时的目录订阅
            addCatalogSubscribe(device);
        }
        if (device.getSubscribeCycleForMobilePosition() > 0) {
            addMobilePositionSubscribe(device);
        }
        // 刷新过期任务
        String registerExpireTaskKey = VideoManagerConstants.REGISTER_EXPIRE_TASK_KEY_PREFIX + device.getDeviceId();
        // 如果第一次注册那么必须在60 * 3时间内收到一个心跳，否则设备离线
        dynamicTask.startDelay(registerExpireTaskKey, ()-> offline(device.getDeviceId(), "首次注册后未能收到心跳"), device.getKeepaliveIntervalTime() * 1000 * 3);
        if (userSetting.getDeviceStatusNotify()) {
            // 发送redis消息
            redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), null, true);
        }

//
//        try {
//            cmder.alarmSubscribe(device, 600, "0", "4", "0", "2023-7-27T00:00:00", "2023-7-28T00:00:00");
//        } catch (InvalidArgumentException e) {
//            throw new RuntimeException(e);
//        } catch (SipException e) {
//            throw new RuntimeException(e);
//        } catch (ParseException e) {
//            throw new RuntimeException(e);
//        }

    }

    @Override
    public void offline(String deviceId, String reason) {
        logger.warn("[设备离线]，{}, device：{}", reason, deviceId);
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            return;
        }
        String registerExpireTaskKey = VideoManagerConstants.REGISTER_EXPIRE_TASK_KEY_PREFIX + deviceId;
        dynamicTask.stop(registerExpireTaskKey);
        device.setOnLine(false);
        redisCatchStorage.updateDevice(device);
        deviceMapper.update(device);
        //进行通道离线
//        deviceChannelMapper.offlineByDeviceId(deviceId);
        // 离线释放所有ssrc
        List<SsrcTransaction> ssrcTransactions = streamSession.getSsrcTransactionForAll(deviceId, null, null, null);
        if (ssrcTransactions != null && ssrcTransactions.size() > 0) {
            for (SsrcTransaction ssrcTransaction : ssrcTransactions) {
                mediaServerService.releaseSsrc(ssrcTransaction.getMediaServerId(), ssrcTransaction.getSsrc());
                mediaServerService.closeRTPServer(ssrcTransaction.getMediaServerId(), ssrcTransaction.getStream());
                streamSession.remove(deviceId, ssrcTransaction.getChannelId(), ssrcTransaction.getStream());
            }
        }
        // 移除订阅
        removeCatalogSubscribe(device);
        removeMobilePositionSubscribe(device);
        if (userSetting.getDeviceStatusNotify()) {
            // 发送redis消息
            redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), null, false);
        }

    }

    @Override
    public boolean addCatalogSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        logger.info("[添加目录订阅] 设备{}", device.getDeviceId());
        // 添加目录订阅
        CatalogSubscribeTask catalogSubscribeTask = new CatalogSubscribeTask(device, sipCommander, dynamicTask);
        // 刷新订阅
        int subscribeCycleForCatalog = Math.max(device.getSubscribeCycleForCatalog(),30);
        // 设置最小值为30
        dynamicTask.startCron(device.getDeviceId() + "catalog", catalogSubscribeTask, (subscribeCycleForCatalog -1) * 1000);
        return true;
    }

    @Override
    public boolean removeCatalogSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        logger.info("[移除目录订阅]: {}", device.getDeviceId());
        String taskKey = device.getDeviceId() + "catalog";
        if (device.isOnLine()) {
            Runnable runnable = dynamicTask.get(taskKey);
            if (runnable instanceof ISubscribeTask) {
                ISubscribeTask subscribeTask = (ISubscribeTask) runnable;
                subscribeTask.stop();
            }
        }
        dynamicTask.stop(taskKey);
        return true;
    }

    @Override
    public boolean addMobilePositionSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForMobilePosition() < 0) {
            return false;
        }
        logger.info("[添加移动位置订阅] 设备{}", device.getDeviceId());
        // 添加目录订阅
        MobilePositionSubscribeTask mobilePositionSubscribeTask = new MobilePositionSubscribeTask(device, sipCommander, dynamicTask);
        // 设置最小值为30
        int subscribeCycleForCatalog = Math.max(device.getSubscribeCycleForMobilePosition(),30);
        // 刷新订阅
        dynamicTask.startCron(device.getDeviceId() + "mobile_position" , mobilePositionSubscribeTask, (subscribeCycleForCatalog) * 1000);
        return true;
    }

    @Override
    public boolean removeMobilePositionSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        logger.info("[移除移动位置订阅]: {}", device.getDeviceId());
        String taskKey = device.getDeviceId() + "mobile_position";
        if (device.isOnLine()) {
            Runnable runnable = dynamicTask.get(taskKey);
            if (runnable instanceof ISubscribeTask) {
                ISubscribeTask subscribeTask = (ISubscribeTask) runnable;
                subscribeTask.stop();
            }
        }
        dynamicTask.stop(taskKey);
        return true;
    }

    @Override
    public SyncStatus getChannelSyncStatus(String deviceId) {
        return catalogResponseMessageHandler.getChannelSyncProgress(deviceId);
    }

    @Override
    public Boolean isSyncRunning(String deviceId) {
        return catalogResponseMessageHandler.isSyncRunning(deviceId);
    }

    @Override
    public void sync(Device device) {
        if (catalogResponseMessageHandler.isSyncRunning(device.getDeviceId())) {
            logger.info("开启同步时发现同步已经存在");
            return;
        }
        int sn = (int)((Math.random()*9+1)*100000);
        catalogResponseMessageHandler.setChannelSyncReady(device, sn);
        try {
            sipCommander.catalogQuery(device, sn, event -> {
                String errorMsg = String.format("同步通道失败，错误码： %s, %s", event.statusCode, event.msg);
                catalogResponseMessageHandler.setChannelSyncEnd(device.getDeviceId(), errorMsg);
            });
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[同步通道], 信令发送失败：{}", e.getMessage() );
            String errorMsg = String.format("同步通道失败，信令发送失败： %s", e.getMessage());
            catalogResponseMessageHandler.setChannelSyncEnd(device.getDeviceId(), errorMsg);
        }
    }

    @Override
    public Device getDevice(String deviceId) {
        Device device = redisCatchStorage.getDevice(deviceId);
        if (device == null) {
            device = deviceMapper.getDeviceByDeviceId(deviceId);
            if (device != null) {
                redisCatchStorage.updateDevice(device);
            }
        }
        return device;
    }

    @Override
    public List<Device> getAllOnlineDevice() {
        return deviceMapper.getOnlineDevices();
    }

    @Override
    public boolean expire(Device device) {
        Instant registerTimeDate = Instant.from(DateUtil.formatter.parse(device.getRegisterTime()));
        Instant expireInstant = registerTimeDate.plusMillis(TimeUnit.SECONDS.toMillis(device.getExpires()));
        return expireInstant.isBefore(Instant.now());
    }

    @Override
    public void checkDeviceStatus(Device device) {
        if (device == null || !device.isOnLine()) {
            return;
        }
        try {
            sipCommander.deviceStatusQuery(device, null);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 设备状态查询: {}", e.getMessage());
        }

    }

    @Override
    public Device getDeviceByHostAndPort(String host, int port) {
        return deviceMapper.getDeviceByHostAndPort(host, port);
    }

    @Override
    public void updateDevice(Device device) {

        String now = DateUtil.getNow();
        device.setUpdateTime(now);
        device.setCharset(device.getCharset().toUpperCase());
        device.setUpdateTime(DateUtil.getNow());
        if (deviceMapper.update(device) > 0) {
            redisCatchStorage.updateDevice(device);
        }
    }

    /**
     * 更新通道坐标系
     */
    private void updateDeviceChannelGeoCoordSys(Device device) {
       List<DeviceChannel> deviceChannels =  deviceChannelMapper.getAllChannelWithCoordinate(device.getDeviceId());
       if (deviceChannels.size() > 0) {
           List<DeviceChannel> deviceChannelsForStore = new ArrayList<>();
           for (DeviceChannel deviceChannel : deviceChannels) {
               deviceChannelsForStore.add(deviceChannelService.updateGps(deviceChannel, device));
           }
           deviceChannelService.updateChannels(device.getDeviceId(), deviceChannelsForStore);
       }
    }


    @Override
    public List<BaseTree<DeviceChannel>> queryVideoDeviceTree(String deviceId, String parentId, boolean onlyCatalog) {
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            return null;
        }
        if (ObjectUtils.isEmpty(parentId) ) {
            parentId = deviceId;
        }
        List<DeviceChannel> rootNodes = deviceChannelMapper.getSubChannelsByDeviceId(deviceId, parentId, onlyCatalog);
        return transportChannelsToTree(rootNodes, "");
    }

    @Override
    public List<DeviceChannel> queryVideoDeviceInTreeNode(String deviceId, String parentId) {
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            return null;
        }
        if (ObjectUtils.isEmpty(parentId) || parentId.equals(deviceId)) {
            return deviceChannelMapper.getSubChannelsByDeviceId(deviceId, null, false);
        }else {
            return deviceChannelMapper.getSubChannelsByDeviceId(deviceId, parentId, false);
        }
    }

    private List<BaseTree<DeviceChannel>> transportChannelsToTree(List<DeviceChannel> channels, String parentId) {
        if (channels == null) {
            return null;
        }
        List<BaseTree<DeviceChannel>> treeNotes = new ArrayList<>();
        if (channels.size() == 0) {
            return treeNotes;
        }
        for (DeviceChannel channel : channels) {

            BaseTree<DeviceChannel> node = new BaseTree<>();
            node.setId(channel.getChannelId());
            node.setDeviceId(channel.getDeviceId());
            node.setName(channel.getName());
            node.setPid(parentId);
            node.setBasicData(channel);
            node.setParent(false);
            if (channel.getChannelId().length() <= 8) {
                node.setParent(true);
            }else {
                if (channel.getChannelId().length() != 20) {
                    node.setParent(channel.getParental() == 1);
                }else {
                    try {
                        int type = Integer.parseInt(channel.getChannelId().substring(10, 13));
                        if (type == 215 || type == 216 || type == 200) {
                            node.setParent(true);
                        }
                    }catch (NumberFormatException e) {
                        node.setParent(false);
                    }
                }
            }
            treeNotes.add(node);
        }
        Collections.sort(treeNotes);
        return treeNotes;
    }

    @Override
    public boolean isExist(String deviceId) {
        return deviceMapper.getDeviceByDeviceId(deviceId) != null;
    }

    @Override
    public void addDevice(Device device) {
        device.setOnLine(false);
        device.setCreateTime(DateUtil.getNow());
        device.setUpdateTime(DateUtil.getNow());
        deviceMapper.addCustomDevice(device);
    }

    @Override
    public void updateCustomDevice(Device device) {
        Device deviceInStore = deviceMapper.getDeviceByDeviceId(device.getDeviceId());
        if (deviceInStore == null) {
            logger.warn("更新设备时未找到设备信息");
            return;
        }
        if(deviceInStore.isSwitchPrimarySubStream() != device.isSwitchPrimarySubStream()){
            //当修改设备的主子码流开关时，需要校验是否存在流，如果存在流则直接关闭
            List<SsrcTransaction> ssrcTransactionForAll = streamSession.getSsrcTransactionForAll(device.getDeviceId(), null, null, null);
            if(ssrcTransactionForAll != null){
                for (SsrcTransaction ssrcTransaction: ssrcTransactionForAll) {
                    try {
                        cmder.streamByeCmd(device, ssrcTransaction.getChannelId(), ssrcTransaction.getStream(), null, null);
                    } catch (InvalidArgumentException | SsrcTransactionNotFoundException | ParseException | SipException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            deviceChannelMapper.clearPlay(device.getDeviceId());
            inviteStreamService.clearInviteInfo(device.getDeviceId());
        }

        if (!ObjectUtils.isEmpty(device.getName())) {
            deviceInStore.setName(device.getName());
        }
        if (!ObjectUtils.isEmpty(device.getCharset())) {
            deviceInStore.setCharset(device.getCharset());
        }
        if (!ObjectUtils.isEmpty(device.getMediaServerId())) {
            deviceInStore.setMediaServerId(device.getMediaServerId());
        }
        if (!ObjectUtils.isEmpty(device.getCharset())) {
            deviceInStore.setCharset(device.getCharset());
        }
        if (!ObjectUtils.isEmpty(device.getSdpIp())) {
            deviceInStore.setSdpIp(device.getSdpIp());
        }
        if (!ObjectUtils.isEmpty(device.getPassword())) {
            deviceInStore.setPassword(device.getPassword());
        }
        if (!ObjectUtils.isEmpty(device.getStreamMode())) {
            deviceInStore.setStreamMode(device.getStreamMode());
        }


        //  目录订阅相关的信息
        if (device.getSubscribeCycleForCatalog() > 0) {
            if (deviceInStore.getSubscribeCycleForCatalog() == 0 || deviceInStore.getSubscribeCycleForCatalog() != device.getSubscribeCycleForCatalog()) {
                deviceInStore.setSubscribeCycleForCatalog(device.getSubscribeCycleForCatalog());
                // 开启订阅
                addCatalogSubscribe(deviceInStore);
            }
        }else if (device.getSubscribeCycleForCatalog() == 0) {
            if (deviceInStore.getSubscribeCycleForCatalog() != 0) {
                deviceInStore.setSubscribeCycleForCatalog(device.getSubscribeCycleForCatalog());
                // 取消订阅
                removeCatalogSubscribe(deviceInStore);
            }
        }

        // 移动位置订阅相关的信息
        if (device.getSubscribeCycleForMobilePosition() > 0) {
            if (deviceInStore.getSubscribeCycleForMobilePosition() == 0 || deviceInStore.getSubscribeCycleForMobilePosition() != device.getSubscribeCycleForMobilePosition()) {
                deviceInStore.setMobilePositionSubmissionInterval(device.getMobilePositionSubmissionInterval());
                deviceInStore.setSubscribeCycleForMobilePosition(device.getSubscribeCycleForMobilePosition());
                // 开启订阅
                addMobilePositionSubscribe(deviceInStore);
            }
        }else if (device.getSubscribeCycleForMobilePosition() == 0) {
            if (deviceInStore.getSubscribeCycleForMobilePosition() != 0) {
                // 取消订阅
                removeMobilePositionSubscribe(deviceInStore);
            }
        }
        if (deviceInStore.getGeoCoordSys() != null) {
            // 坐标系变化，需要重新计算GCJ02坐标和WGS84坐标
            if (!deviceInStore.getGeoCoordSys().equals(device.getGeoCoordSys())) {
                deviceInStore.setGeoCoordSys(device.getGeoCoordSys());
                updateDeviceChannelGeoCoordSys(deviceInStore);
            }
        }else {
            deviceInStore.setGeoCoordSys("WGS84");
        }
        if (device.getCharset() == null) {
            deviceInStore.setCharset("GB2312");
        }
        //SSRC校验
        deviceInStore.setSsrcCheck(device.isSsrcCheck());
        //作为消息通道
        deviceInStore.setAsMessageChannel(device.isAsMessageChannel());
        
        // 更新redis
        deviceMapper.updateCustom(deviceInStore);
        redisCatchStorage.removeDevice(deviceInStore.getDeviceId());
    }

    @Override
    public boolean delete(String deviceId) {
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        boolean result = false;
        try {
            platformChannelMapper.delChannelForDeviceId(deviceId);
            deviceChannelMapper.cleanChannelsByDeviceId(deviceId);
            if ( deviceMapper.del(deviceId) < 0 ) {
                //事务回滚
                dataSourceTransactionManager.rollback(transactionStatus);
            }
            result = true;
            dataSourceTransactionManager.commit(transactionStatus);     //手动提交
        }catch (Exception e) {
            dataSourceTransactionManager.rollback(transactionStatus);
        }
        if (result) {
            redisCatchStorage.removeDevice(deviceId);
        }
        return result;
    }

    @Override
    public ResourceBaseInfo getOverview() {
        List<Device> onlineDevices = deviceMapper.getOnlineDevices();
        List<Device> all = deviceMapper.getAll();
        return new ResourceBaseInfo(all.size(), onlineDevices.size());
    }

    @Override
    public List<Device> getAll() {
        return deviceMapper.getAll();
    }


}
