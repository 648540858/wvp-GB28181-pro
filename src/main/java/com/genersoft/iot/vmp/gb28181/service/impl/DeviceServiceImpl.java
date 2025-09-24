package com.genersoft.iot.vmp.gb28181.service.impl;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.task.deviceStatus.DeviceStatusTask;
import com.genersoft.iot.vmp.gb28181.task.deviceStatus.DeviceStatusTaskInfo;
import com.genersoft.iot.vmp.gb28181.task.deviceStatus.DeviceStatusTaskRunner;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.SubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.SubscribeTaskInfo;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.SubscribeTaskRunner;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.impl.SubscribeTaskForCatalog;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.impl.SubscribeTaskForMobilPosition;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd.CatalogResponseMessageHandler;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * 设备业务（目录订阅）
 */
@Slf4j
@Service
@Order(value=16)
public class DeviceServiceImpl implements IDeviceService, CommandLineRunner {

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
    private DeviceChannelMapper deviceChannelMapper;

    @Autowired
    private CommonGBChannelMapper commonGBChannelMapper;

    @Autowired
    private EventPublisher eventPublisher;

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

    @Autowired
    private IRedisRpcService redisRpcService;

    @Autowired
    private SubscribeTaskRunner subscribeTaskRunner;

    @Autowired
    private DeviceStatusTaskRunner deviceStatusTaskRunner;

    private Device getDeviceByDeviceIdFromDb(String deviceId) {
        return deviceMapper.getDeviceByDeviceId(deviceId);
    }

    @Override
    public void run(String... args) throws Exception {

        // 清理数据库不存在但是redis中存在的数据
        List<Device> devicesInDb = getAll();
        if (devicesInDb.isEmpty()) {
            redisCatchStorage.removeAllDevice();
        }else {
            List<Device> devicesInRedis = redisCatchStorage.getAllDevices();
            if (!devicesInRedis.isEmpty()) {
                Map<String, Device> deviceMapInDb = new HashMap<>();
                devicesInDb.parallelStream().forEach(device -> {
                    deviceMapInDb.put(device.getDeviceId(), device);
                });
                devicesInRedis.parallelStream().forEach(device -> {
                    if (deviceMapInDb.get(device.getDeviceId()) == null
                            && userSetting.getServerId().equals(device.getServerId())) {
                        redisCatchStorage.removeDevice(device.getDeviceId());
                    }
                });
            }
        }

        // 重置cseq计数
        redisCatchStorage.resetAllCSEQ();
        // 处理设备状态
        List<DeviceStatusTaskInfo> allTaskInfo = deviceStatusTaskRunner.getAllTaskInfo();
        List<String> onlineDeviceIds = new ArrayList<>();
        if (!allTaskInfo.isEmpty()) {
            for (DeviceStatusTaskInfo taskInfo : allTaskInfo) {
                Device device = getDeviceByDeviceId(taskInfo.getDeviceId());
                if (device == null) {
                    deviceStatusTaskRunner.removeTask(taskInfo.getDeviceId());
                    continue;
                }
                // 恢复定时任务, TCP因为连接已经断开必须等待设备重新连接
                DeviceStatusTask deviceStatusTask = DeviceStatusTask.getInstance(taskInfo.getDeviceId(),
                        taskInfo.getTransactionInfo(), taskInfo.getExpireTime() + 1000 + System.currentTimeMillis(), this::deviceStatusExpire);
                deviceStatusTaskRunner.addTask(deviceStatusTask);
                onlineDeviceIds.add(taskInfo.getDeviceId());
            }
            // 除了记录的设备以外， 其他设备全部离线
            List<Device> onlineDevice = getAllOnlineDevice(userSetting.getServerId());
            if (!onlineDevice.isEmpty()) {
                List<Device> offlineDevices = new ArrayList<>();
                for (Device device : onlineDevice) {
                    if (!onlineDeviceIds.contains(device.getDeviceId())) {
                        // 此设备需要离线
                        device.setOnLine(false);
                        // 清理离线设备的相关缓存
                        cleanOfflineDevice(device);
                        // 更新数据库
                        offlineDevices.add(device);
                    }
                }
                if (!offlineDevices.isEmpty()) {
                    offlineByIds(offlineDevices);
                }
            }
        }else {
            // 所有设备全部离线
            List<Device> onlineDevice = getAllOnlineDevice(userSetting.getServerId());
            for (Device device : onlineDevice) {
                // 此设备需要离线
                device.setOnLine(false);
                // 清理离线设备的相关缓存
                cleanOfflineDevice(device);
            }
            offlineByIds(onlineDevice);
        }

        // 处理订阅任务
        List<SubscribeTaskInfo> taskInfoList = subscribeTaskRunner.getAllTaskInfo();
        if (!taskInfoList.isEmpty()) {
            for (SubscribeTaskInfo taskInfo : taskInfoList) {
                if (taskInfo == null) {
                    continue;
                }
                Device device = getDeviceByDeviceId(taskInfo.getDeviceId());
                if (device == null || !device.isOnLine() || !onlineDeviceIds.contains(taskInfo.getDeviceId())) {
                    subscribeTaskRunner.removeSubscribe(taskInfo.getKey());
                    continue;
                }
                if (SubscribeTaskForCatalog.name.equals(taskInfo.getName())) {
                    device.setSubscribeCycleForCatalog((int)taskInfo.getExpireTime());
                    SubscribeTask subscribeTask = SubscribeTaskForCatalog.getInstance(device, this::catalogSubscribeExpire, taskInfo.getTransactionInfo());
                    if (subscribeTask != null) {
                        subscribeTaskRunner.addSubscribe(subscribeTask);
                    }
                }else if (SubscribeTaskForMobilPosition.name.equals(taskInfo.getName())) {
                    device.setSubscribeCycleForMobilePosition((int)taskInfo.getExpireTime());
                    SubscribeTask subscribeTask = SubscribeTaskForMobilPosition.getInstance(device, this::mobilPositionSubscribeExpire, taskInfo.getTransactionInfo());
                    if (subscribeTask != null) {
                        subscribeTaskRunner.addSubscribe(subscribeTask);
                    }
                }
            }
        }
    }

