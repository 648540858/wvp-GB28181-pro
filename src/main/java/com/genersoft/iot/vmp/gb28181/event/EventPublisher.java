package com.genersoft.iot.vmp.gb28181.event;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.event.alarm.AlarmEvent;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.event.subscribe.mobilePosition.MobilePositionEvent;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOfflineEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOnlineEvent;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**    
 * @description:Event事件通知推送器，支持推送在线事件、离线事件
 * @author: swwheihei
 * @date:   2020年5月6日 上午11:30:50     
 */
@Component
public class EventPublisher {

	@Autowired
    private ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private IRedisRpcService redisRpcService;

	/**
	 * 设备报警事件
	 * @param deviceAlarm
	 */
	public void deviceAlarmEventPublish(DeviceAlarm deviceAlarm) {
		AlarmEvent alarmEvent = new AlarmEvent(this);
		alarmEvent.setAlarmInfo(deviceAlarm);
		applicationEventPublisher.publishEvent(alarmEvent);
	}

	public void mediaServerOfflineEventPublish(MediaServer mediaServer){
		MediaServerOfflineEvent outEvent = new MediaServerOfflineEvent(this);
		outEvent.setMediaServer(mediaServer);
		applicationEventPublisher.publishEvent(outEvent);
	}

	public void mediaServerOnlineEventPublish(MediaServer mediaServer) {
		MediaServerOnlineEvent outEvent = new MediaServerOnlineEvent(this);
		outEvent.setMediaServer(mediaServer);
		applicationEventPublisher.publishEvent(outEvent);
	}


	public void catalogEventPublish(Platform platform, CommonGBChannel deviceChannel, String type) {
		List<CommonGBChannel> deviceChannelList = new ArrayList<>();
		deviceChannelList.add(deviceChannel);
		catalogEventPublish(platform, deviceChannelList, type);
	}

	public void catalogEventPublish(Platform platform, List<CommonGBChannel> deviceChannels, String type) {
		catalogEventPublish(platform, deviceChannels, type, true);
	}
	public void catalogEventPublish(Platform platform, List<CommonGBChannel> deviceChannels, String type, boolean share) {
		if (platform != null && !userSetting.getServerId().equals(platform.getServerId())) {
			// 指定了上级平台的推送，则发送到指定的设备，未指定的则全部发送， 接收后各自处理自己的
			CatalogEvent outEvent = new CatalogEvent(this);
			outEvent.setChannels(deviceChannels);
			outEvent.setType(type);
			outEvent.setPlatform(platform);
			redisRpcService.catalogEventPublish(platform.getServerId(), outEvent);
			return;
		}
		CatalogEvent outEvent = new CatalogEvent(this);
		List<CommonGBChannel> channels = new ArrayList<>();
		if (deviceChannels.size() > 1) {
			// 数据去重
			Set<String> gbIdSet = new HashSet<>();
			for (CommonGBChannel deviceChannel : deviceChannels) {
				if (deviceChannel != null && deviceChannel.getGbDeviceId() != null && !gbIdSet.contains(deviceChannel.getGbDeviceId())) {
					gbIdSet.add(deviceChannel.getGbDeviceId());
					channels.add(deviceChannel);
				}
			}
		}else {
			channels = deviceChannels;
		}
		outEvent.setChannels(channels);
		outEvent.setType(type);
		outEvent.setPlatform(platform);
		applicationEventPublisher.publishEvent(outEvent);
		if (platform == null && share) {
			// 如果没指定上级平台，则推送消息到所有在线的wvp处理自己含有的平台的目录更新
			redisRpcService.catalogEventPublish(null, outEvent);
		}
	}

	public void mobilePositionEventPublish(MobilePosition mobilePosition) {
		MobilePositionEvent event = new MobilePositionEvent(this);
		event.setMobilePosition(mobilePosition);
		applicationEventPublisher.publishEvent(event);
	}


}
