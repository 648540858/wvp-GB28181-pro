package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.Coordtransform;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SIP命令类型： NOTIFY请求
 */
@Component
public class NotifyRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {


    private final static Logger logger = LoggerFactory.getLogger(NotifyRequestProcessor.class);

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private SipConfig sipConfig;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private EventPublisher publisher;

	private final String method = "NOTIFY";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	private boolean taskQueueHandlerRun = false;

	private final ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

	@Qualifier("taskExecutor")
	@Autowired
	private ThreadPoolTaskExecutor taskExecutor;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	@Override
	public void process(RequestEvent evt) {
		try {

			taskQueue.offer(new HandlerCatchData(evt, null, null));
			responseAck(evt, Response.OK);
			if (!taskQueueHandlerRun) {
				taskQueueHandlerRun = true;
				taskExecutor.execute(()-> {
							while (!taskQueue.isEmpty()) {
								try {
									HandlerCatchData take = taskQueue.poll();
									Element rootElement = getRootElement(take.getEvt());
									String cmd = XmlUtil.getText(rootElement, "CmdType");

									if (CmdType.CATALOG.equals(cmd)) {
										logger.info("接收到Catalog通知");
										processNotifyCatalogList(take.getEvt());
									} else if (CmdType.ALARM.equals(cmd)) {
										logger.info("接收到Alarm通知");
										processNotifyAlarm(take.getEvt());
									} else if (CmdType.MOBILE_POSITION.equals(cmd)) {
										logger.info("接收到MobilePosition通知");
										processNotifyMobilePosition(take.getEvt());
									} else {
										logger.info("接收到消息：" + cmd);
									}
								} catch (DocumentException e) {
									throw new RuntimeException(e);
								}
							}
						taskQueueHandlerRun = false;
						});
			}


		} catch (SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 处理MobilePosition移动位置Notify
	 * 
	 * @param evt
	 */
	private void processNotifyMobilePosition(RequestEvent evt) {
		try {
			FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
			String deviceId = SipUtils.getUserIdFromFromHeader(fromHeader);

			// 回复 200 OK
			Element rootElement = getRootElement(evt);

			MobilePosition mobilePosition = new MobilePosition();
			Element deviceIdElement = rootElement.element("DeviceID");
			String channelId = deviceIdElement.getTextTrim().toString();
			Device device = redisCatchStorage.getDevice(deviceId);
			if (device != null) {
				if (!StringUtils.isEmpty(device.getName())) {
					mobilePosition.setDeviceName(device.getName());
				}
			}
			mobilePosition.setDeviceId(XmlUtil.getText(rootElement, "DeviceID"));
			mobilePosition.setChannelId(channelId);
			String time = XmlUtil.getText(rootElement, "Time");
			mobilePosition.setTime(time);
			mobilePosition.setLongitude(Double.parseDouble(XmlUtil.getText(rootElement, "Longitude")));
			mobilePosition.setLatitude(Double.parseDouble(XmlUtil.getText(rootElement, "Latitude")));
			if (NumericUtil.isDouble(XmlUtil.getText(rootElement, "Speed"))) {
				mobilePosition.setSpeed(Double.parseDouble(XmlUtil.getText(rootElement, "Speed")));
			} else {
				mobilePosition.setSpeed(0.0);
			}
			if (NumericUtil.isDouble(XmlUtil.getText(rootElement, "Direction"))) {
				mobilePosition.setDirection(Double.parseDouble(XmlUtil.getText(rootElement, "Direction")));
			} else {
				mobilePosition.setDirection(0.0);
			}
			if (NumericUtil.isDouble(XmlUtil.getText(rootElement, "Altitude"))) {
				mobilePosition.setAltitude(Double.parseDouble(XmlUtil.getText(rootElement, "Altitude")));
			} else {
				mobilePosition.setAltitude(0.0);
			}
			logger.info("[收到 移动位置订阅]：{}/{}->{}.{}", mobilePosition.getDeviceId(), mobilePosition.getChannelId(),
					mobilePosition.getLongitude(), mobilePosition.getLatitude());
			mobilePosition.setReportSource("Mobile Position");
			// 默认来源坐标系为WGS-84处理
			Double[] gcj02Point = Coordtransform.WGS84ToGCJ02(mobilePosition.getLongitude(), mobilePosition.getLatitude());
			logger.info("GCJ02坐标：" + gcj02Point[0] + ", " + gcj02Point[1]);
			mobilePosition.setGeodeticSystem("GCJ-02");
			mobilePosition.setCnLng(gcj02Point[0] + "");
			mobilePosition.setCnLat(gcj02Point[1] + "");
			if (!userSetting.getSavePositionHistory()) {
				storager.clearMobilePositionsByDeviceId(deviceId);
			}
			storager.insertMobilePosition(mobilePosition);
			storager.updateChannelPotion(deviceId, channelId, mobilePosition.getLongitude(), mobilePosition.getLatitude() );
			// 发送redis消息。 通知位置信息的变化
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("time", time);
			jsonObject.put("serial", deviceId);
			jsonObject.put("code", channelId);
			jsonObject.put("longitude", mobilePosition.getLongitude());
			jsonObject.put("latitude", mobilePosition.getLatitude());
			jsonObject.put("altitude", mobilePosition.getAltitude());
			jsonObject.put("direction", mobilePosition.getDirection());
			jsonObject.put("speed", mobilePosition.getSpeed());
			redisCatchStorage.sendMobilePositionMsg(jsonObject);
		} catch (DocumentException  e) {
			e.printStackTrace();
		}
	}

	/***
	 * 处理alarm设备报警Notify
	 * 
	 * @param evt
	 */
	private void processNotifyAlarm(RequestEvent evt) {
		if (!sipConfig.isAlarm()) {
			return;
		}
		try {
			Element rootElement = getRootElement(evt);
			Element deviceIdElement = rootElement.element("DeviceID");
			String deviceId = deviceIdElement.getText().toString();

			Device device = redisCatchStorage.getDevice(deviceId);
			if (device == null) {
				logger.warn("[ NotifyAlarm ] 未找到设备：{}", deviceId);
				return;
			}
			rootElement = getRootElement(evt, device.getCharset());
			DeviceAlarm deviceAlarm = new DeviceAlarm();
			deviceAlarm.setDeviceId(deviceId);
			deviceAlarm.setAlarmPriority(XmlUtil.getText(rootElement, "AlarmPriority"));
			deviceAlarm.setAlarmMethod(XmlUtil.getText(rootElement, "AlarmMethod"));
			String alarmTime = XmlUtil.getText(rootElement, "AlarmTime");
			if (alarmTime == null) {
				logger.warn("[ NotifyAlarm ] AlarmTime cannot be null");
				return;
			}
			deviceAlarm.setAlarmTime(DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(alarmTime));
			if (XmlUtil.getText(rootElement, "AlarmDescription") == null) {
				deviceAlarm.setAlarmDescription("");
			} else {
				deviceAlarm.setAlarmDescription(XmlUtil.getText(rootElement, "AlarmDescription"));
			}
			if (NumericUtil.isDouble(XmlUtil.getText(rootElement, "Longitude"))) {
				deviceAlarm.setLongitude(Double.parseDouble(XmlUtil.getText(rootElement, "Longitude")));
			} else {
				deviceAlarm.setLongitude(0.00);
			}
			if (NumericUtil.isDouble(XmlUtil.getText(rootElement, "Latitude"))) {
				deviceAlarm.setLatitude(Double.parseDouble(XmlUtil.getText(rootElement, "Latitude")));
			} else {
				deviceAlarm.setLatitude(0.00);
			}
			logger.info("[收到Notify-Alarm]：{}/{}", device.getDeviceId(), deviceAlarm.getChannelId());
			if ("4".equals(deviceAlarm.getAlarmMethod())) {
				MobilePosition mobilePosition = new MobilePosition();
				mobilePosition.setDeviceId(deviceAlarm.getDeviceId());
				mobilePosition.setTime(deviceAlarm.getAlarmTime());
				mobilePosition.setLongitude(deviceAlarm.getLongitude());
				mobilePosition.setLatitude(deviceAlarm.getLatitude());
				mobilePosition.setReportSource("GPS Alarm");
				// 默认来源坐标系为WGS-84处理
				Double[] gcj02Point = Coordtransform.WGS84ToGCJ02(mobilePosition.getLongitude(), mobilePosition.getLatitude());
				logger.info("GCJ02坐标：" + gcj02Point[0] + ", " + gcj02Point[1]);
				mobilePosition.setGeodeticSystem("GCJ-02");
				mobilePosition.setCnLng(gcj02Point[0] + "");
				mobilePosition.setCnLat(gcj02Point[1] + "");
				if (!userSetting.getSavePositionHistory()) {
					storager.clearMobilePositionsByDeviceId(deviceId);
				}
				storager.insertMobilePosition(mobilePosition);
			}
			// TODO: 需要实现存储报警信息、报警分类

			// 回复200 OK
			if (redisCatchStorage.deviceIsOnline(deviceId)) {
				publisher.deviceAlarmEventPublish(deviceAlarm);
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 处理catalog设备目录列表Notify
	 * 
	 * @param evt
	 */
	private void processNotifyCatalogList(RequestEvent evt) {
		try {
			FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
			String deviceId = SipUtils.getUserIdFromFromHeader(fromHeader);

			Device device = redisCatchStorage.getDevice(deviceId);
			if (device == null || device.getOnline() == 0) {
				logger.warn("[收到 目录订阅]：{}, 但是设备已经离线", (device != null ? device.getDeviceId():"" ));
				return;
			}
			Element rootElement = getRootElement(evt, device.getCharset());
			Element deviceListElement = rootElement.element("DeviceList");
			if (deviceListElement == null) {
				return;
			}
			Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
			if (deviceListIterator != null) {

				// 遍历DeviceList
				while (deviceListIterator.hasNext()) {
					Element itemDevice = deviceListIterator.next();
					Element channelDeviceElement = itemDevice.element("DeviceID");
					if (channelDeviceElement == null) {
						continue;
					}
					Element eventElement = itemDevice.element("Event");
					String event;
					if (eventElement == null) {
						logger.warn("[收到 目录订阅]：{}, 但是Event为空, 设为默认值 ADD", (device != null ? device.getDeviceId():"" ));
						event = CatalogEvent.ADD;
					}else {
						event = eventElement.getText().toUpperCase();
					}
					DeviceChannel channel = XmlUtil.channelContentHander(itemDevice, device);
					channel.setDeviceId(device.getDeviceId());
					logger.info("[收到 目录订阅]：{}/{}", device.getDeviceId(), channel.getChannelId());
					switch (event) {
						case CatalogEvent.ON:
							// 上线
							logger.info("收到来自设备【{}】的通道【{}】上线通知", device.getDeviceId(), channel.getChannelId());
							storager.deviceChannelOnline(deviceId, channel.getChannelId());
							break;
						case CatalogEvent.OFF :
							// 离线
							logger.info("收到来自设备【{}】的通道【{}】离线通知", device.getDeviceId(), channel.getChannelId());
							storager.deviceChannelOffline(deviceId, channel.getChannelId());
							break;
						case CatalogEvent.VLOST:
							// 视频丢失
							logger.info("收到来自设备【{}】的通道【{}】视频丢失通知", device.getDeviceId(), channel.getChannelId());
							storager.deviceChannelOffline(deviceId, channel.getChannelId());
							break;
						case CatalogEvent.DEFECT:
							// 故障
							break;
						case CatalogEvent.ADD:
							// 增加
							logger.info("收到来自设备【{}】的增加通道【{}】通知", device.getDeviceId(), channel.getChannelId());
							storager.updateChannel(deviceId, channel);
							break;
						case CatalogEvent.DEL:
							// 删除
							logger.info("收到来自设备【{}】的删除通道【{}】通知", device.getDeviceId(), channel.getChannelId());
							storager.delChannel(deviceId, channel.getChannelId());
							break;
						case CatalogEvent.UPDATE:
							// 更新
							logger.info("收到来自设备【{}】的更新通道【{}】通知", device.getDeviceId(), channel.getChannelId());
							storager.updateChannel(deviceId, channel);
							break;
						default:
							logger.warn("[ NotifyCatalog ] event not found ： {}", event );

					}
					// 转发变化信息
					eventPublisher.catalogEventPublish(null, channel, event);

				}
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}

	public void setCmder(SIPCommander cmder) {
	}

	public void setStorager(IVideoManagerStorage storager) {
		this.storager = storager;
	}

	public void setPublisher(EventPublisher publisher) {
		this.publisher = publisher;
	}

	public void setRedis(RedisUtil redis) {
	}

	public void setDeferredResultHolder(DeferredResultHolder deferredResultHolder) {
	}

	public IRedisCatchStorage getRedisCatchStorage() {
		return redisCatchStorage;
	}

	public void setRedisCatchStorage(IRedisCatchStorage redisCatchStorage) {
		this.redisCatchStorage = redisCatchStorage;
	}
}