    private void offlineByIds(List<Device> offlineDevices) {
        if (offlineDevices.isEmpty()) {
            log.info("[更新多个离线设备信息] 参数为空");
            return;
        }
        deviceMapper.offlineByList(offlineDevices);
        for (Device device : offlineDevices) {
            device.setOnLine(false);
            redisCatchStorage.updateDevice(device);
        }
    }

    private void cleanOfflineDevice(Device device) {
        if (subscribeTaskRunner.containsKey(SubscribeTaskForCatalog.getKey(device))) {
            subscribeTaskRunner.removeSubscribe(SubscribeTaskForCatalog.getKey(device));
        }
        if (subscribeTaskRunner.containsKey(SubscribeTaskForMobilPosition.getKey(device))) {
            subscribeTaskRunner.removeSubscribe(SubscribeTaskForMobilPosition.getKey(device));
        }
        // 离线释放所有ssrc
        List<SsrcTransaction> ssrcTransactions = sessionManager.getSsrcTransactionByDeviceId(device.getDeviceId());
        if (ssrcTransactions != null && !ssrcTransactions.isEmpty()) {
            for (SsrcTransaction ssrcTransaction : ssrcTransactions) {
                mediaServerService.releaseSsrc(ssrcTransaction.getMediaServerId(), ssrcTransaction.getSsrc());
                mediaServerService.closeRTPServer(ssrcTransaction.getMediaServerId(), ssrcTransaction.getStream());
                sessionManager.removeByCallId(ssrcTransaction.getCallId());
            }
        }
        // 移除订阅
        removeCatalogSubscribe(device, null);
        removeMobilePositionSubscribe(device, null);

        List<AudioBroadcastCatch> audioBroadcastCatches = audioBroadcastManager.getByDeviceId(device.getDeviceId());
        if (!audioBroadcastCatches.isEmpty()) {
            for (AudioBroadcastCatch audioBroadcastCatch : audioBroadcastCatches) {

                SendRtpInfo sendRtpItem = sendRtpServerService.queryByChannelId(audioBroadcastCatch.getChannelId(), device.getDeviceId());
                if (sendRtpItem != null) {
                    sendRtpServerService.delete(sendRtpItem);
                    MediaServer mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                    mediaServerService.stopSendRtp(mediaInfo, sendRtpItem.getApp(), sendRtpItem.getStream(), null);
                }

                audioBroadcastManager.del(audioBroadcastCatch.getChannelId());
            }
        }
    }

    private void deviceStatusExpire(String deviceId, SipTransactionInfo transactionInfo) {
        log.info("[设备状态] 到期， 编号： {}", deviceId);
        offline(deviceId, "保活到期");
    }

