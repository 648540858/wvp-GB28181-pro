package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd.CatalogResponseMessageHandler;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.task.impl.CatalogSubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.MobilePositionSubscribeTask;
import com.genersoft.iot.vmp.gb28181.bean.SyncStatus;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.sip.DialogState;
import javax.sip.TimeoutEvent;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
    private ISIPCommander commander;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private IMediaServerService mediaServerService;

    @Override
    public void online(Device device) {
        logger.info("[设备上线]，deviceId：" + device.getDeviceId());
        Device deviceInRedis = redisCatchStorage.getDevice(device.getDeviceId());
        Device deviceInDb = deviceMapper.getDeviceByDeviceId(device.getDeviceId());

        String now = DateUtil.getNow();
        if (deviceInRedis != null && deviceInDb == null) {
            // redis 存在脏数据
            redisCatchStorage.clearCatchByDeviceId(device.getDeviceId());

        }
        if (device.getRegisterTime() == null) {
            device.setRegisterTime(now);
        }
        if(device.getUpdateTime() == null) {
            device.setUpdateTime(now);
        }
        device.setOnline(1);

        // 第一次上线
        if (device.getCreateTime() == null) {
            device.setCreateTime(now);
            logger.info("[设备上线,首次注册]: {}，查询设备信息以及通道信息", device.getDeviceId());
            commander.deviceInfoQuery(device);
            sync(device);
            deviceMapper.add(device);
        }else {
            deviceMapper.update(device);
        }
        redisCatchStorage.updateDevice(device);
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
        dynamicTask.stop(registerExpireTaskKey);
        dynamicTask.startDelay(registerExpireTaskKey, ()->{
            offline(device.getDeviceId());
        }, device.getExpires() * 1000);
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
        logger.info("移除目录订阅: {}", device.getDeviceId());
        dynamicTask.stop(device.getDeviceId() + "catalog");
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
        logger.info("移除移动位置订阅: {}", device.getDeviceId());
        dynamicTask.stop(device.getDeviceId() + "mobile_position");
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
        Date registerTimeDate;
        try {
            registerTimeDate = DateUtil.format.parse(device.getRegisterTime());
        } catch (ParseException e) {
            logger.error("设备时间格式化失败：{}->{} ", device.getDeviceId(), device.getRegisterTime() );
            return false;
        }
        int expires = device.getExpires();
        Calendar calendarForExpire = Calendar.getInstance();
        calendarForExpire.setTime(registerTimeDate);
        calendarForExpire.set(Calendar.SECOND, calendarForExpire.get(Calendar.SECOND) + expires);
        return calendarForExpire.before(DateUtil.getNow());
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
}
