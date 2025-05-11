package com.genersoft.iot.vmp.gb28181.service.impl;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.CatalogSubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.MobilePositionSubscribeTask;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * 设备业务（目录订阅）
 */
@Slf4j
@Service
public class DeviceServiceImpl implements IDeviceService {

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

    @Autowired
    private IRedisRpcService redisRpcService;

    private Device getDeviceByDeviceIdFromDb(String deviceId) {
        return deviceMapper.getDeviceByDeviceId(deviceId);
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
            if(!device.isOnLine()){
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
                deviceMapper.update(device);
                redisCatchStorage.updateDevice(device);
            }
            if (deviceChannelMapper.queryChannelsByDeviceDbId(device.getId()).isEmpty()) {
                log.info("[设备上线]: {}，通道数为0,查询通道信息", device.getDeviceId());
                sync(device);
            }
        }

        // 刷新过期任务
        String registerExpireTaskKey = VideoManagerConstants.REGISTER_EXPIRE_TASK_KEY_PREFIX + device.getDeviceId();
        // 如果第一次注册那么必须在60 * 3时间内收到一个心跳，否则设备离线
        dynamicTask.startDelay(registerExpireTaskKey, ()-> offline(device.getDeviceId(), "三次心跳超时"),
                device.getHeartBeatInterval() * 1000 * device.getHeartBeatCount());

    }

    @Override
    public void offline(String deviceId, String reason) {
        Device device = getDeviceByDeviceIdFromDb(deviceId);
        if (device == null) {
            log.warn("[设备不存在] device：{}", deviceId);
            return;
        }

        // 主动查询设备状态
        Boolean deviceStatus = getDeviceStatus(device);
        if (deviceStatus != null && deviceStatus) {
            log.info("[设备离线] 主动探测发现设备在线，暂不处理  device：{}", deviceId);
            online(device, null);
            return;
        }
        log.info("[设备离线] {}, device：{}， 心跳间隔： {}，心跳超时次数： {}， 上次心跳时间：{}， 上次注册时间： {}", reason, deviceId,
                device.getHeartBeatInterval(), device.getHeartBeatCount(), device.getKeepaliveTime(), device.getRegisterTime());
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
            if (callback != null) {
                callback.run(false);
            }
            return false;
        }
        log.info("[移除目录订阅]: {}", device.getDeviceId());
        String taskKey = device.getDeviceId() + "catalog";
        if (device.isOnLine()) {
            Runnable runnable = dynamicTask.get(taskKey);
            if (runnable instanceof ISubscribeTask) {
                ISubscribeTask subscribeTask = (ISubscribeTask) runnable;
                subscribeTask.stop(callback);
            }else {
                log.info("[移除目录订阅]失败，未找到订阅任务 : {}", device.getDeviceId());
                if (callback != null) {
                    callback.run(false);
                }
            }
        }else {
            log.info("[移除目录订阅订阅]失败，设备已经离线 : {}", device.getDeviceId());
            if (callback != null) {
                callback.run(false);
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
            if (callback != null) {
                callback.run(false);
            }
            return false;
        }
        log.info("[移除移动位置订阅]: {}", device.getDeviceId());
        String taskKey = device.getDeviceId() + "mobile_position";
        if (device.isOnLine()) {
            Runnable runnable = dynamicTask.get(taskKey);
            if (runnable instanceof ISubscribeTask) {
                ISubscribeTask subscribeTask = (ISubscribeTask) runnable;
                subscribeTask.stop(callback);
            }else {
                log.info("[移除移动位置订阅]失败，未找到订阅任务 : {}", device.getDeviceId());
                if (callback != null) {
                    callback.run(false);
                }
            }
        }else {
            log.info("[移除移动位置订阅]失败，设备已经离线 : {}", device.getDeviceId());
            if (callback != null) {
                callback.run(false);
            }
        }
        dynamicTask.stop(taskKey);
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
        return deviceMapper.getDevices(ChannelDataType.GB28181.value, status);
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

        String now = DateUtil.getNow();
        device.setUpdateTime(now);
        device.setCharset(device.getCharset() == null ? "" : device.getCharset().toUpperCase());
        device.setUpdateTime(DateUtil.getNow());
        if (deviceMapper.update(device) > 0) {
            redisCatchStorage.updateDevice(device);
        }
    }

    @Override
    public boolean isExist(String deviceId) {
        return getDeviceByDeviceIdFromDb(deviceId) != null;
    }

    @Override
    public void addDevice(Device device) {
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
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<Device> all = deviceMapper.getDeviceList(ChannelDataType.GB28181.value, query, status);
        return new PageInfo<>(all);
    }

    @Override
    public Device getDevice(Integer id) {
        return deviceMapper.query(id);
    }

    @Override
    public Device getDeviceByChannelId(Integer channelId) {
        return deviceMapper.queryByChannelId(ChannelDataType.GB28181.value,channelId);
    }

    @Override
    public Device getDeviceBySourceChannelDeviceId(String channelId) {
        return deviceMapper.getDeviceBySourceChannelDeviceId(ChannelDataType.GB28181.value,channelId);
    }

    @Override
    public void subscribeCatalog(int id, int cycle) {
        Device device = deviceMapper.query(id);
        Assert.notNull(device, "未找到设备");

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
                if (cycle > 0) {
                    // 开启订阅
                    addCatalogSubscribe(device);
                }
                // 因为是异步执行，需要在这里更新下数据
                deviceMapper.updateSubscribeCatalog(device);
                redisCatchStorage.updateDevice(device);
            });
        }else {
            // 开启订阅
            device.setSubscribeCycleForCatalog(cycle);
            addCatalogSubscribe(device);
            deviceMapper.updateSubscribeCatalog(device);
            redisCatchStorage.updateDevice(device);
        }
    }

    @Override
    public void subscribeMobilePosition(int id, int cycle, int interval) {
        Device device = deviceMapper.query(id);
        Assert.notNull(device, "未找到设备");
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
                if (cycle > 0) {
                    addMobilePositionSubscribe(device);
                }
                // 因为是异步执行，需要在这里更新下数据
                deviceMapper.updateSubscribeMobilePosition(device);
                redisCatchStorage.updateDevice(device);
            });
        }else {
            // 订阅未开启
            device.setSubscribeCycleForMobilePosition(cycle);
            device.setMobilePositionSubmissionInterval(interval);
            // 开启订阅
            addMobilePositionSubscribe(device);
            // 因为是异步执行，需要在这里更新下数据
            deviceMapper.updateSubscribeMobilePosition(device);
            redisCatchStorage.updateDevice(device);
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
            // 刷新过期任务
            String registerExpireTaskKey = VideoManagerConstants.REGISTER_EXPIRE_TASK_KEY_PREFIX + device.getDeviceId();
            // 如果第一次注册那么必须在60 * 3时间内收到一个心跳，否则设备离线
            dynamicTask.startDelay(registerExpireTaskKey, ()-> offline(device.getDeviceId(), "三次心跳超时"),
                    device.getHeartBeatInterval() * 1000 * device.getHeartBeatCount());
            deviceInDb.setHeartBeatCount(device.getHeartBeatCount());
            deviceInDb.setHeartBeatInterval(device.getHeartBeatInterval());
            deviceInDb.setPositionCapability(device.getPositionCapability());
            updateDevice(deviceInDb);
        }
    }

    @Override
    public WVPResult<SyncStatus> devicesSync(Device device) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
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
    public void queryPreset(Device device, String channelId, ErrorCallback<Object> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<Object> result = redisRpcService.queryPreset(device.getServerId(), device, channelId);
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