    @Override
    public void online(Device device, SipTransactionInfo sipTransactionInfo) {
        log.info("[设备上线] deviceId：{}->{}:{}", device.getDeviceId(), device.getIp(), device.getPort());
        Device deviceInRedis = redisCatchStorage.getDevice(device.getDeviceId());
        Device deviceInDb = getDeviceByDeviceIdFromDb(device.getDeviceId());

        String now = DateUtil.getNow();
        if (deviceInRedis != null && deviceInDb == null) {
            // redis 存在脏数据
            inviteStreamService.clearInviteInfo(device.getDeviceId());
        }
        device.setUpdateTime(now);
        device.setKeepaliveTime(now);
        if (device.getHeartBeatCount() == null) {
            // 读取设备配置， 获取心跳间隔和心跳超时次数， 在次之前暂时设置为默认值
            device.setHeartBeatCount(3);
            device.setHeartBeatInterval(60);
            device.setPositionCapability(0);

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
            if(device.getStreamMode() == null) {
                device.setStreamMode("TCP-PASSIVE");
            }
            deviceMapper.add(device);
            redisCatchStorage.updateDevice(device);
            try {
                commander.deviceInfoQuery(device, null);
                commander.deviceConfigQuery(device, null, "BasicParam", null);
            } catch (InvalidArgumentException | SipException | ParseException e) {
                log.error("[命令发送失败] 查询设备信息: {}", e.getMessage());
            }
            sync(device);
        }else {
            device.setServerId(userSetting.getServerId());
            if(!deviceInDb.isOnLine()){
                device.setOnLine(true);
                device.setCreateTime(now);
                deviceMapper.update(device);
                redisCatchStorage.updateDevice(device);
                if (userSetting.getSyncChannelOnDeviceOnline()) {
                    log.info("[设备上线,离线状态下重新注册]: {}，查询设备信息以及通道信息", device.getDeviceId());
                    try {
                        commander.deviceInfoQuery(device, null);
                    } catch (InvalidArgumentException | SipException | ParseException e) {
                        log.error("[命令发送失败] 查询设备信息: {}", e.getMessage());
                    }
                    sync(device);
                }else {
                    if (isDevice(device.getDeviceId())) {
                        sync(device);
                    }
                }
                // 上线添加订阅
                if (device.getSubscribeCycleForCatalog() > 0 && !subscribeTaskRunner.containsKey(SubscribeTaskForCatalog.getKey(device))) {
                    // 查询在线设备那些开启了订阅，为设备开启定时的目录订阅
                    addCatalogSubscribe(device, null);
                }
                if (device.getSubscribeCycleForMobilePosition() > 0 && !subscribeTaskRunner.containsKey(SubscribeTaskForMobilPosition.getKey(device))) {
                    addMobilePositionSubscribe(device, null);
                }

                if (userSetting.getDeviceStatusNotify()) {
                    // 发送redis消息
                    redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), null, true);
                }

            }else {
                deviceMapper.update(device);
                redisCatchStorage.updateDevice(device);
            }
            if (deviceChannelMapper.queryChannelsByDeviceDbId(device.getId()).isEmpty()) {
                log.info("[设备上线]: {}，通道数为0,查询通道信息", device.getDeviceId());
                sync(device);
            }
        }
        long expiresTime = Math.min(device.getExpires(), device.getHeartBeatInterval() * device.getHeartBeatCount()) * 1000L;
        if (deviceStatusTaskRunner.containsKey(device.getDeviceId())) {
            if (sipTransactionInfo == null) {
                deviceStatusTaskRunner.updateDelay(device.getDeviceId(), expiresTime + System.currentTimeMillis());
            }else {
                deviceStatusTaskRunner.removeTask(device.getDeviceId());
                DeviceStatusTask task = DeviceStatusTask.getInstance(device.getDeviceId(), sipTransactionInfo, expiresTime + System.currentTimeMillis(), this::deviceStatusExpire);
                deviceStatusTaskRunner.addTask(task);
            }
        }else {
            DeviceStatusTask task = DeviceStatusTask.getInstance(device.getDeviceId(), sipTransactionInfo, expiresTime + System.currentTimeMillis(), this::deviceStatusExpire);
            deviceStatusTaskRunner.addTask(task);
        }

    }

    @Override
    @Transactional
    public void offline(String deviceId, String reason) {
        Device device = getDeviceByDeviceIdFromDb(deviceId);
        if (device == null) {
            log.warn("[设备不存在] device：{}", deviceId);
            return;
        }

        // 主动查询设备状态, 没有HostAddress无法发送请求，可能是手动添加的设备
        if (device.getHostAddress() != null) {
            Boolean deviceStatus = getDeviceStatus(device);
            if (deviceStatus != null && deviceStatus) {
                log.info("[设备离线] 主动探测发现设备在线，暂不处理  device：{}", deviceId);
                online(device, null);
                return;
            }
        }
        log.info("[设备离线] {}, device：{}， 心跳间隔： {}，心跳超时次数： {}， 上次心跳时间：{}， 上次注册时间： {}", reason, deviceId,
                device.getHeartBeatInterval(), device.getHeartBeatCount(), device.getKeepaliveTime(), device.getRegisterTime());
        device.setOnLine(false);
        cleanOfflineDevice(device);
        redisCatchStorage.updateDevice(device);
        deviceMapper.update(device);
        if (userSetting.getDeviceStatusNotify()) {
            // 发送redis消息
            redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), null, false);
        }
        if (isDevice(deviceId)) {
            channelOfflineByDevice(device);
        }
    }

    private void channelOfflineByDevice(Device device) {
        // 进行通道离线
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryOnlineListsByGbDeviceId(device.getId());
        if (channelList.isEmpty()) {
            return;
        }
        deviceChannelMapper.offlineByDeviceId(device.getId());
        // 发送通道离线通知
        eventPublisher.catalogEventPublish(null, channelList, CatalogEvent.OFF);
    }

    private boolean isDevice(String deviceId) {
        GbCode decode = GbCode.decode(deviceId);
        if (decode == null) {
            return true;
        }
        int code = Integer.parseInt(decode.getTypeCode());
        return code <= 199;
    }

    // 订阅丢失检查
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void lostCheckForSubscribe(){
        // 获取所有设备
        List<Device> deviceList = redisCatchStorage.getAllDevices();
        if (deviceList.isEmpty()) {
            return;
        }
        for (Device device : deviceList) {
            if (device == null || !device.isOnLine() || !userSetting.getServerId().equals(device.getServerId())) {
                continue;
            }
            if (device.getSubscribeCycleForCatalog() > 0 && !subscribeTaskRunner.containsKey(SubscribeTaskForCatalog.getKey(device))) {
                log.debug("[订阅丢失] 目录订阅， 编号： {}, 重新发起订阅", device.getDeviceId());
                addCatalogSubscribe(device, null);
            }
            if (device.getSubscribeCycleForMobilePosition() > 0 && !subscribeTaskRunner.containsKey(SubscribeTaskForMobilPosition.getKey(device))) {
                log.debug("[订阅丢失] 移动位置订阅， 编号： {}, 重新发起订阅", device.getDeviceId());
                addMobilePositionSubscribe(device, null);
            }
        }
    }

    // 设备状态丢失检查
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void lostCheckForStatus(){
        // 获取所有设备
        List<Device> deviceList = redisCatchStorage.getAllDevices();
        if (deviceList.isEmpty()) {
            return;
        }
        for (Device device : deviceList) {
            if (device == null || !device.isOnLine() || !userSetting.getServerId().equals(device.getServerId())) {
                continue;
            }
            if (!deviceStatusTaskRunner.containsKey(device.getDeviceId())) {
                log.debug("[状态丢失] 执行设备离线， 编号： {},", device.getDeviceId());
                offline(device.getDeviceId(), "");
            }
        }
    }

    private void catalogSubscribeExpire(String deviceId, SipTransactionInfo transactionInfo) {
        log.info("[目录订阅] 到期， 编号： {}", deviceId);
        Device device = getDeviceByDeviceId(deviceId);
        if (device == null) {
            log.info("[目录订阅] 到期， 编号： {}, 设备不存在， 忽略", deviceId);
            return;
        }
        if (device.isOnLine() && device.getSubscribeCycleForCatalog() > 0) {
            addCatalogSubscribe(device, transactionInfo);
        }
    }

    private void mobilPositionSubscribeExpire(String deviceId, SipTransactionInfo transactionInfo) {
        log.info("[移动位置订阅] 到期， 编号： {}", deviceId);
        Device device = getDeviceByDeviceId(deviceId);
        if (device == null) {
            log.info("[移动位置订阅] 到期， 编号： {}, 设备不存在， 忽略", deviceId);
            return;
        }
        if (device.isOnLine() && device.getSubscribeCycleForMobilePosition() > 0) {
            addMobilePositionSubscribe(device, transactionInfo);
        }
    }

    @Override
    public boolean addCatalogSubscribe(@NotNull Device device, SipTransactionInfo transactionInfo) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        if (transactionInfo == null) {
            log.info("[添加目录订阅] 设备 {}", device.getDeviceId());
        }else {
            log.info("[目录订阅续期] 设备 {}", device.getDeviceId());
        }
        try {
            sipCommander.catalogSubscribe(device, transactionInfo, eventResult -> {
                ResponseEvent event = (ResponseEvent) eventResult.event;
                // 成功
                log.info("[目录订阅]成功： {}", device.getDeviceId());
                if (!subscribeTaskRunner.containsKey(SubscribeTaskForCatalog.getKey(device))) {
                    SIPResponse response = (SIPResponse) event.getResponse();
                    SipTransactionInfo transactionInfoForResponse = new SipTransactionInfo(response);
                    SubscribeTask subscribeTask = SubscribeTaskForCatalog.getInstance(device, this::catalogSubscribeExpire, transactionInfoForResponse);
                    if (subscribeTask != null) {
                        subscribeTaskRunner.addSubscribe(subscribeTask);
                    }
                }else {
                    subscribeTaskRunner.updateDelay(SubscribeTaskForCatalog.getKey(device), (device.getSubscribeCycleForCatalog() * 1000L - 500L) + System.currentTimeMillis());
                }

            },eventResult -> {
                // 失败
                log.warn("[目录订阅]失败，信令发送失败： {}-{} ", device.getDeviceId(), eventResult.msg);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 目录订阅: {}", e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean removeCatalogSubscribe(@NotNull Device device, CommonCallback<Boolean> callback) {
        String key = SubscribeTaskForCatalog.getKey(device);
        if (subscribeTaskRunner.containsKey(key)) {
            log.info("[移除目录订阅]: {}", device.getDeviceId());
            SipTransactionInfo transactionInfo = subscribeTaskRunner.getTransactionInfo(key);
            if (transactionInfo == null) {
                log.warn("[移除目录订阅] 未找到事务信息，{}", device.getDeviceId());
            }
            try {
                device.setSubscribeCycleForCatalog(0);
                sipCommander.catalogSubscribe(device, transactionInfo, eventResult -> {
                    // 成功
                    log.info("[取消目录订阅]成功： {}", device.getDeviceId());
                    subscribeTaskRunner.removeSubscribe(SubscribeTaskForCatalog.getKey(device));
                    if (callback != null) {
                        callback.run(true);
                    }
                },eventResult -> {
                    // 失败
                    log.warn("[取消目录订阅]失败，信令发送失败： {}-{} ", device.getDeviceId(), eventResult.msg);
                });
            }catch (Exception e) {
                // 失败
                log.warn("[取消目录订阅]失败： {}-{} ", device.getDeviceId(), e.getMessage());
            }
        }
        return true;
    }

    @Override
    public boolean addMobilePositionSubscribe(@NotNull Device device, SipTransactionInfo transactionInfo) {
        if (transactionInfo == null) {
            log.info("[添加移动位置订阅] 设备 {}", device.getDeviceId());
        }else {
            log.info("[移动位置订阅续期] 设备 {}", device.getDeviceId());
        }
        try {
            sipCommander.mobilePositionSubscribe(device, transactionInfo, eventResult -> {
                ResponseEvent event = (ResponseEvent) eventResult.event;
                // 成功
                log.info("[移动位置订阅]成功： {}", device.getDeviceId());
                if (!subscribeTaskRunner.containsKey(SubscribeTaskForMobilPosition.getKey(device))) {
                    SIPResponse response = (SIPResponse) event.getResponse();
                    SipTransactionInfo transactionInfoForResponse = new SipTransactionInfo(response);
                    SubscribeTask subscribeTask = SubscribeTaskForMobilPosition.getInstance(device, this::mobilPositionSubscribeExpire, transactionInfoForResponse);
                    if (subscribeTask != null) {
                        subscribeTaskRunner.addSubscribe(subscribeTask);
                    }
                }else {
                    subscribeTaskRunner.updateDelay(SubscribeTaskForMobilPosition.getKey(device), (device.getSubscribeCycleForMobilePosition() * 1000L - 500L) + System.currentTimeMillis());
                }

            },eventResult -> {
                // 失败
                log.warn("[移动位置订阅]失败，信令发送失败： {}-{} ", device.getDeviceId(), eventResult.msg);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 移动位置订阅: {}", e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean removeMobilePositionSubscribe(Device device, CommonCallback<Boolean> callback) {

        String key = SubscribeTaskForMobilPosition.getKey(device);
        if (subscribeTaskRunner.containsKey(key)) {
            log.info("[移除移动位置订阅]: {}", device.getDeviceId());
            SipTransactionInfo transactionInfo = subscribeTaskRunner.getTransactionInfo(key);
            if (transactionInfo == null) {
                log.warn("[移除移动位置订阅] 未找到事务信息，{}", device.getDeviceId());
            }
            try {
                device.setSubscribeCycleForMobilePosition(0);
                sipCommander.mobilePositionSubscribe(device, transactionInfo, eventResult -> {
                    // 成功
                    log.info("[取消移动位置订阅]成功： {}", device.getDeviceId());
                    subscribeTaskRunner.removeSubscribe(SubscribeTaskForMobilPosition.getKey(device));
                    if (callback != null) {
                        callback.run(true);
                    }
                },eventResult -> {
                    // 失败
                    log.warn("[取消移动位置订阅]失败，信令发送失败： {}-{} ", device.getDeviceId(), eventResult.msg);
                });
            }catch (Exception e) {
                // 失败
                log.warn("[取消移动位置订阅]失败： {}-{} ", device.getDeviceId(), e.getMessage());
            }
        }
        return true;
    }

    @Override
    public SyncStatus getChannelSyncStatus(String deviceId) {
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR404.getCode(), "设备不存在");
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            return redisRpcService.getChannelSyncStatus(device.getServerId(), deviceId);
        }
        return catalogResponseMessageHandler.getChannelSyncProgress(deviceId);
    }

    @Override
    public Boolean isSyncRunning(String deviceId) {
        return catalogResponseMessageHandler.isSyncRunning(deviceId);
    }

    @Override
    public void sync(Device device) {
        if (catalogResponseMessageHandler.isSyncRunning(device.getDeviceId())) {
            SyncStatus syncStatus = catalogResponseMessageHandler.getChannelSyncProgress(device.getDeviceId());
            log.info("[同步通道] 同步已存在, 设备: {}, 同步信息: {}", device.getDeviceId(), JSON.toJSON(syncStatus));
            return;
        }
        int sn = (int)((Math.random()*9+1)*100000);
        catalogResponseMessageHandler.setChannelSyncReady(device, sn);
        try {
            sipCommander.catalogQuery(device, sn, event -> {
                String errorMsg = String.format("同步通道失败，错误码： %s, %s", event.statusCode, event.msg);
                log.info("[同步通道]失败,编号: {}, 错误码： {}, {}", device.getDeviceId(), event.statusCode, event.msg);
                catalogResponseMessageHandler.setChannelSyncEnd(device.getDeviceId(), sn, errorMsg);
            });
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[同步通道], 信令发送失败：{}", e.getMessage() );
            String errorMsg = String.format("同步通道失败，信令发送失败： %s", e.getMessage());
            catalogResponseMessageHandler.setChannelSyncEnd(device.getDeviceId(), sn, errorMsg);
        }
    }

    @Override
    public Device getDeviceByDeviceId(String deviceId) {
        Device device = redisCatchStorage.getDevice(deviceId);
        if (device == null) {
            device = getDeviceByDeviceIdFromDb(deviceId);
            if (device != null) {
                redisCatchStorage.updateDevice(device);
            }
        }
        return device;
    }

    @Override
    public List<Device> getAllOnlineDevice(String serverId) {
        return deviceMapper.getOnlineDevicesByServerId(serverId);
    }

    @Override
    public List<Device> getAllByStatus(Boolean status) {
        return deviceMapper.getDevices(ChannelDataType.GB28181, status);
    }

    @Override
    public boolean expire(Device device) {
        Instant registerTimeDate = Instant.from(DateUtil.formatter.parse(device.getRegisterTime()));
        Instant expireInstant = registerTimeDate.plusMillis(TimeUnit.SECONDS.toMillis(device.getExpires()));
        return expireInstant.isBefore(Instant.now());
    }

    @Override
    public Boolean getDeviceStatus(@NotNull Device device) {
        SynchronousQueue<String> queue = new SynchronousQueue<>();
        try {
            sipCommander.deviceStatusQuery(device, ((code, msg, data) -> {
                queue.offer(msg);
            }));
            String data = queue.poll(10, TimeUnit.SECONDS);
            if (data != null && "ONLINE".equalsIgnoreCase(data.trim())) {
                return Boolean.TRUE;
            }else {
                return Boolean.FALSE;
            }

        } catch (InvalidArgumentException | SipException | ParseException | InterruptedException e) {
            log.error("[命令发送失败] 设备状态查询: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Device getDeviceByHostAndPort(String host, int port) {
        return deviceMapper.getDeviceByHostAndPort(host, port);
    }

    @Override
    public void updateDevice(Device device) {

        device.setCharset(device.getCharset() == null ? "" : device.getCharset().toUpperCase());
        device.setUpdateTime(DateUtil.getNow());
        if (deviceMapper.update(device) > 0) {
            redisCatchStorage.updateDevice(device);
        }
    }

    @Transactional
    @Override
    public void updateDeviceList(List<Device> deviceList) {
        if (deviceList.isEmpty()){
            log.info("[批量更新设备] 列表为空，更细失败");
            return;
        }
        if (deviceList.size() == 1) {
            updateDevice(deviceList.get(0));
        }else {
            for (Device device : deviceList) {
                device.setCharset(device.getCharset() == null ? "" : device.getCharset().toUpperCase());
                device.setUpdateTime(DateUtil.getNow());
            }
            int limitCount = 300;
            if (!deviceList.isEmpty()) {
                if (deviceList.size() > limitCount) {
                    for (int i = 0; i < deviceList.size(); i += limitCount) {
                        int toIndex = i + limitCount;
                        if (i + limitCount > deviceList.size()) {
                            toIndex = deviceList.size();
                        }
                        deviceMapper.batchUpdate(deviceList.subList(i, toIndex));
                    }
                }else {
                    deviceMapper.batchUpdate(deviceList);
                }
                for (Device device : deviceList) {
                    redisCatchStorage.updateDevice(device);
                }
            }
        }
    }

    @Override
    public boolean isExist(String deviceId) {
        return getDeviceByDeviceIdFromDb(deviceId) != null;
    }

    @Override
    public void addCustomDevice(Device device) {
        device.setOnLine(false);
        device.setCreateTime(DateUtil.getNow());
        device.setUpdateTime(DateUtil.getNow());
        if(device.getStreamMode() == null) {
            device.setStreamMode("TCP-PASSIVE");
        }
        deviceMapper.addCustomDevice(device);
    }

    @Override
    public void updateCustomDevice(Device device) {
        // 订阅状态的修改使用一个单独方法控制，此处不再进行状态修改
        Device deviceInStore = deviceMapper.query(device.getId());
        if (deviceInStore == null) {
            log.warn("更新设备时未找到设备信息");
            return;
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

        deviceMapper.updateCustom(device);
        redisCatchStorage.updateDevice(device);
    }

    @Override
    @Transactional
    public boolean delete(String deviceId) {
        Device device = getDeviceByDeviceIdFromDb(deviceId);
        Assert.notNull(device, "未找到设备");
        if (subscribeTaskRunner.containsKey(SubscribeTaskForCatalog.getKey(device))) {
            removeCatalogSubscribe(device, null);
        }
        if (subscribeTaskRunner.containsKey(SubscribeTaskForMobilPosition.getKey(device))) {
            removeMobilePositionSubscribe(device, null);
        }
        if (deviceStatusTaskRunner.containsKey(deviceId)) {
            deviceStatusTaskRunner.removeTask(deviceId);
        }
        platformChannelMapper.delChannelForDeviceId(deviceId);
        deviceChannelMapper.cleanChannelsByDeviceId(device.getId());
        deviceMapper.del(deviceId);
        redisCatchStorage.removeDevice(deviceId);
        inviteStreamService.clearInviteInfo(deviceId);
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
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<Device> all = deviceMapper.getDeviceList(ChannelDataType.GB28181, query, status);
        return new PageInfo<>(all);
    }

    @Override
    public Device getDevice(Integer id) {
        return deviceMapper.query(id);
    }

    @Override
    public Device getDeviceByChannelId(Integer channelId) {
        return deviceMapper.queryByChannelId(ChannelDataType.GB28181,channelId);
    }

    @Override
    public Device getDeviceBySourceChannelDeviceId(String channelId) {
        return deviceMapper.getDeviceBySourceChannelDeviceId(ChannelDataType.GB28181,channelId);
    }

    @Override
    public void subscribeCatalog(int id, int cycle) {
        Device device = deviceMapper.query(id);
        Assert.notNull(device, "未找到设备");
        Assert.isTrue(device.isOnLine(), "设备已离线");
        if (device.getSubscribeCycleForCatalog() == cycle) {
            return;
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcService.subscribeCatalog(id, cycle);
            return;
        }
        //  目录订阅相关的信息
        if (device.getSubscribeCycleForCatalog() > 0) {
            // 订阅周期不同，则先取消
            removeCatalogSubscribe(device, result->{
                device.setSubscribeCycleForCatalog(cycle);
                updateDevice(device);
                if (cycle > 0) {
                    // 开启订阅
                    addCatalogSubscribe(device, null);
                }
            });
        }else {
            // 开启订阅
            device.setSubscribeCycleForCatalog(cycle);
            updateDevice(device);
            addCatalogSubscribe(device, null);
        }
    }

    @Override
    public void subscribeMobilePosition(int id, int cycle, int interval) {
        Device device = deviceMapper.query(id);
        Assert.notNull(device, "未找到设备");
        if (!device.isOnLine()) {
            // 开启订阅
            device.setSubscribeCycleForMobilePosition(cycle);
            device.setMobilePositionSubmissionInterval(interval);
            updateDevice(device);
            if (subscribeTaskRunner.containsKey(SubscribeTaskForMobilPosition.getKey(device))) {
                subscribeTaskRunner.removeSubscribe(SubscribeTaskForMobilPosition.getKey(device));
            }
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备已离线");
        }

        if (device.getSubscribeCycleForMobilePosition() == cycle) {
            return;
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcService.subscribeMobilePosition(id, cycle, interval);
            return;
        }
        //  目录订阅相关的信息
        if (device.getSubscribeCycleForMobilePosition() > 0) {
            // 订阅周期已经开启，则先取消
            removeMobilePositionSubscribe(device, result->{
                // 开启订阅
                device.setSubscribeCycleForMobilePosition(cycle);
                device.setMobilePositionSubmissionInterval(interval);
                updateDevice(device);
                if (cycle > 0) {
                    addMobilePositionSubscribe(device, null);
                }
            });
        }else {
            // 订阅未开启
            device.setSubscribeCycleForMobilePosition(cycle);
            device.setMobilePositionSubmissionInterval(interval);
            updateDevice(device);
            // 开启订阅
            addMobilePositionSubscribe(device, null);
        }
    }

    @Override
    public void updateDeviceHeartInfo(Device device) {
        Device deviceInDb = deviceMapper.query(device.getId());
        if (deviceInDb == null) {
            return;
        }
        if (!Objects.equals(deviceInDb.getHeartBeatCount(), device.getHeartBeatCount())
                || !Objects.equals(deviceInDb.getHeartBeatInterval(), device.getHeartBeatInterval())) {

            deviceInDb.setHeartBeatCount(device.getHeartBeatCount());
            deviceInDb.setHeartBeatInterval(device.getHeartBeatInterval());
            deviceInDb.setPositionCapability(device.getPositionCapability());
            updateDevice(deviceInDb);

            long expiresTime = Math.min(device.getExpires(), device.getHeartBeatInterval() * device.getHeartBeatCount()) * 1000L;
            if (deviceStatusTaskRunner.containsKey(device.getDeviceId())) {
                deviceStatusTaskRunner.updateDelay(device.getDeviceId(), expiresTime + System.currentTimeMillis());
            }
        }
    }

    @Override
    public WVPResult<SyncStatus> devicesSync(Device device) {
        if (device.getServerId() != null && !userSetting.getServerId().equals(device.getServerId())) {
            return redisRpcService.devicesSync(device.getServerId(), device.getDeviceId());
        }
        // 已存在则返回进度
        if (isSyncRunning(device.getDeviceId())) {
            SyncStatus channelSyncStatus = getChannelSyncStatus(device.getDeviceId());
            WVPResult<SyncStatus> wvpResult = new WVPResult();
            if (channelSyncStatus.getErrorMsg() != null) {
                wvpResult.setCode(ErrorCode.ERROR100.getCode());
                wvpResult.setMsg(channelSyncStatus.getErrorMsg());
            }else if (channelSyncStatus.getTotal() == null || channelSyncStatus.getTotal() == 0){
                wvpResult.setCode(ErrorCode.SUCCESS.getCode());
                wvpResult.setMsg("等待通道信息...");
            }else {
                wvpResult.setCode(ErrorCode.SUCCESS.getCode());
                wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());
                wvpResult.setData(channelSyncStatus);
            }
            return wvpResult;
        }
        sync(device);
        WVPResult<SyncStatus> wvpResult = new WVPResult<>();
        wvpResult.setCode(0);
        wvpResult.setMsg("开始同步");
        return wvpResult;
    }

    @Override
    public void deviceBasicConfig(Device device, BasicParam basicParam, ErrorCallback<String> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.deviceBasicConfig(device.getServerId(), device, basicParam);
            if (result.getCode() == ErrorCode.SUCCESS.getCode()) {
                callback.run(result.getCode(), result.getMsg(), result.getData());
            }
            return;
        }

        try {
            sipCommander.deviceBasicConfigCmd(device, basicParam, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 设备配置: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage());
        }
    }

    @Override
    public void deviceConfigQuery(Device device, String channelId, String configType, ErrorCallback<Object> callback) {

        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.deviceConfigQuery(device.getServerId(), device, channelId, configType);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }

        try {
            sipCommander.deviceConfigQuery(device, channelId, configType, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 获取设备配置: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage());
        }
    }

    @Override
    public void teleboot(Device device) {

        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcService.teleboot(device.getServerId(), device);
        }
        try {
            sipCommander.teleBootCmd(device);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 远程启动: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
        }
    }

    @Override
    public void record(Device device, String channelId, String recordCmdStr, ErrorCallback<String> callback) {

        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.recordControl(device.getServerId(), device, channelId, recordCmdStr);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }

        try {
            sipCommander.recordCmd(device, channelId, recordCmdStr, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 开始/停止录像: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage());
        }
    }

    @Override
    public void guard(Device device, String guardCmdStr, ErrorCallback<String> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.guard(device.getServerId(), device, guardCmdStr);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }

        try {
            sipCommander.guardCmd(device, guardCmdStr, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 布防/撤防操作: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage());
        }
    }

    @Override
    public void resetAlarm(Device device, String channelId, String alarmMethod, String alarmType, ErrorCallback<String> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.resetAlarm(device.getServerId(), device, channelId, alarmMethod, alarmType);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }
        try {
            sipCommander.alarmResetCmd(device, alarmMethod, alarmType, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 布防/撤防操作: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage());
        }

    }

    @Override
    public void iFrame(Device device, String channelId) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcService.iFrame(device.getServerId(), device, channelId);
            return;
        }

        try {
            sipCommander.iFrameCmd(device, channelId);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 强制关键帧操作: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage());
        }
    }

    @Override
    public void homePosition(Device device, String channelId, Boolean enabled, Integer resetTime, Integer presetIndex, ErrorCallback<String> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.homePosition(device.getServerId(), device, channelId, enabled, resetTime, presetIndex);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }

        try {
            sipCommander.homePositionCmd(device, channelId, enabled, resetTime, presetIndex, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 看守位控制: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
        }
    }

    @Override
    public void dragZoomIn(Device device, String channelId, int length, int width, int midpointx, int midpointy, int lengthx, int lengthy, ErrorCallback<String> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcService.dragZoomIn(device.getServerId(), device, channelId, length, width, midpointx, midpointy, lengthx, lengthy);
            return;
        }

        StringBuffer cmdXml = new StringBuffer(200);
        cmdXml.append("<DragZoomIn>\r\n");
        cmdXml.append("<Length>" + length+ "</Length>\r\n");
        cmdXml.append("<Width>" + width+ "</Width>\r\n");
        cmdXml.append("<MidPointX>" + midpointx+ "</MidPointX>\r\n");
        cmdXml.append("<MidPointY>" + midpointy+ "</MidPointY>\r\n");
        cmdXml.append("<LengthX>" + lengthx+ "</LengthX>\r\n");
        cmdXml.append("<LengthY>" + lengthy+ "</LengthY>\r\n");
        cmdXml.append("</DragZoomIn>\r\n");
        try {
            sipCommander.dragZoomCmd(device, channelId, cmdXml.toString(), callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 拉框放大: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " +  e.getMessage());
        }
    }

    @Override
    public void dragZoomOut(Device device, String channelId, int length, int width, int midpointx, int midpointy, int lengthx, int lengthy, ErrorCallback<String> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcService.dragZoomOut(device.getServerId(), device, channelId, length, width, midpointx, midpointy, lengthx, lengthy);
            return;
        }

        StringBuffer cmdXml = new StringBuffer(200);
        cmdXml.append("<DragZoomOut>\r\n");
        cmdXml.append("<Length>" + length+ "</Length>\r\n");
        cmdXml.append("<Width>" + width+ "</Width>\r\n");
        cmdXml.append("<MidPointX>" + midpointx+ "</MidPointX>\r\n");
        cmdXml.append("<MidPointY>" + midpointy+ "</MidPointY>\r\n");
        cmdXml.append("<LengthX>" + lengthx+ "</LengthX>\r\n");
        cmdXml.append("<LengthY>" + lengthy+ "</LengthY>\r\n");
        cmdXml.append("</DragZoomOut>\r\n");
        try {
            sipCommander.dragZoomCmd(device, channelId, cmdXml.toString(), callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 拉框放大: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " +  e.getMessage());
        }
    }

    @Override
    public void deviceStatus(Device device, ErrorCallback<String> callback) {

        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.deviceStatus(device.getServerId(), device);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }
        try {
            sipCommander.deviceStatusQuery(device, (code, msg, data) -> {
                if ("ONLINE".equalsIgnoreCase(data.trim())) {
                    online(device, null);
                }else {
                    offline(device.getDeviceId(), "设备状态查询结果：" + data.trim());
                }
                if (callback != null) {
                    callback.run(code, msg, data);
                }
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 获取设备状态: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
        }
    }


    @Override
    public void alarm(Device device, String startPriority, String endPriority, String alarmMethod, String alarmType, String startTime, String endTime, ErrorCallback<Object> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.alarm(device.getServerId(), device, startPriority, endPriority, alarmMethod, alarmType, startTime, endTime);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }

        String startAlarmTime = "";
        if (startTime != null) {
            startAlarmTime = DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(startTime);
        }
        String endAlarmTime = "";
        if (startTime != null) {
            endAlarmTime = DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(endTime);
        }

        try {
            sipCommander.alarmInfoQuery(device, startPriority, endPriority, alarmMethod, alarmType, startAlarmTime, endAlarmTime, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 获取设备状态: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
        }
    }

    @Override
    public void deviceInfo(Device device, ErrorCallback<Object> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<Object> result = redisRpcService.deviceInfo(device.getServerId(), device);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }

        try {
            sipCommander.deviceInfoQuery(device, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 获取设备信息: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
        }
    }

    @Override
    public void queryPreset(Device device, String channelId, ErrorCallback<List<Preset>> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<List<Preset>> result = redisRpcService.queryPreset(device.getServerId(), device, channelId);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }

        try {
            sipCommander.presetQuery(device, channelId, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 预制位查询: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
        }
    }


}
