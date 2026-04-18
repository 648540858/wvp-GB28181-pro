package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceMobilePosition;
import com.genersoft.iot.vmp.gb28181.bean.HandlerCatchData;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SIP命令类型： NOTIFY请求中的移动位置请求处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyRequestForMobilePositionProcessor extends SIPRequestProcessorParent {

	private final ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

	private final UserSetting userSetting;

	private final EventPublisher eventPublisher;

	private final IRedisCatchStorage redisCatchStorage;

	public void process(RequestEvent evt) {
		if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
			log.error("[notify-移动位置] 待处理消息队列已满 {}，返回486 BUSY_HERE，消息不做处理", userSetting.getMaxNotifyCountQueue());
			return;
		}
		taskQueue.offer(new HandlerCatchData(evt, null, null));
	}

	@Scheduled(fixedDelay = 200)
	@Async
	public void executeTaskQueue() {
		if (taskQueue.isEmpty()) {
			return;
		}
		List<HandlerCatchData> handlerCatchDataList = new ArrayList<>();
		int size = taskQueue.size();
		for (int i = 0; i < size; i++) {
			HandlerCatchData poll = taskQueue.poll();
			if (poll != null) {
				handlerCatchDataList.add(poll);
			}
		}
		if (handlerCatchDataList.isEmpty()) {
			return;
		}
		List<DeviceMobilePosition> mobilePositionList = new ArrayList<>();
		for (HandlerCatchData take : handlerCatchDataList) {
			RequestEvent evt = take.getEvt();
			try {
				FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
				String deviceId = SipUtils.getUserIdFromFromHeader(fromHeader);
				Device device = redisCatchStorage.getDevice(deviceId);
				if (device == null) {
					log.error("[notify-移动位置] 未获取到device, {}", deviceId);
					continue;
				}
				Element rootElement = getRootElement(evt, device.getCharset());
				if (rootElement == null) {
					log.warn("[notify-移动位置] {}处理失败，未识别到信息体", deviceId);
					continue;
				}
				List<DeviceMobilePosition> mobilePositions = DeviceMobilePosition.decode(device, rootElement);
				for (DeviceMobilePosition mobilePosition : mobilePositions) {
					log.info("[收到移动位置订阅通知]：{}/{}->{}.{}, 时间： {}", device.getDeviceId(), mobilePosition.getChannelDeviceId(),
							mobilePosition.getLongitude(), mobilePosition.getLatitude(), mobilePosition.getTimestamp());
					mobilePositionList.add(mobilePosition);
				}
			} catch (Exception e) {
				log.warn("[notify-移动位置] 发现未处理的异常, \r\n{}", evt.getRequest());
				log.error("[notify-移动位置] 异常内容： ", e);
			}
		}
		if (!mobilePositionList.isEmpty()) {
			try {
				eventPublisher.mobilePositionsEventPublish(mobilePositionList);
			} catch (Exception e) {
				log.error("[MobilePositionEvent] 发送失败：  ", e);
			}
		}
	}
}
