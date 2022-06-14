package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd.CatalogResponseMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.Coordtransform;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.task.impl.CatalogSubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.MobilePositionSubscribeTask;
import com.genersoft.iot.vmp.gb28181.bean.SyncStatus;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 设备业务（目录订阅）
 */
@Service
public class DeviceServiceImpl implements IDeviceService {

    private final static Logger logger = LoggerFactory.getLogger(DeviceServiceImpl.class);

    private final String  registerExpireTaskKeyPrefix = "device-register-expire-";

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private ISIPCommander sipCommander;

    @Autowired
    private CatalogResponseMessageHandler catalogResponseMessageHandler;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    @Autowired
    private IVideoManagerStorage storage;

    @Autowired
    private ISIPCommander commander;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private IMediaServerService mediaServerService;

    @Override
    public void online(Device device) {
        logger.info("[设备上线] deviceId：{}->{}:{}", device.getDeviceId(), device.getIp(), device.getPort());
        Device deviceInRedis = redisCatchStorage.getDevice(device.getDeviceId());
        Device deviceInDb = deviceMapper.getDeviceByDeviceId(device.getDeviceId());

        String now = DateUtil.getNow();
        if (deviceInRedis != null && deviceInDb == null) {
            // redis 存在脏数据
            redisCatchStorage.clearCatchByDeviceId(device.getDeviceId());
        }
        device.setUpdateTime(now);
        device.setOnline(1);

        // 第一次上线
        if (device.getCreateTime() == null) {
            device.setCreateTime(now);
            logger.info("[设备上线,首次注册]: {}，查询设备信息以及通道信息", device.getDeviceId());
            deviceMapper.add(device);
            redisCatchStorage.updateDevice(device);
            commander.deviceInfoQuery(device);
            sync(device);
        }else {
            deviceMapper.update(device);
            redisCatchStorage.updateDevice(device);
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
        String registerExpireTaskKey = registerExpireTaskKeyPrefix + device.getDeviceId();
        dynamicTask.startDelay(registerExpireTaskKey, ()-> offline(device.getDeviceId()), device.getExpires() * 1000);
    }

    @Override
    public void offline(String deviceId) {
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            return;
        }
        String registerExpireTaskKey = registerExpireTaskKeyPrefix + deviceId;
        dynamicTask.stop(registerExpireTaskKey);
        device.setOnline(0);
        redisCatchStorage.updateDevice(device);
        deviceMapper.update(device);
        // 离线释放所有ssrc
        List<SsrcTransaction> ssrcTransactions = streamSession.getSsrcTransactionForAll(deviceId, null, null, null);
        if (ssrcTransactions != null && ssrcTransactions.size() > 0) {
            for (SsrcTransaction ssrcTransaction : ssrcTransactions) {
                mediaServerService.releaseSsrc(ssrcTransaction.getMediaServerId(), ssrcTransaction.getSsrc());
                mediaServerService.closeRTPServer(deviceId, ssrcTransaction.getChannelId(), ssrcTransaction.getStream());
                streamSession.remove(deviceId, ssrcTransaction.getChannelId(), ssrcTransaction.getStream());
            }
        }
        // 移除订阅
        removeCatalogSubscribe(device);
        removeMobilePositionSubscribe(device);
    }

    @Override
    public boolean addCatalogSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        logger.info("[添加目录订阅] 设备{}", device.getDeviceId());
        // 添加目录订阅
        CatalogSubscribeTask catalogSubscribeTask = new CatalogSubscribeTask(device, sipCommander, dynamicTask);
        // 提前开始刷新订阅
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
        if (device.getOnline() == 1) {
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
        // 提前开始刷新订阅
        dynamicTask.startCron(device.getDeviceId() + "mobile_position" , mobilePositionSubscribeTask, (subscribeCycleForCatalog -1 ) * 1000);
        return true;
    }

    @Override
    public boolean removeMobilePositionSubscribe(Device device) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        logger.info("[移除移动位置订阅]: {}", device.getDeviceId());
        String taskKey = device.getDeviceId() + "mobile_position";
        if (device.getOnline() == 1) {
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
        sipCommander.catalogQuery(device, sn, event -> {
            String errorMsg = String.format("同步通道失败，错误码： %s, %s", event.statusCode, event.msg);
            catalogResponseMessageHandler.setChannelSyncEnd(device.getDeviceId(), errorMsg);
        });
    }

    @Override
    public Device queryDevice(String deviceId) {
        return deviceMapper.getDeviceByDeviceId(deviceId);
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
        if (device == null || device.getOnline() == 0) {
            return;
        }
        sipCommander.deviceStatusQuery(device, null);

    }

    @Override
    public Device getDeviceByHostAndPort(String host, int port) {
        return deviceMapper.getDeviceByHostAndPort(host, port);
    }

    @Override
    public void updateDevice(Device device) {

        Device deviceInStore = deviceMapper.getDeviceByDeviceId(device.getDeviceId());
        if (deviceInStore == null) {
            logger.warn("更新设备时未找到设备信息");
            return;
        }
        if (!StringUtils.isEmpty(device.getName())) {
            deviceInStore.setName(device.getName());
        }
        if (!StringUtils.isEmpty(device.getCharset())) {
            deviceInStore.setCharset(device.getCharset());
        }
        if (!StringUtils.isEmpty(device.getMediaServerId())) {
            deviceInStore.setMediaServerId(device.getMediaServerId());
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
        // 坐标系变化，需要重新计算GCJ02坐标和WGS84坐标
        if (!deviceInStore.getGeoCoordSys().equals(device.getGeoCoordSys())) {
            updateDeviceChannelGeoCoordSys(device);
        }

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
           for (DeviceChannel deviceChannel : deviceChannels) {
               if ("WGS84".equals(device.getGeoCoordSys())) {
                   deviceChannel.setLongitudeWgs84(deviceChannel.getLongitude());
                   deviceChannel.setLatitudeWgs84(deviceChannel.getLatitude());
                   Double[] position = Coordtransform.WGS84ToGCJ02(deviceChannel.getLongitude(), deviceChannel.getLatitude());
                   deviceChannel.setLongitudeGcj02(position[0]);
                   deviceChannel.setLatitudeGcj02(position[1]);
               }else if ("GCJ02".equals(device.getGeoCoordSys())) {
                   deviceChannel.setLongitudeGcj02(deviceChannel.getLongitude());
                   deviceChannel.setLatitudeGcj02(deviceChannel.getLatitude());
                   Double[] position = Coordtransform.GCJ02ToWGS84(deviceChannel.getLongitude(), deviceChannel.getLatitude());
                   deviceChannel.setLongitudeWgs84(position[0]);
                   deviceChannel.setLatitudeWgs84(position[1]);
               }
           }
       }
        storage.updateChannels(device.getDeviceId(), deviceChannels);
    }
}
