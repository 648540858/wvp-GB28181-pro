package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.HandlerCatchData;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IMobilePositionService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

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
public class NotifyRequestForMobilePositionProcessor extends SIPRequestProcessorParent {

	private ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IDeviceChannelService deviceChannelService;

	@Autowired
	private IMobilePositionService mobilePositionService;

	public void process(RequestEvent evt) {

		if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
			log.error("[notify-移动位置] 待处理消息队列已满 {}，返回486 BUSY_HERE，消息不做处理", userSetting.getMaxNotifyCountQueue());
			return;
		}
		taskQueue.offer(new HandlerCatchData(evt, null, null));
	}

	@Scheduled(fixedRate = 200) //每200毫秒执行一次
	public void executeTaskQueue() {
		if (taskQueue.isEmpty()) {
			return;
		}
		List<HandlerCatchData> handlerCatchDataList = new ArrayList<>();
		while (!taskQueue.isEmpty()) {
			handlerCatchDataList.add(taskQueue.poll());
		}
		if (handlerCatchDataList.isEmpty()) {
			return;
		}
		for (HandlerCatchData take : handlerCatchDataList) {
			if (take == null) {
				continue;
			}
			RequestEvent evt = take.getEvt();
			try {
				FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
				String deviceId = SipUtils.getUserIdFromFromHeader(fromHeader);
				long startTime = System.currentTimeMillis();
				// 回复 200 OK
				Element rootElement = getRootElement(evt);
				if (rootElement == null) {
					log.error("处理MobilePosition移动位置Notify时未获取到消息体,{}", evt.getRequest());
					continue;
				}
				Device device = redisCatchStorage.getDevice(deviceId);
				if (device == null) {
					log.error("处理MobilePosition移动位置Notify时未获取到device,{}", deviceId);
					continue;
				}
				MobilePosition mobilePosition = new MobilePosition();
				mobilePosition.setDeviceId(device.getDeviceId());
				mobilePosition.setDeviceName(device.getName());
				mobilePosition.setCreateTime(DateUtil.getNow());

				DeviceChannel deviceChannel = null;
				List<Element> elements = rootElement.elements();
				readDocument: for (Element element : elements) {
					switch (element.getName()){
						case "DeviceID":
							String channelId = element.getStringValue();
							deviceChannel = deviceChannelService.getOne(device.getDeviceId(), channelId);
							if (deviceChannel != null) {
								mobilePosition.setChannelId(deviceChannel.getId());
							}else {
								log.error("[notify-移动位置] 未找到通道 {}/{}", device.getDeviceId(), channelId);
								break readDocument;
							}
							break;
						case "Time":
							String timeVal = element.getStringValue();
							if (ObjectUtils.isEmpty(timeVal)) {
								mobilePosition.setTime(DateUtil.getNow());
							} else {
								mobilePosition.setTime(SipUtils.parseTime(timeVal));
							}
							break;
						case "Longitude":
							mobilePosition.setLongitude(Double.parseDouble(element.getStringValue()));
							break;
						case "Latitude":
							mobilePosition.setLatitude(Double.parseDouble(element.getStringValue()));
							break;
						case "Speed":
							String speedVal = element.getStringValue();
							if (NumericUtil.isDouble(speedVal)) {
								mobilePosition.setSpeed(Double.parseDouble(speedVal));
							} else {
								mobilePosition.setSpeed(0.0);
							}
							break;
						case "Direction":
							String directionVal = element.getStringValue();
							if (NumericUtil.isDouble(directionVal)) {
								mobilePosition.setDirection(Double.parseDouble(directionVal));
							} else {
								mobilePosition.setDirection(0.0);
							}
							break;
						case "Altitude":
							String altitudeVal = element.getStringValue();
							if (NumericUtil.isDouble(altitudeVal)) {
								mobilePosition.setAltitude(Double.parseDouble(altitudeVal));
							} else {
								mobilePosition.setAltitude(0.0);
							}
							break;

					}
				}
				if (deviceChannel == null) {
					continue;
				}

				log.info("[收到移动位置订阅通知]：{}/{}->{}.{}, 时间： {}", mobilePosition.getDeviceId(), mobilePosition.getChannelId(),
					mobilePosition.getLongitude(), mobilePosition.getLatitude(), System.currentTimeMillis() - startTime);
				mobilePosition.setReportSource("Mobile Position");

				mobilePositionService.add(mobilePosition);
				// 向关联了该通道并且开启移动位置订阅的上级平台发送移动位置订阅消息
				try {
					eventPublisher.mobilePositionEventPublish(mobilePosition);
				}catch (Exception e) {
					log.error("[向上级转发移动位置失败] ", e);
				}
				if (mobilePosition.getChannelId() == null) {
					List<DeviceChannel> channels = deviceChannelService.queryChaneListByDeviceId(mobilePosition.getDeviceId());
					channels.forEach(channel -> {
						// 发送redis消息。 通知位置信息的变化
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("time", DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(mobilePosition.getTime()));
						jsonObject.put("serial", device.getDeviceId());
						jsonObject.put("code", channel.getDeviceId());
						jsonObject.put("longitude", mobilePosition.getLongitude());
						jsonObject.put("latitude", mobilePosition.getLatitude());
						jsonObject.put("altitude", mobilePosition.getAltitude());
						jsonObject.put("direction", mobilePosition.getDirection());
						jsonObject.put("speed", mobilePosition.getSpeed());
						redisCatchStorage.sendMobilePositionMsg(jsonObject);
					});
				}else {
					// 发送redis消息。 通知位置信息的变化
					if (deviceChannel != null) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("time", DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(mobilePosition.getTime()));
						jsonObject.put("serial", mobilePosition.getDeviceId());
						jsonObject.put("code", deviceChannel.getDeviceId());
						jsonObject.put("longitude", mobilePosition.getLongitude());
						jsonObject.put("latitude", mobilePosition.getLatitude());
						jsonObject.put("altitude", mobilePosition.getAltitude());
						jsonObject.put("direction", mobilePosition.getDirection());
						jsonObject.put("speed", mobilePosition.getSpeed());
						redisCatchStorage.sendMobilePositionMsg(jsonObject);
					}
				}
			} catch (DocumentException e) {
				log.error("未处理的异常 ", e);
			}
		}
	}
//	@Scheduled(fixedRate = 10000)
//	public void execute(){
//		logger.debug("[待处理Notify-移动位置订阅消息数量]: {}", taskQueue.size());
//	}
}
