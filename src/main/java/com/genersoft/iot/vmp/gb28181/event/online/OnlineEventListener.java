package com.genersoft.iot.vmp.gb28181.event.online;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.service.IDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * @description: 在线事件监听器，监听到离线后，修改设备离在线状态。 设备在线有两个来源：
 *               1、设备主动注销，发送注销指令
 *               2、设备未知原因离线，心跳超时
 * @author: swwheihei
 * @date: 2020年5月6日 下午1:51:23
 */
@Component
public class OnlineEventListener implements ApplicationListener<OnlineEvent> {
	
	private final static Logger logger = LoggerFactory.getLogger(OnlineEventListener.class);

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private IDeviceService deviceService;
	
	@Autowired
    private RedisUtil redis;

	@Autowired
    private SipConfig sipConfig;

	@Autowired
    private UserSetting userSetting;

	@Autowired
    private EventPublisher eventPublisher;

	@Autowired
	private SIPCommander cmder;


	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Override
	public void onApplicationEvent(OnlineEvent event) {

		logger.info("设备上线事件触发，deviceId：" + event.getDevice().getDeviceId() + ",from:" + event.getFrom());
		Device device = event.getDevice();
		if (device == null) {
			return;
		}
		String key = VideoManagerConstants.KEEPLIVEKEY_PREFIX + userSetting.getServerId() + "_" + event.getDevice().getDeviceId();
		Device deviceInStore = storager.queryVideoDevice(device.getDeviceId());
		device.setOnline(1);
		switch (event.getFrom()) {
		// 注册时触发的在线事件，先在redis中增加超时超时监听
		case VideoManagerConstants.EVENT_ONLINE_REGISTER:
			// 超时时间
			redis.set(key, event.getDevice().getDeviceId(), sipConfig.getKeepaliveTimeOut());
			device.setRegisterTime(format.format(System.currentTimeMillis()));
			if (deviceInStore == null) { //第一次上线
				logger.info("[{}] 首次注册，查询设备信息以及通道信息", device.getDeviceId());
				cmder.deviceInfoQuery(device);
				deviceService.sync(device);
			}
			break;
		// 设备主动发送心跳触发的在线事件
		case VideoManagerConstants.EVENT_ONLINE_KEEPLIVE:
			boolean exist = redis.hasKey(key);
			// 先判断是否还存在，当设备先心跳超时后又发送心跳时，redis没有监听，需要增加
			if (!exist) {
				redis.set(key, event.getDevice().getDeviceId(), sipConfig.getKeepaliveTimeOut());
			} else {
				redis.expire(key, sipConfig.getKeepaliveTimeOut());
			}
			device.setKeepaliveTime(format.format(System.currentTimeMillis()));
			break;
		// 设备主动发送消息触发的在线事件
		case VideoManagerConstants.EVENT_ONLINE_MESSAGE:

			break;
		}
		// 处理上线监听
		storager.updateDevice(device);
		// 上线添加订阅
		if (device.getSubscribeCycleForCatalog() > 0) {
			// 查询在线设备那些开启了订阅，为设备开启定时的目录订阅
			deviceService.addCatalogSubscribe(device);
		}
		if (device.getSubscribeCycleForMobilePosition() > 0) {
			deviceService.addMobilePositionSubscribe(device);
		}
	}
}
