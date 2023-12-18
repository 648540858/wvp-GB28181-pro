package com.genersoft.iot.vmp.gb28181.event;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.device.RequestTimeoutEvent;
import com.genersoft.iot.vmp.gb28181.event.record.RecordEndEvent;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.media.zlm.event.ZLMOfflineEvent;
import com.genersoft.iot.vmp.media.zlm.event.ZLMOnlineEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.gb28181.event.alarm.AlarmEvent;

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
	
	/**
	 * 设备报警事件
	 * @param deviceAlarm
	 */
	public void deviceAlarmEventPublish(DeviceAlarm deviceAlarm) {
		AlarmEvent alarmEvent = new AlarmEvent(this);
		alarmEvent.setAlarmInfo(deviceAlarm);
		applicationEventPublisher.publishEvent(alarmEvent);
	}

	public void zlmOfflineEventPublish(String mediaServerId){
		ZLMOfflineEvent outEvent = new ZLMOfflineEvent(this);
		outEvent.setMediaServerId(mediaServerId);
		applicationEventPublisher.publishEvent(outEvent);
	}

	public void zlmOnlineEventPublish(String mediaServerId) {
		ZLMOnlineEvent outEvent = new ZLMOnlineEvent(this);
		outEvent.setMediaServerId(mediaServerId);
		applicationEventPublisher.publishEvent(outEvent);
	}


	public void catalogEventPublish(Integer platformId, CommonGbChannel channel, String type) {
		List<CommonGbChannel> channelList = new ArrayList<>();
		channelList.add(channel);
		catalogEventPublish(platformId, channelList, type);
	}


	public void requestTimeOut(TimeoutEvent timeoutEvent) {
		RequestTimeoutEvent requestTimeoutEvent = new RequestTimeoutEvent(this);
		requestTimeoutEvent.setTimeoutEvent(timeoutEvent);
		applicationEventPublisher.publishEvent(requestTimeoutEvent);
	}


	/**
	 *
	 * @param platformId
	 * @param channels
	 * @param type
	 */
	public void catalogEventPublish(Integer platformId, List<CommonGbChannel> channels, String type) {
		CatalogEvent outEvent = new CatalogEvent(this);
		outEvent.setChannels(channels);
		outEvent.setType(type);
		outEvent.setPlatformId(platformId);
		applicationEventPublisher.publishEvent(outEvent);
	}

	public void recordEndEventPush(RecordInfo recordInfo) {
		RecordEndEvent outEvent = new RecordEndEvent(this);
		outEvent.setRecordInfo(recordInfo);
		applicationEventPublisher.publishEvent(outEvent);
	}

}
