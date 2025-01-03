package com.genersoft.iot.vmp.gb28181.event;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.event.alarm.AlarmEvent;
import com.genersoft.iot.vmp.gb28181.event.device.RequestTimeoutEvent;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.event.subscribe.mobilePosition.MobilePositionEvent;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOfflineEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOnlineEvent;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.TimeoutEvent;
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


	public void requestTimeOut(TimeoutEvent timeoutEvent) {
		RequestTimeoutEvent requestTimeoutEvent = new RequestTimeoutEvent(this);
		requestTimeoutEvent.setTimeoutEvent(timeoutEvent);
		applicationEventPublisher.publishEvent(requestTimeoutEvent);
	}

	public void catalogEventPublish(Platform platform, CommonGBChannel deviceChannel, String type) {
		List<CommonGBChannel> deviceChannelList = new ArrayList<>();
		deviceChannelList.add(deviceChannel);
		catalogEventPublish(platform, deviceChannelList, type);
	}

	public void catalogEventPublish(Platform platform, List<CommonGBChannel> deviceChannels, String type) {
		if (!userSetting.getServerId().equals(platform.getServerId())) {
			List<Integer> ids = new ArrayList<>();
			for (int i = 0; i < deviceChannels.size(); i++) {
				ids.add(deviceChannels.get(i).getGbId());
			}
			redisRpcService.catalogEventPublish(platform.getServerId(), platform.getId(), ids, type);
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
	}

	public void mobilePositionEventPublish(MobilePosition mobilePosition) {
		MobilePositionEvent event = new MobilePositionEvent(this);
		event.setMobilePosition(mobilePosition);
		applicationEventPublisher.publishEvent(event);
	}


}
