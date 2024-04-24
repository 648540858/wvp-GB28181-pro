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
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SIP命令类型： NOTIFY请求中的移动位置请求处理
 */
@Component
public class NotifyRequestForMobilePositionProcessor extends SIPRequestProcessorParent {


    private final static Logger logger = LoggerFactory.getLogger(NotifyRequestForMobilePositionProcessor.class);

	private ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IDeviceChannelService deviceChannelService;

	public void process(RequestEvent evt) {

		if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
			logger.error("[notify-移动位置] 待处理消息队列已满 {}，返回486 BUSY_HERE，消息不做处理", userSetting.getMaxNotifyCountQueue());
			return;
		}
		taskQueue.offer(new HandlerCatchData(evt, null, null));
	}

	@Scheduled(fixedRate = 200) //每200毫秒执行一次
	@Transactional
	public void executeTaskQueue() {
		if (taskQueue.isEmpty()) {
			return;
		}
		Map<String, DeviceChannel> updateChannelMap = new ConcurrentHashMap<>();
		List<MobilePosition> addMobilePositionList = new ArrayList<>();
		for (HandlerCatchData take : taskQueue) {
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
					logger.error("处理MobilePosition移动位置Notify时未获取到消息体,{}", evt.getRequest());
					return;
				}
				Device device = redisCatchStorage.getDevice(deviceId);
				if (device == null) {
					logger.error("处理MobilePosition移动位置Notify时未获取到device,{}", deviceId);
					return;
				}
				MobilePosition mobilePosition = new MobilePosition();
				mobilePosition.setDeviceId(device.getDeviceId());
				mobilePosition.setDeviceName(device.getName());
				mobilePosition.setCreateTime(DateUtil.getNow());
				List<Element> elements = rootElement.elements();
				for (Element element : elements) {
					switch (element.getName()){
						case "DeviceID":
							String channelId = element.getStringValue();
							if (!deviceId.equals(channelId)) {
								mobilePosition.setChannelId(channelId);
							}
							continue;
						case "Time":
							String timeVal = element.getStringValue();
							if (ObjectUtils.isEmpty(timeVal)) {
								mobilePosition.setTime(DateUtil.getNow());
							} else {
								mobilePosition.setTime(SipUtils.parseTime(timeVal));
							}
							continue;
						case "Longitude":
							mobilePosition.setLongitude(Double.parseDouble(element.getStringValue()));
							continue;
						case "Latitude":
							mobilePosition.setLatitude(Double.parseDouble(element.getStringValue()));
							continue;
						case "Speed":
							String speedVal = element.getStringValue();
							if (NumericUtil.isDouble(speedVal)) {
								mobilePosition.setSpeed(Double.parseDouble(speedVal));
							} else {
								mobilePosition.setSpeed(0.0);
							}
							continue;
						case "Direction":
							String directionVal = element.getStringValue();
							if (NumericUtil.isDouble(directionVal)) {
								mobilePosition.setDirection(Double.parseDouble(directionVal));
							} else {
								mobilePosition.setDirection(0.0);
							}
							continue;
						case "Altitude":
							String altitudeVal = element.getStringValue();
							if (NumericUtil.isDouble(altitudeVal)) {
								mobilePosition.setAltitude(Double.parseDouble(altitudeVal));
							} else {
								mobilePosition.setAltitude(0.0);
							}
							continue;

					}
				}

//			logger.info("[收到移动位置订阅通知]：{}/{}->{}.{}, 时间： {}", mobilePosition.getDeviceId(), mobilePosition.getChannelId(),
//					mobilePosition.getLongitude(), mobilePosition.getLatitude(), System.currentTimeMillis() - startTime);
				mobilePosition.setReportSource("Mobile Position");

				// 更新device channel 的经纬度
				DeviceChannel deviceChannel = new DeviceChannel();
				deviceChannel.setDeviceId(device.getDeviceId());
				deviceChannel.setLongitude(mobilePosition.getLongitude());
				deviceChannel.setLatitude(mobilePosition.getLatitude());
				deviceChannel.setGpsTime(mobilePosition.getTime());
				updateChannelMap.put(deviceId + mobilePosition.getChannelId(), deviceChannel);
				addMobilePositionList.add(mobilePosition);


				// 向关联了该通道并且开启移动位置订阅的上级平台发送移动位置订阅消息
				try {
					eventPublisher.mobilePositionEventPublish(mobilePosition);
				}catch (Exception e) {
					logger.error("[向上级转发移动位置失败] ", e);
				}
				if (mobilePosition.getChannelId().equals(mobilePosition.getDeviceId()) || mobilePosition.getChannelId() == null) {
					List<DeviceChannel> channels = deviceChannelService.queryChaneListByDeviceId(mobilePosition.getDeviceId());
					channels.forEach(channel -> {
						// 发送redis消息。 通知位置信息的变化
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("time", DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(mobilePosition.getTime()));
						jsonObject.put("serial", channel.getDeviceId());
						jsonObject.put("code", channel.getChannelId());
						jsonObject.put("longitude", mobilePosition.getLongitude());
						jsonObject.put("latitude", mobilePosition.getLatitude());
						jsonObject.put("altitude", mobilePosition.getAltitude());
						jsonObject.put("direction", mobilePosition.getDirection());
						jsonObject.put("speed", mobilePosition.getSpeed());
						redisCatchStorage.sendMobilePositionMsg(jsonObject);
					});
				}else {
					// 发送redis消息。 通知位置信息的变化
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("time", DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(mobilePosition.getTime()));
					jsonObject.put("serial", mobilePosition.getDeviceId());
					jsonObject.put("code", mobilePosition.getChannelId());
					jsonObject.put("longitude", mobilePosition.getLongitude());
					jsonObject.put("latitude", mobilePosition.getLatitude());
					jsonObject.put("altitude", mobilePosition.getAltitude());
					jsonObject.put("direction", mobilePosition.getDirection());
					jsonObject.put("speed", mobilePosition.getSpeed());
					redisCatchStorage.sendMobilePositionMsg(jsonObject);
				}
			} catch (DocumentException e) {
				logger.error("未处理的异常 ", e);
			}
		}
		taskQueue.clear();
		if(!updateChannelMap.isEmpty()) {
			List<DeviceChannel>  channels = new ArrayList<>(updateChannelMap.values());
			logger.info("[移动位置订阅]更新通道位置： {}", channels.size());
			deviceChannelService.batchUpdateChannel(channels);
			updateChannelMap.clear();
		}
		if (userSetting.isSavePositionHistory() && !addMobilePositionList.isEmpty()) {
			try {
				logger.info("[移动位置订阅] 添加通道轨迹点位： {}", addMobilePositionList.size());
				deviceChannelService.batchAddMobilePosition(addMobilePositionList);
			}catch (Exception e) {
				logger.info("[移动位置订阅] b添加通道轨迹点位保存失败： {}", addMobilePositionList.size());
			}
			addMobilePositionList.clear();
		}
	}
	@Scheduled(fixedRate = 10000)
	public void execute(){
		logger.info("[待处理Notify-移动位置订阅消息数量]: {}", taskQueue.size());
	}
}
