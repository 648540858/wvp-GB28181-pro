package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.conf.CivilCodeFileConf;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SIP命令类型： NOTIFY请求中的移动位置请求处理
 */
@Component
public class NotifyRequestForMobilePositionProcessor extends SIPRequestProcessorParent {


    private final static Logger logger = LoggerFactory.getLogger(NotifyRequestForMobilePositionProcessor.class);

	private final Map<String, DeviceChannel> updateChannelMap = new ConcurrentHashMap<>();

	private final List<MobilePosition> addMobilePositionList = new CopyOnWriteArrayList();


	@Autowired
	private UserSetting userSetting;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IDeviceChannelService deviceChannelService;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private CivilCodeFileConf civilCodeFileConf;

	@Autowired
	private SipConfig sipConfig;

	private final static String talkKey = "notify-request-for-mobile-position-task";

	@Async("taskExecutor")
	public void process(RequestEvent evt) {
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
			MobilePosition mobilePosition = new MobilePosition();
			mobilePosition.setCreateTime(DateUtil.getNow());
			List<Element> elements = rootElement.elements();
			String channelId = null;
			for (Element element : elements) {
				switch (element.getName()){
					case "DeviceID":
						channelId = element.getStringValue();
						if (device == null) {
							device = redisCatchStorage.getDevice(channelId);
							if (device == null) {
								// 根据通道id查询设备Id
								List<Device> deviceList = deviceChannelService.getDeviceByChannelId(channelId);
								if (!deviceList.isEmpty()) {
									device = deviceList.get(0);
								}
							}
						}
						if (device == null) {
							logger.warn("[mobilePosition移动位置Notify] 未找到通道{}所属的设备", channelId);
							return;
						}
						mobilePosition.setDeviceId(device.getDeviceId());
						mobilePosition.setChannelId(channelId);
						// 兼容设备部分设备上报是通道编号与设备编号一致的情况
						if (deviceId.equals(channelId)) {
							List<DeviceChannel> deviceChannels = deviceChannelService.queryChaneListByDeviceId(deviceId);
							if (deviceChannels.size() == 1) {
								channelId = deviceChannels.get(0).getChannelId();
							}
						}
						if (!ObjectUtils.isEmpty(device.getName())) {
							mobilePosition.setDeviceName(device.getName());
						}
						mobilePosition.setDeviceId(device.getDeviceId());
						mobilePosition.setChannelId(channelId);
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
			deviceChannel.setChannelId(channelId);
			deviceChannel.setLongitude(mobilePosition.getLongitude());
			deviceChannel.setLatitude(mobilePosition.getLatitude());
			deviceChannel.setGpsTime(mobilePosition.getTime());
			updateChannelMap.put(deviceId + channelId, deviceChannel);
			addMobilePositionList.add(mobilePosition);
			if(updateChannelMap.size() > 2000) {
				executeSaveChannel();
			}
			if (userSetting.isSavePositionHistory()) {
				if(addMobilePositionList.size() > 2000) {
					executeSaveMobilePosition();
				}
			}

//			deviceChannel = deviceChannelService.updateGps(deviceChannel, device);
//
//			mobilePosition.setLongitudeWgs84(deviceChannel.getLongitudeWgs84());
//			mobilePosition.setLatitudeWgs84(deviceChannel.getLatitudeWgs84());
//			mobilePosition.setLongitudeGcj02(deviceChannel.getLongitudeGcj02());
//			mobilePosition.setLatitudeGcj02(deviceChannel.getLatitudeGcj02());

//			deviceChannelService.updateChannelGPS(device, deviceChannel, mobilePosition);

			if (!dynamicTask.contains(talkKey)) {
				dynamicTask.startDelay(talkKey, this::executeSave, 3000);
			}

		} catch (DocumentException e) {
			logger.error("未处理的异常 ", e);
		}


	}

	private void executeSave(){
		executeSaveChannel();
		executeSaveMobilePosition();
		dynamicTask.stop(talkKey);
	}

	@Async("taskExecutor")
	public void executeSaveChannel(){
		dynamicTask.execute();
		try {
			logger.info("[移动位置订阅]更新通道位置： {}", updateChannelMap.size());
			ArrayList<DeviceChannel> deviceChannels = new ArrayList<>(updateChannelMap.values());
			deviceChannelService.batchUpdateChannelGPS(deviceChannels);
			updateChannelMap.clear();
		}catch (Exception e) {

		}
	}
	@Async("taskExecutor")
	public void executeSaveMobilePosition(){
		if (userSetting.isSavePositionHistory()) {
			try {
				logger.info("[移动位置订阅] 添加通道轨迹点位： {}", addMobilePositionList.size());
				deviceChannelService.batchAddMobilePosition(addMobilePositionList);
				addMobilePositionList.clear();
			}catch (Exception e) {
				logger.info("[移动位置订阅] b添加通道轨迹点位保存失败： {}", addMobilePositionList.size());
			}
		}
	}

}
