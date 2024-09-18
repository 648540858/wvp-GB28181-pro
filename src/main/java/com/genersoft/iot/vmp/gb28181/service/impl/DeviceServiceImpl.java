package com.genersoft.iot.vmp.gb28181.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.CatalogSubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.MobilePositionSubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd.CatalogResponseMessageHandler;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 设备业务（目录订阅）
 */
@Slf4j
@Service
@DS("master")
public class DeviceServiceImpl implements IDeviceService {

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
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ISIPCommander commander;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Override
    public void online(Device device, SipTransactionInfo sipTransactionInfo) {
        log.info("[设备上线] deviceId：{}->{}:{}", device.getDeviceId(), device.getIp(), device.getPort());
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
            log.info("[设备上线,首次注册]: {}，查询设备信息以及通道信息", device.getDeviceId());
            deviceMapper.add(device);
            redisCatchStorage.updateDevice(device);
            try {
                commander.deviceInfoQuery(device);
            } catch (InvalidArgumentException | SipException | ParseException e) {
                log.error("[命令发送失败] 查询设备信息: {}", e.getMessage());
            }
            sync(device);
        }else {
            if(!device.isOnLine()){
                device.setOnLine(true);
                device.setCreateTime(now);
                deviceMapper.update(device);
                redisCatchStorage.updateDevice(device);
                if (userSetting.getSyncChannelOnDeviceOnline()) {
                    log.info("[设备上线,离线状态下重新注册]: {}，查询设备信息以及通道信息", device.getDeviceId());
                    try {
                        commander.deviceInfoQuery(device);
                    } catch (InvalidArgumentException | SipException | ParseException e) {
                        log.error("[命令发送失败] 查询设备信息: {}", e.getMessage());
                    }
                    sync(device);
                    // TODO 如果设备下的通道级联到了其他平台，那么需要发送事件或者notify给上级平台
                }
                // 上线添加订阅
                if (device.getSubscribeCycleForCatalog() > 0) {
                    // 查询在线设备那些开启了订阅，为设备开启定时的目录订阅
                    addCatalogSubscribe(device);
                }
                if (device.getSubscribeCycleForMobilePosition() > 0) {
                    addMobilePositionSubscribe(device);
                }
                if (userSetting.getDeviceStatusNotify()) {
                    // 发送redis消息
                    redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), null, true);
                }

            }else {
                if (deviceChannelMapper.queryAllChannels(device.getId()).isEmpty()) {
                    log.info("[设备上线]: {}，通道数为0,查询通道信息", device.getDeviceId());
                    sync(device);
                }

                deviceMapper.update(device);
                redisCatchStorage.updateDevice(device);
            }

        }

        // 刷新过期任务
        String registerExpireTaskKey = VideoManagerConstants.REGISTER_EXPIRE_TASK_KEY_PREFIX + device.getDeviceId();
        // 如果第一次注册那么必须在60 * 3时间内收到一个心跳，否则设备离线
        dynamicTask.startDelay(registerExpireTaskKey, ()-> offline(device.getDeviceId(), "首次注册后未能收到心跳"), device.getKeepaliveIntervalTime() * 1000 * 3);

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
        log.warn("[设备离线]，{}, device：{}", reason, deviceId);
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            return;
        }
        String registerExpireTaskKey = VideoManagerConstants.REGISTER_EXPIRE_TASK_KEY_PREFIX + deviceId;
        dynamicTask.stop(registerExpireTaskKey);
        if (device.isOnLine()) {
            if (userSetting.getDeviceStatusNotify()) {
                // 发送redis消息
                redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), null, false);
            }
        }

        device.setOnLine(false);
        redisCatchStorage.updateDevice(device);
        deviceMapper.update(device);
        //进行通道离线
