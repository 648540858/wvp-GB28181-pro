package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.DeviceOffLineDetector;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.utils.GpsUtil;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Iterator;

/**
 * SIP命令类型： NOTIFY请求
 */
@Component
public class NotifyRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {


    private final static Logger logger = LoggerFactory.getLogger(NotifyRequestProcessor.class);

	@Autowired
	private UserSetup userSetup;

	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private SipConfig sipConfig;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private EventPublisher publisher;

	@Autowired
	private DeviceOffLineDetector offLineDetector;


	private String method = "NOTIFY";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	@Override
	public void process(RequestEvent evt) {
		try {
			Element rootElement = getRootElement(evt);
			String cmd = XmlUtil.getText(rootElement, "CmdType");

			if (CmdType.CATALOG.equals(cmd)) {
				logger.info("接收到Catalog通知");
				processNotifyCatalogList(evt);
			} else if (CmdType.ALARM.equals(cmd)) {
				logger.info("接收到Alarm通知");
				processNotifyAlarm(evt);
			} else if (CmdType.MOBILE_POSITION.equals(cmd)) {
				logger.info("接收到MobilePosition通知");
				processNotifyMobilePosition(evt);
			} else {
				logger.info("接收到消息：" + cmd);
				responseAck(evt, Response.OK);
			}
		} catch (DocumentException | SipException | InvalidArgumentException | ParseException e) {
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
			// 回复 200 OK
			Element rootElement = getRootElement(evt);
			MobilePosition mobilePosition = new MobilePosition();
			Element deviceIdElement = rootElement.element("DeviceID");
			String deviceId = deviceIdElement.getTextTrim().toString();
			Device device = redisCatchStorage.getDevice(deviceId);
			if (device != null) {
				if (!StringUtils.isEmpty(device.getName())) {
					mobilePosition.setDeviceName(device.getName());
				}
			}
			mobilePosition.setDeviceId(XmlUtil.getText(rootElement, "DeviceID"));
			mobilePosition.setTime(XmlUtil.getText(rootElement, "Time"));
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
			mobilePosition.setReportSource("Mobile Position");
			BaiduPoint bp = new BaiduPoint();
			bp = GpsUtil.Wgs84ToBd09(String.valueOf(mobilePosition.getLongitude()), String.valueOf(mobilePosition.getLatitude()));
			logger.info("百度坐标：" + bp.getBdLng() + ", " + bp.getBdLat());
			mobilePosition.setGeodeticSystem("BD-09");
			mobilePosition.setCnLng(bp.getBdLng());
			mobilePosition.setCnLat(bp.getBdLat());
			if (!userSetup.getSavePositionHistory()) {
				storager.clearMobilePositionsByDeviceId(deviceId);
			}
			storager.insertMobilePosition(mobilePosition);
			responseAck(evt, Response.OK);
		} catch (DocumentException | SipException | InvalidArgumentException | ParseException e) {
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
				return;
			}
			rootElement = getRootElement(evt, device.getCharset());
			DeviceAlarm deviceAlarm = new DeviceAlarm();
			deviceAlarm.setDeviceId(deviceId);
			deviceAlarm.setAlarmPriority(XmlUtil.getText(rootElement, "AlarmPriority"));
			deviceAlarm.setAlarmMethod(XmlUtil.getText(rootElement, "AlarmMethod"));
			deviceAlarm.setAlarmTime(XmlUtil.getText(rootElement, "AlarmTime"));
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

			if (deviceAlarm.getAlarmMethod().equals("4")) {
				MobilePosition mobilePosition = new MobilePosition();
				mobilePosition.setDeviceId(deviceAlarm.getDeviceId());
				mobilePosition.setTime(deviceAlarm.getAlarmTime());
				mobilePosition.setLongitude(deviceAlarm.getLongitude());
				mobilePosition.setLatitude(deviceAlarm.getLatitude());
				mobilePosition.setReportSource("GPS Alarm");
				BaiduPoint bp = new BaiduPoint();
				bp = GpsUtil.Wgs84ToBd09(String.valueOf(mobilePosition.getLongitude()), String.valueOf(mobilePosition.getLatitude()));
				logger.info("百度坐标：" + bp.getBdLng() + ", " + bp.getBdLat());
				mobilePosition.setGeodeticSystem("BD-09");
				mobilePosition.setCnLng(bp.getBdLng());
				mobilePosition.setCnLat(bp.getBdLat());
				if (!userSetup.getSavePositionHistory()) {
					storager.clearMobilePositionsByDeviceId(deviceId);
				}
				storager.insertMobilePosition(mobilePosition);
			}
			// TODO: 需要实现存储报警信息、报警分类

			// 回复200 OK
			responseAck(evt, Response.OK);
			if (offLineDetector.isOnline(deviceId)) {
				publisher.deviceAlarmEventPublish(deviceAlarm);
			}
		} catch (DocumentException | SipException | InvalidArgumentException | ParseException e) {
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

			Element rootElement = getRootElement(evt);
			Device device = redisCatchStorage.getDevice(deviceId);
			if (device == null) {
				return;
			}
			if (device != null ) {
				rootElement = getRootElement(evt, device.getCharset());
			}
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
					DeviceChannel channel = XmlUtil.channelContentHander(itemDevice);
					channel.setDeviceId(device.getDeviceId());
					logger.debug("收到来自设备【{}】的通道: {}【{}】", device.getDeviceId(), channel.getName(), channel.getChannelId());
					switch (eventElement.getText().toUpperCase()) {
						case CatalogEvent.ON: // 上线
							logger.info("收到来自设备【{}】的通道【{}】上线通知", device.getDeviceId(), channel.getChannelId());
							storager.deviceChannelOnline(deviceId, channel.getChannelId());
							// 回复200 OK
							responseAck(evt, Response.OK);
							break;
						case CatalogEvent.OFF : // 离线
							logger.info("收到来自设备【{}】的通道【{}】离线通知", device.getDeviceId(), channel.getChannelId());
							storager.deviceChannelOffline(deviceId, channel.getChannelId());
							// 回复200 OK
							responseAck(evt, Response.OK);
							break;
						case CatalogEvent.VLOST: // 视频丢失
							logger.info("收到来自设备【{}】的通道【{}】视频丢失通知", device.getDeviceId(), channel.getChannelId());
							storager.deviceChannelOffline(deviceId, channel.getChannelId());
							// 回复200 OK
							responseAck(evt, Response.OK);
							break;
						case CatalogEvent.DEFECT: // 故障
							// 回复200 OK
							responseAck(evt, Response.OK);
							break;
						case CatalogEvent.ADD: // 增加
							logger.info("收到来自设备【{}】的增加通道【{}】通知", device.getDeviceId(), channel.getChannelId());
							storager.updateChannel(deviceId, channel);
							responseAck(evt, Response.OK);
							break;
						case CatalogEvent.DEL: // 删除
							logger.info("收到来自设备【{}】的删除通道【{}】通知", device.getDeviceId(), channel.getChannelId());
							storager.delChannel(deviceId, channel.getChannelId());
							responseAck(evt, Response.OK);
							break;
						case CatalogEvent.UPDATE: // 更新
							logger.info("收到来自设备【{}】的更新通道【{}】通知", device.getDeviceId(), channel.getChannelId());
							storager.updateChannel(deviceId, channel);
							responseAck(evt, Response.OK);
							break;
						default:
							responseAck(evt, Response.BAD_REQUEST, "event not found");

					}
					// 转发变化信息
					eventPublisher.catalogEventPublish(null, channel, eventElement.getText().toUpperCase());

				}

				if (offLineDetector.isOnline(deviceId)) {
					publisher.onlineEventPublish(device, VideoManagerConstants.EVENT_ONLINE_MESSAGE);
				}
			}
		} catch (DocumentException | SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		}
	}

	public void setCmder(SIPCommander cmder) {
	}

	public void setStorager(IVideoManagerStorager storager) {
		this.storager = storager;
	}

	public void setPublisher(EventPublisher publisher) {
		this.publisher = publisher;
	}

	public void setRedis(RedisUtil redis) {
	}

	public void setDeferredResultHolder(DeferredResultHolder deferredResultHolder) {
	}

	public void setOffLineDetector(DeviceOffLineDetector offLineDetector) {
		this.offLineDetector = offLineDetector;
	}

	public IRedisCatchStorage getRedisCatchStorage() {
		return redisCatchStorage;
	}

	public void setRedisCatchStorage(IRedisCatchStorage redisCatchStorage) {
		this.redisCatchStorage = redisCatchStorage;
	}
}