//        deviceChannelMapper.offlineByDeviceId(deviceId);
        // 离线释放所有ssrc
        List<SsrcTransaction> ssrcTransactions = sessionManager.getSsrcTransactionByDeviceId(deviceId);
        if (ssrcTransactions != null && ssrcTransactions.size() > 0) {
            for (SsrcTransaction ssrcTransaction : ssrcTransactions) {
                mediaServerService.releaseSsrc(ssrcTransaction.getMediaServerId(), ssrcTransaction.getSsrc());
                mediaServerService.closeRTPServer(ssrcTransaction.getMediaServerId(), ssrcTransaction.getStream());
                sessionManager.removeByCallId(ssrcTransaction.getCallId());
            }
        }
        // 移除订阅
        removeCatalogSubscribe(device, null);
        removeMobilePositionSubscribe(device, null);

        List<AudioBroadcastCatch> audioBroadcastCatches = audioBroadcastManager.getByDeviceId(deviceId);
        if (!audioBroadcastCatches.isEmpty()) {
            for (AudioBroadcastCatch audioBroadcastCatch : audioBroadcastCatches) {

                SendRtpInfo sendRtpItem = sendRtpServerService.queryByChannelId(audioBroadcastCatch.getChannelId(), deviceId);
                if (sendRtpItem != null) {
                    sendRtpServerService.delete(sendRtpItem);
                    MediaServer mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                    mediaServerService.stopSendRtp(mediaInfo, sendRtpItem.getApp(), sendRtpItem.getStream(), null);
                }

                audioBroadcastManager.del(audioBroadcastCatch.getChannelId());
            }
        }
    }

    @Override
    public boolean addCatalogSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        log.info("[添加目录订阅] 设备{}", device.getDeviceId());
        // 添加目录订阅
        CatalogSubscribeTask catalogSubscribeTask = new CatalogSubscribeTask(device, sipCommander, dynamicTask);
        // 刷新订阅
        int subscribeCycleForCatalog = Math.max(device.getSubscribeCycleForCatalog(),30);
        // 设置最小值为30
        dynamicTask.startCron(device.getDeviceId() + "catalog", catalogSubscribeTask, (subscribeCycleForCatalog -1) * 1000);

        catalogSubscribeTask.run();
        return true;
    }

    @Override
    public boolean removeCatalogSubscribe(Device device, CommonCallback<Boolean> callback) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        log.info("[移除目录订阅]: {}", device.getDeviceId());
        String taskKey = device.getDeviceId() + "catalog";
        if (device.isOnLine()) {
            Runnable runnable = dynamicTask.get(taskKey);
            if (runnable instanceof ISubscribeTask) {
                ISubscribeTask subscribeTask = (ISubscribeTask) runnable;
                subscribeTask.stop(callback);
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
        log.info("[添加移动位置订阅] 设备{}", device.getDeviceId());
        // 添加目录订阅
        MobilePositionSubscribeTask mobilePositionSubscribeTask = new MobilePositionSubscribeTask(device, sipCommander, dynamicTask);
        // 设置最小值为30
        int subscribeCycleForCatalog = Math.max(device.getSubscribeCycleForMobilePosition(),30);
        // 刷新订阅
        dynamicTask.startCron(device.getDeviceId() + "mobile_position" , mobilePositionSubscribeTask, subscribeCycleForCatalog * 1000);
        mobilePositionSubscribeTask.run();
        return true;
    }

    @Override
    public boolean removeMobilePositionSubscribe(Device device, CommonCallback<Boolean> callback) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        log.info("[移除移动位置订阅]: {}", device.getDeviceId());
        String taskKey = device.getDeviceId() + "mobile_position";
        if (device.isOnLine()) {
            Runnable runnable = dynamicTask.get(taskKey);
            if (runnable instanceof ISubscribeTask) {
                ISubscribeTask subscribeTask = (ISubscribeTask) runnable;
                subscribeTask.stop(callback);
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
            log.info("开启同步时发现同步已经存在");
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
            log.error("[同步通道], 信令发送失败：{}", e.getMessage() );
            String errorMsg = String.format("同步通道失败，信令发送失败： %s", e.getMessage());
            catalogResponseMessageHandler.setChannelSyncEnd(device.getDeviceId(), errorMsg);
        }
    }

    @Override
    public Device getDeviceByDeviceId(String deviceId) {
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
    public List<Device> getAllByStatus(Boolean status) {
        return deviceMapper.getDevices(status);
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
            log.error("[命令发送失败] 设备状态查询: {}", e.getMessage());
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
        device.setCharset(device.getCharset() == null ? "" : device.getCharset().toUpperCase());
        device.setUpdateTime(DateUtil.getNow());
        if (deviceMapper.update(device) > 0) {
            redisCatchStorage.updateDevice(device);
        }
    }
    @Override
    public List<DeviceChannel> queryVideoDeviceInTreeNode(String deviceId, String parentId) {
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            return null;
        }
        if (ObjectUtils.isEmpty(parentId) || parentId.equals(deviceId)) {
            return deviceChannelMapper.getSubChannelsByDeviceId(device.getId(), null, false);
        }else {
            return deviceChannelMapper.getSubChannelsByDeviceId(device.getId(), parentId, false);
        }
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
            log.warn("更新设备时未找到设备信息");
            return;
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
        deviceInStore.setBroadcastPushAfterAck(device.isBroadcastPushAfterAck());
        //  目录订阅相关的信息
        if (deviceInStore.getSubscribeCycleForCatalog() != device.getSubscribeCycleForCatalog()) {
            if (device.getSubscribeCycleForCatalog() > 0) {
                // 若已开启订阅，但订阅周期不同，则先取消
                if (deviceInStore.getSubscribeCycleForCatalog() != 0) {
                    removeCatalogSubscribe(deviceInStore, result->{
                        // 开启订阅
                        deviceInStore.setSubscribeCycleForCatalog(device.getSubscribeCycleForCatalog());
                        addCatalogSubscribe(deviceInStore);
                        // 因为是异步执行，需要在这里更新下数据
                        deviceMapper.updateCustom(deviceInStore);
                        redisCatchStorage.updateDevice(deviceInStore);
                    });
                }else {
                    // 开启订阅
                    deviceInStore.setSubscribeCycleForCatalog(device.getSubscribeCycleForCatalog());
                    addCatalogSubscribe(deviceInStore);
                }

            }else if (device.getSubscribeCycleForCatalog() == 0) {
                // 取消订阅
                deviceInStore.setSubscribeCycleForCatalog(0);
                removeCatalogSubscribe(deviceInStore, null);
            }
        }
        // 移动位置订阅相关的信息
        if (deviceInStore.getSubscribeCycleForMobilePosition() != device.getSubscribeCycleForMobilePosition()) {
            if (device.getSubscribeCycleForMobilePosition() > 0) {
                // 若已开启订阅，但订阅周期不同，则先取消
                if (deviceInStore.getSubscribeCycleForMobilePosition() != 0) {
                    removeMobilePositionSubscribe(deviceInStore, result->{
                        // 开启订阅
                        deviceInStore.setSubscribeCycleForMobilePosition(device.getSubscribeCycleForMobilePosition());
                        deviceInStore.setMobilePositionSubmissionInterval(device.getMobilePositionSubmissionInterval());
                        addMobilePositionSubscribe(deviceInStore);
                        // 因为是异步执行，需要在这里更新下数据
                        deviceMapper.updateCustom(deviceInStore);
                        redisCatchStorage.updateDevice(deviceInStore);
                    });
                }else {
                    // 开启订阅
                    deviceInStore.setSubscribeCycleForMobilePosition(device.getSubscribeCycleForMobilePosition());
                    deviceInStore.setMobilePositionSubmissionInterval(device.getMobilePositionSubmissionInterval());
                    addMobilePositionSubscribe(deviceInStore);
                }

            }else if (device.getSubscribeCycleForMobilePosition() == 0) {
                // 取消订阅
                deviceInStore.setSubscribeCycleForMobilePosition(0);
                deviceInStore.setMobilePositionSubmissionInterval(0);
                removeMobilePositionSubscribe(deviceInStore, null);
            }
        }
        if (deviceInStore.getGeoCoordSys() != null) {
            // 坐标系变化，需要重新计算GCJ02坐标和WGS84坐标
            if (!deviceInStore.getGeoCoordSys().equals(device.getGeoCoordSys())) {
                deviceInStore.setGeoCoordSys(device.getGeoCoordSys());
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

        deviceMapper.updateCustom(deviceInStore);
        redisCatchStorage.updateDevice(deviceInStore);
    }

    @Override
    @Transactional
    public boolean delete(String deviceId) {
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到设备:" + deviceId);
        }
        platformChannelMapper.delChannelForDeviceId(deviceId);
        deviceChannelMapper.cleanChannelsByDeviceId(device.getId());
        deviceMapper.del(deviceId);
        redisCatchStorage.removeDevice(deviceId);
        return true;
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

    @Override
    public PageInfo<Device> getAll(int page, int count, String query, Boolean status) {
        PageHelper.startPage(page, count);
        List<Device> all = deviceMapper.getDeviceList(query, status);
        return new PageInfo<>(all);
    }

    @Override
    public Device getDevice(Integer id) {
        return deviceMapper.query(id);
    }

    @Override
    public Device getDeviceByChannelId(Integer channelId) {
        return deviceMapper.queryByChannelId(channelId);
    }
}
