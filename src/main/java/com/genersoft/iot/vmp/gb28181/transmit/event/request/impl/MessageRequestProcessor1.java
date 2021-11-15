package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.VManageBootstrap;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.DeviceOffLineDetector;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.callback.CheckForAllRecordsThread;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.DateUtil;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.service.IDeviceAlarmService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.utils.GpsUtil;
import com.genersoft.iot.vmp.utils.SpringBeanFactory;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.address.SipUri;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sip.*;
import javax.sip.address.SipURI;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * @description: MESSAGE请求分发处理器，
 * @author: panlinlin
 * @date: 2021年11月8日 10：28
 */
@SuppressWarnings(value={"unchecked", "rawtypes"})
@Component
public class MessageRequestProcessor1 extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	public static volatile List<String> threadNameList = new ArrayList();
	private final static Logger logger = LoggerFactory.getLogger(MessageRequestProcessor1.class);
	private static Map<String, ISIPRequestProcessor> messageHandlerMap = new ConcurrentHashMap<>();


	private final static String CACHE_RECORDINFO_KEY = "CACHE_RECORDINFO_";
	private static final String MESSAGE_KEEP_ALIVE = "Keepalive";
	private static final String MESSAGE_CONFIG_DOWNLOAD = "ConfigDownload";
	private static final String MESSAGE_CATALOG = "Catalog";
	private static final String MESSAGE_DEVICE_INFO = "DeviceInfo";
	private static final String MESSAGE_ALARM = "Alarm";
	private static final String MESSAGE_RECORD_INFO = "RecordInfo";
	private static final String MESSAGE_MEDIA_STATUS = "MediaStatus";
	private static final String MESSAGE_BROADCAST = "Broadcast";
	private static final String MESSAGE_DEVICE_STATUS = "DeviceStatus";
	private static final String MESSAGE_DEVICE_CONTROL = "DeviceControl";
	private static final String MESSAGE_DEVICE_CONFIG = "DeviceConfig";
	private static final String MESSAGE_MOBILE_POSITION = "MobilePosition";
	private static final String MESSAGE_PRESET_QUERY = "PresetQuery";
	private String method = "MESSAGE1111";

	@Autowired
	private UserSetup userSetup;

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private SipConfig config;

	@Autowired
	private SIPCommanderFroPlatform cmderFroPlatform;

	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private EventPublisher publisher;

	@Autowired
	private RedisUtil redis;

	@Autowired
	private DeferredResultHolder deferredResultHolder;

	@Autowired
	private DeviceOffLineDetector offLineDetector;

	@Autowired
	private IDeviceAlarmService deviceAlarmService;

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	/**
	 * 处理MESSAGE请求
	 * 
	 * @param evt
	 */
	@Override
	public void process(RequestEvent evt) {

		try {
			Element rootElement = getRootElement(evt);
			String cmd = getText(rootElement, "CmdType");

			if (MESSAGE_KEEP_ALIVE.equals(cmd)) {
				logger.debug("接收到KeepAlive消息");
				processMessageKeepAlive(evt);
			} else if (MESSAGE_CONFIG_DOWNLOAD.equals(cmd)) {
				logger.debug("接收到ConfigDownload消息");
				processMessageConfigDownload(evt);
			} else if (MESSAGE_CATALOG.equals(cmd)) {
				logger.debug("接收到Catalog消息");
				processMessageCatalogList(evt);
			} else if (MESSAGE_DEVICE_INFO.equals(cmd)) {
				// DeviceInfo消息处理
				processMessageDeviceInfo(evt);
			} else if (MESSAGE_DEVICE_STATUS.equals(cmd)) {
				// DeviceStatus消息处理
				processMessageDeviceStatus(evt);
			} else if (MESSAGE_DEVICE_CONTROL.equals(cmd)) {
				logger.debug("接收到DeviceControl消息");
				processMessageDeviceControl(evt);
			} else if (MESSAGE_DEVICE_CONFIG.equals(cmd)) {
				logger.info("接收到DeviceConfig消息");
				processMessageDeviceConfig(evt);
			} else if (MESSAGE_ALARM.equals(cmd)) {
				logger.debug("接收到Alarm消息");
				processMessageAlarm(evt);
			} else if (MESSAGE_RECORD_INFO.equals(cmd)) {
				logger.debug("接收到RecordInfo消息");
				processMessageRecordInfo(evt);
			}else if (MESSAGE_MEDIA_STATUS.equals(cmd)) {
				logger.debug("接收到MediaStatus消息");
				processMessageMediaStatus(evt);
			} else if (MESSAGE_MOBILE_POSITION.equals(cmd)) {
				logger.debug("接收到MobilePosition消息");
				processMessageMobilePosition(evt);
			} else if (MESSAGE_PRESET_QUERY.equals(cmd)) {
				logger.debug("接收到PresetQuery消息");
				processMessagePresetQuery(evt);
			} else if (MESSAGE_BROADCAST.equals(cmd)) {
				// Broadcast消息处理
				processMessageBroadcast(evt);
			} else {
				logger.debug("接收到消息：" + cmd);
				responseAck(evt, Response.OK);
			}
		} catch (DocumentException | SipException |InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 处理MobilePosition移动位置消息
	 * 
	 * @param evt
	 */
	private void processMessageMobilePosition(RequestEvent evt) {
		try {
			String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				logger.warn("处理MobilePosition移动位置消息时未找到设备信息");
				responseAck(evt, Response.NOT_FOUND);
				return;
			}
			Element rootElement = getRootElement(evt, device.getCharset());

			MobilePosition mobilePosition = new MobilePosition();
			if (!StringUtils.isEmpty(device.getName())) {
				mobilePosition.setDeviceName(device.getName());
			}
			mobilePosition.setDeviceId(deviceId);
			mobilePosition.setChannelId(getText(rootElement, "DeviceID"));
			mobilePosition.setTime(getText(rootElement, "Time"));
			mobilePosition.setLongitude(Double.parseDouble(getText(rootElement, "Longitude")));
			mobilePosition.setLatitude(Double.parseDouble(getText(rootElement, "Latitude")));
            if (NumericUtil.isDouble(getText(rootElement, "Speed"))) {
				mobilePosition.setSpeed(Double.parseDouble(getText(rootElement, "Speed")));
			} else {
				mobilePosition.setSpeed(0.0);
			}
			if (NumericUtil.isDouble(getText(rootElement, "Direction"))) {
				mobilePosition.setDirection(Double.parseDouble(getText(rootElement, "Direction")));
			} else {
				mobilePosition.setDirection(0.0);
			}
			if (NumericUtil.isDouble(getText(rootElement, "Altitude"))) {
				mobilePosition.setAltitude(Double.parseDouble(getText(rootElement, "Altitude")));
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
			//回复 200 OK
			responseAck(evt, Response.OK);
		} catch (DocumentException | SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 处理DeviceStatus设备状态Message
	 * 
	 * @param evt
	 */
	private void processMessageDeviceStatus(RequestEvent evt) {
		try {
			String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				logger.warn("处理DeviceStatus设备状态Message时未找到设备信息");
				responseAck(evt, Response.NOT_FOUND);
				return;
			}
			Element rootElement = getRootElement(evt);
			String name = rootElement.getName();
			Element deviceIdElement = rootElement.element("DeviceID");
			String channelId = deviceIdElement.getText();
			if (name.equalsIgnoreCase("Query")) { // 区分是Response——查询响应，还是Query——查询请求
				logger.info("接收到DeviceStatus查询消息");
				FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
				String platformId = ((SipUri) fromHeader.getAddress().getURI()).getUser();
					if (platformId == null) {
					responseAck(evt, Response.NOT_FOUND);
					return;
				} else {
					// 回复200 OK
					responseAck(evt, Response.OK);
					String sn = rootElement.element("SN").getText();
					ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(platformId);
					cmderFroPlatform.deviceStatusResponse(parentPlatform, sn, fromHeader.getTag());
				}
			} else {
				logger.info("接收到DeviceStatus应答消息");
				// 检查设备是否存在， 不存在则不回复
				if (storager.exists(deviceId)) {
					// 回复200 OK
					responseAck(evt, Response.OK);
					JSONObject json = new JSONObject();
					XmlUtil.node2Json(rootElement, json);
					if (logger.isDebugEnabled()) {
						logger.debug(json.toJSONString());
					}
					RequestMessage msg = new RequestMessage();
					msg.setKey(DeferredResultHolder.CALLBACK_CMD_DEVICESTATUS + deviceId + channelId);
					msg.setData(json);
					deferredResultHolder.invokeAllResult(msg);

					if (offLineDetector.isOnline(deviceId)) {
						publisher.onlineEventPublish(device, VideoManagerConstants.EVENT_ONLINE_MESSAGE);
					} else {
					}
				}
			}

		} catch (ParseException | SipException | InvalidArgumentException | DocumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 处理DeviceControl设备状态Message
	 * 
	 * @param evt
	 */
	private void processMessageDeviceControl(RequestEvent evt) {
		try {
			String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				logger.warn("处理DeviceControl设备状态Message未找到设备信息");
				responseAck(evt, Response.NOT_FOUND);
				return;
			}
			Element rootElement = getRootElement(evt);
			String channelId = getText(rootElement, "DeviceID");
			//String result = getText(rootElement, "Result");
			// 回复200 OK
			responseAck(evt, Response.OK);
			if (rootElement.getName().equals("Response")) {//} !StringUtils.isEmpty(result)) {
				// 此处是对本平台发出DeviceControl指令的应答
				JSONObject json = new JSONObject();
				XmlUtil.node2Json(rootElement, json);
				if (logger.isDebugEnabled()) {
					logger.debug(json.toJSONString());
				}
				RequestMessage msg = new RequestMessage();
				String key = DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL +  deviceId + channelId;
				msg.setKey(key);
				msg.setData(json);
				deferredResultHolder.invokeAllResult(msg);
			} else {
				// 此处是上级发出的DeviceControl指令
				String platformId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
				String targetGBId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
				// 远程启动功能
				if (!StringUtils.isEmpty(getText(rootElement, "TeleBoot"))) {
					if (deviceId.equals(targetGBId)) {
						// 远程启动本平台：需要在重新启动程序后先对SipStack解绑
						logger.info("执行远程启动本平台命令");
						ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(platformId);
						cmderFroPlatform.unregister(parentPlatform, null, null);
	
						Thread restartThread = new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									Thread.sleep(3000);
									SipProvider up = (SipProvider) SpringBeanFactory.getBean("udpSipProvider");
									SipStackImpl stack = (SipStackImpl)up.getSipStack();
									stack.stop();
									Iterator listener = stack.getListeningPoints();
									while (listener.hasNext()) {
										stack.deleteListeningPoint((ListeningPoint) listener.next());
									}
									Iterator providers = stack.getSipProviders();
									while (providers.hasNext()) {
										stack.deleteSipProvider((SipProvider) providers.next());
									}
									VManageBootstrap.restart();
								} catch (InterruptedException ignored) {
								} catch (ObjectInUseException e) {
									e.printStackTrace();
								}
							}
						});
			
						restartThread.setDaemon(false);
						restartThread.start();
					} else {
						// 远程启动指定设备
					}
				}
				// 云台/前端控制命令
				if (!StringUtils.isEmpty(getText(rootElement,"PTZCmd")) && !deviceId.equals(targetGBId)) {
					String cmdString = getText(rootElement,"PTZCmd");
					Device deviceForPlatform = storager.queryVideoDeviceByPlatformIdAndChannelId(platformId, deviceId);
					cmder.fronEndCmd(deviceForPlatform, deviceId, cmdString);
				}
			}
		} catch (ParseException | SipException | InvalidArgumentException | DocumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 处理DeviceConfig设备状态Message
	 * 
	 * @param evt
	 */
	private void processMessageDeviceConfig(RequestEvent evt) {
		try {
			String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
			// 查询设备是否存在
			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				logger.warn("处理DeviceConfig设备状态Message消息时未找到设备信息");
				responseAck(evt, Response.NOT_FOUND);
				return;
			}
			Element rootElement = getRootElement(evt);
			String channelId = getText(rootElement, "DeviceID");
			// 回复200 OK
			responseAck(evt, Response.OK);
			if (rootElement.getName().equals("Response")) {
					// 此处是对本平台发出DeviceControl指令的应答
				JSONObject json = new JSONObject();
				XmlUtil.node2Json(rootElement, json);
				if (logger.isDebugEnabled()) {
					logger.debug(json.toJSONString());
				}
				String key = DeferredResultHolder.CALLBACK_CMD_DEVICECONFIG + deviceId + channelId;
				RequestMessage msg = new RequestMessage();
				msg.setKey(key);
				msg.setData(json);
				deferredResultHolder.invokeAllResult(msg);
			} else {
				// 此处是上级发出的DeviceConfig指令
			}
		} catch (ParseException | SipException | InvalidArgumentException | DocumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 处理ConfigDownload设备状态Message
	 * 
	 * @param evt
	 */
	private void processMessageConfigDownload(RequestEvent evt) {
		try {
			String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
			// 查询设备是否存在
			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				logger.warn("处理ConfigDownload设备状态Message时未找到设备信息");
				responseAck(evt, Response.NOT_FOUND);
				return;
			}
			Element rootElement = getRootElement(evt);
			String channelId = getText(rootElement, "DeviceID");
			String key = DeferredResultHolder.CALLBACK_CMD_CONFIGDOWNLOAD + deviceId + channelId;
			// 回复200 OK
			responseAck(evt, Response.OK);
			if (rootElement.getName().equals("Response")) {
					// 此处是对本平台发出DeviceControl指令的应答
				JSONObject json = new JSONObject();
				XmlUtil.node2Json(rootElement, json);
				if (logger.isDebugEnabled()) {
					logger.debug(json.toJSONString());
				}
				RequestMessage msg = new RequestMessage();
				msg.setKey(key);
				msg.setData(json);
				deferredResultHolder.invokeAllResult(msg);
			} else {
				// 此处是上级发出的DeviceConfig指令
			}
		} catch (ParseException | SipException | InvalidArgumentException | DocumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 处理PresetQuery预置位列表Message
	 * 
	 * @param evt
	 */
	private void processMessagePresetQuery(RequestEvent evt) {
		try {
			String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
			// 查询设备是否存在
			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				logger.warn("处理PresetQuery预置位列表Message时未找到设备信息");
				responseAck(evt, Response.NOT_FOUND);
				return;
			}
			Element rootElement = getRootElement(evt);
			String channelId = getText(rootElement, "DeviceID");
			String key = DeferredResultHolder.CALLBACK_CMD_PRESETQUERY + deviceId + channelId;
			// 回复200 OK
			responseAck(evt, Response.OK);
			if (rootElement.getName().equals("Response")) {//   !StringUtils.isEmpty(result)) {
				// 此处是对本平台发出DeviceControl指令的应答
				JSONObject json = new JSONObject();
				XmlUtil.node2Json(rootElement, json);
				if (logger.isDebugEnabled()) {
					logger.debug(json.toJSONString());
				}
				RequestMessage msg = new RequestMessage();
				msg.setKey(key);
				msg.setData(json);
				deferredResultHolder.invokeAllResult(msg);
			} else {
				// 此处是上级发出的DeviceControl指令
			}
		} catch (ParseException | SipException | InvalidArgumentException | DocumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 处理DeviceInfo设备信息Message
	 * 
	 * @param evt
	 */
	private void processMessageDeviceInfo(RequestEvent evt) {
		try {
			String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
			// 查询设备是否存在
			Device device = storager.queryVideoDevice(deviceId);
			ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(deviceId);

			Element rootElement = getRootElement(evt);
			String requestName = rootElement.getName();
			Element deviceIdElement = rootElement.element("DeviceID");
			String channelId = deviceIdElement.getTextTrim();
			String key = DeferredResultHolder.CALLBACK_CMD_DEVICEINFO + deviceId + channelId;
			if (device != null ) {
				rootElement = getRootElement(evt, device.getCharset());
			}
			if (requestName.equals("Query")) {
				logger.info("接收到DeviceInfo查询消息");
				FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
				if (parentPlatform == null) {
					responseAck(evt, Response.NOT_FOUND);
					return;
				} else {
					// 回复200 OK
					responseAck(evt, Response.OK);
					String sn = rootElement.element("SN").getText();
					cmderFroPlatform.deviceInfoResponse(parentPlatform, sn, fromHeader.getTag());
				}
			} else {
				logger.debug("接收到DeviceInfo应答消息");
				if (device == null) {
					logger.warn("处理DeviceInfo设备信息Message时未找到设备信息");
					responseAck(evt, Response.NOT_FOUND);
					return;
				}

				device.setName(getText(rootElement, "DeviceName"));

				device.setManufacturer(getText(rootElement, "Manufacturer"));
				device.setModel(getText(rootElement, "Model"));
				device.setFirmware(getText(rootElement, "Firmware"));
				if (StringUtils.isEmpty(device.getStreamMode())) {
					device.setStreamMode("UDP");
				}
				storager.updateDevice(device);
	
				RequestMessage msg = new RequestMessage();
				msg.setKey(key);
				msg.setData(device);
				deferredResultHolder.invokeAllResult(msg);
				// 回复200 OK
				responseAck(evt, Response.OK);
				if (offLineDetector.isOnline(deviceId)) {
					publisher.onlineEventPublish(device, VideoManagerConstants.EVENT_ONLINE_MESSAGE);
				}
			}
		} catch (DocumentException | SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 收到catalog设备目录列表请求 处理
	 * 
	 * @param evt
	 */
	private void processMessageCatalogList(RequestEvent evt) {
		try {

			String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
			// 查询设备是否存在
			Device device = storager.queryVideoDevice(deviceId);
			ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(deviceId);


			Element rootElement = getRootElement(evt);
			String name = rootElement.getName();
			Element deviceIdElement = rootElement.element("DeviceID");
			String channelId = deviceIdElement.getText();
			Element deviceListElement = rootElement.element("DeviceList");
			String key = DeferredResultHolder.CALLBACK_CMD_CATALOG + deviceId;
			FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
			if (name.equalsIgnoreCase("Query")) { // 区分是Response——查询响应，还是Query——查询请求
				// TODO 后续将代码拆分
				if (parentPlatform == null) {
					responseAck(evt, Response.NOT_FOUND);
					return;
				} else {
					// 回复200 OK
					responseAck(evt, Response.OK);

					Element snElement = rootElement.element("SN");
					String sn = snElement.getText();
					// 准备回复通道信息
					List<ChannelReduce> channelReduces = storager.queryChannelListInParentPlatform(parentPlatform.getServerGBId());
					// 查询关联的直播通道
					List<GbStream> gbStreams = storager.queryGbStreamListInPlatform(parentPlatform.getServerGBId());
					int size = channelReduces.size() + gbStreams.size();
					// 回复级联的通道
					if (channelReduces.size() > 0) {
						for (ChannelReduce channelReduce : channelReduces) {
							DeviceChannel deviceChannel = storager.queryChannel(channelReduce.getDeviceId(), channelReduce.getChannelId());
							cmderFroPlatform.catalogQuery(deviceChannel, parentPlatform, sn, fromHeader.getTag(), size);
						}
					}
					// 回复直播的通道
					if (gbStreams.size() > 0) {
						for (GbStream gbStream : gbStreams) {
							DeviceChannel deviceChannel = new DeviceChannel();
							deviceChannel.setChannelId(gbStream.getGbId());
							deviceChannel.setName(gbStream.getName());
							deviceChannel.setLongitude(gbStream.getLongitude());
							deviceChannel.setLatitude(gbStream.getLatitude());
							deviceChannel.setDeviceId(parentPlatform.getDeviceGBId());
							deviceChannel.setManufacture("wvp-pro");
							deviceChannel.setStatus(gbStream.isStatus()?1:0);
//							deviceChannel.setParentId(parentPlatform.getDeviceGBId());
							deviceChannel.setRegisterWay(1);
							deviceChannel.setCivilCode(config.getDomain());
							deviceChannel.setModel("live");
							deviceChannel.setOwner("wvp-pro");
//							deviceChannel.setAddress("test");
							deviceChannel.setParental(0);
							deviceChannel.setSecrecy("0");
							deviceChannel.setSecrecy("0");

							cmderFroPlatform.catalogQuery(deviceChannel, parentPlatform, sn, fromHeader.getTag(), size);
						}
					}
					if (size == 0) {
						// 回复无通道
						cmderFroPlatform.catalogQuery(null, parentPlatform, sn, fromHeader.getTag(), size);
					}
				}


			} else {
				if (device == null) {
					logger.warn("收到catalog设备目录列表请求时未找到设备信息");
					responseAck(evt, Response.NOT_FOUND);
					return;
				}
				deviceListElement = getRootElement(evt, device.getCharset()).element("DeviceList");
				Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
				if (deviceListIterator != null) {

					// 遍历DeviceList
					while (deviceListIterator.hasNext()) {
						Element itemDevice = deviceListIterator.next();
						Element channelDeviceElement = itemDevice.element("DeviceID");
						if (channelDeviceElement == null) {
							continue;
						}
						String channelDeviceId = channelDeviceElement.getText();
						Element channdelNameElement = itemDevice.element("Name");
						String channelName = channdelNameElement != null ? channdelNameElement.getTextTrim().toString() : "";
						Element statusElement = itemDevice.element("Status");
						String status = statusElement != null ? statusElement.getText().toString() : "ON";
						DeviceChannel deviceChannel = new DeviceChannel();
						deviceChannel.setName(channelName);
						deviceChannel.setChannelId(channelDeviceId);
						// ONLINE OFFLINE  HIKVISION DS-7716N-E4 NVR的兼容性处理
						if (status.equals("ON") || status.equals("On") || status.equals("ONLINE")) {
							deviceChannel.setStatus(1);
						}
						if (status.equals("OFF") || status.equals("Off") || status.equals("OFFLINE")) {
							deviceChannel.setStatus(0);
						}

						deviceChannel.setManufacture(getText(itemDevice, "Manufacturer"));
						deviceChannel.setModel(getText(itemDevice, "Model"));
						deviceChannel.setOwner(getText(itemDevice, "Owner"));
						deviceChannel.setCivilCode(getText(itemDevice, "CivilCode"));
						deviceChannel.setBlock(getText(itemDevice, "Block"));
						deviceChannel.setAddress(getText(itemDevice, "Address"));
						if (getText(itemDevice, "Parental") == null || getText(itemDevice, "Parental") == "") {
							deviceChannel.setParental(0);
						} else {
							deviceChannel.setParental(Integer.parseInt(getText(itemDevice, "Parental")));
						}
						deviceChannel.setParentId(getText(itemDevice, "ParentID"));
						if (getText(itemDevice, "SafetyWay") == null || getText(itemDevice, "SafetyWay") == "") {
							deviceChannel.setSafetyWay(0);
						} else {
							deviceChannel.setSafetyWay(Integer.parseInt(getText(itemDevice, "SafetyWay")));
						}
						if (getText(itemDevice, "RegisterWay") == null || getText(itemDevice, "RegisterWay") == "") {
							deviceChannel.setRegisterWay(1);
						} else {
							deviceChannel.setRegisterWay(Integer.parseInt(getText(itemDevice, "RegisterWay")));
						}
						deviceChannel.setCertNum(getText(itemDevice, "CertNum"));
						if (getText(itemDevice, "Certifiable") == null || getText(itemDevice, "Certifiable") == "") {
							deviceChannel.setCertifiable(0);
						} else {
							deviceChannel.setCertifiable(Integer.parseInt(getText(itemDevice, "Certifiable")));
						}
						if (getText(itemDevice, "ErrCode") == null || getText(itemDevice, "ErrCode") == "") {
							deviceChannel.setErrCode(0);
						} else {
							deviceChannel.setErrCode(Integer.parseInt(getText(itemDevice, "ErrCode")));
						}
						deviceChannel.setEndTime(getText(itemDevice, "EndTime"));
						deviceChannel.setSecrecy(getText(itemDevice, "Secrecy"));
						deviceChannel.setIpAddress(getText(itemDevice, "IPAddress"));
						if (getText(itemDevice, "Port") == null || getText(itemDevice, "Port") == "") {
							deviceChannel.setPort(0);
						} else {
							deviceChannel.setPort(Integer.parseInt(getText(itemDevice, "Port")));
						}
						deviceChannel.setPassword(getText(itemDevice, "Password"));
						if (NumericUtil.isDouble(getText(itemDevice, "Longitude"))) {
							deviceChannel.setLongitude(Double.parseDouble(getText(itemDevice, "Longitude")));
						} else {
							deviceChannel.setLongitude(0.00);
						}
						if (NumericUtil.isDouble(getText(itemDevice, "Latitude"))) {
							deviceChannel.setLatitude(Double.parseDouble(getText(itemDevice, "Latitude")));
						} else {
							deviceChannel.setLatitude(0.00);
						}
						if (getText(itemDevice, "PTZType") == null || getText(itemDevice, "PTZType") == "") {
							deviceChannel.setPTZType(0);
						} else {
							deviceChannel.setPTZType(Integer.parseInt(getText(itemDevice, "PTZType")));
						}
						deviceChannel.setHasAudio(true); // 默认含有音频，播放时再检查是否有音频及是否AAC
						storager.updateChannel(device.getDeviceId(), deviceChannel);
					}

					RequestMessage msg = new RequestMessage();
					msg.setKey(key);
					msg.setData(device);
					deferredResultHolder.invokeAllResult(msg);
					// 回复200 OK
					responseAck(evt, Response.OK);
					if (offLineDetector.isOnline(deviceId)) {
						publisher.onlineEventPublish(device, VideoManagerConstants.EVENT_ONLINE_MESSAGE);
					}
				}
			}
		} catch (DocumentException | SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 收到alarm设备报警信息 处理
	 * 
	 * @param evt
	 */
	private void processMessageAlarm(RequestEvent evt) {
		try {
			String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				logger.warn("处理alarm设备报警信息未找到设备信息");
				responseAck(evt, Response.NOT_FOUND);
				return;
			}
			Element rootElement = getRootElement(evt, device.getCharset());
			Element deviceIdElement = rootElement.element("DeviceID");
			String channelId = deviceIdElement.getText().toString();
			String key = DeferredResultHolder.CALLBACK_CMD_ALARM + deviceId + channelId;
			// 回复200 OK
			responseAck(evt, Response.OK);

			if (device.getCharset() != null) {
				rootElement = getRootElement(evt, device.getCharset());
			}

			if (rootElement.getName().equals("Notify")) {	// 处理报警通知
				DeviceAlarm deviceAlarm = new DeviceAlarm();
				deviceAlarm.setDeviceId(deviceId);
				deviceAlarm.setChannelId(channelId);
				deviceAlarm.setAlarmPriority(getText(rootElement, "AlarmPriority"));
				deviceAlarm.setAlarmMethod(getText(rootElement, "AlarmMethod"));
				deviceAlarm.setAlarmTime(getText(rootElement, "AlarmTime"));
				if (getText(rootElement, "AlarmDescription") == null) {
					deviceAlarm.setAlarmDescription("");
				} else {
					deviceAlarm.setAlarmDescription(getText(rootElement, "AlarmDescription"));
				}
				if (NumericUtil.isDouble(getText(rootElement, "Longitude"))) {
					deviceAlarm.setLongitude(Double.parseDouble(getText(rootElement, "Longitude")));
				} else {
					deviceAlarm.setLongitude(0.00);
				}
				if (NumericUtil.isDouble(getText(rootElement, "Latitude"))) {
					deviceAlarm.setLatitude(Double.parseDouble(getText(rootElement, "Latitude")));
				} else {
					deviceAlarm.setLatitude(0.00);
				}
	
				if (!StringUtils.isEmpty(deviceAlarm.getAlarmMethod())) {
					if ( deviceAlarm.getAlarmMethod().equals("4")) {
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
				}
				logger.debug("存储报警信息、报警分类");
				// 存储报警信息、报警分类
				deviceAlarmService.add(deviceAlarm);
	
				if (offLineDetector.isOnline(deviceId)) {
					publisher.deviceAlarmEventPublish(deviceAlarm);
				}
			} else if (rootElement.getName().equals("Response")) {	// 处理报警查询响应
				JSONObject json = new JSONObject();
				XmlUtil.node2Json(rootElement, json);
				if (logger.isDebugEnabled()) {
					logger.debug(json.toJSONString());
				}
				RequestMessage msg = new RequestMessage();
				msg.setKey(key);
				msg.setData(json);
				deferredResultHolder.invokeAllResult(msg);
			}
		} catch (DocumentException | SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 收到keepalive请求 处理
	 * 
	 * @param evt
	 */
	private void processMessageKeepAlive(RequestEvent evt) {
		try {

			String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
			// 查询设备是否存在
			Device device = storager.queryVideoDevice(deviceId);

			Element rootElement = getRootElement(evt);
			String channelId = getText(rootElement, "DeviceID");

			// 检查设备是否存在并在线， 不在线则设置为在线
			if (device != null ) {
				// 回复200 OK
				responseAck(evt, Response.OK);
				publisher.onlineEventPublish(device, VideoManagerConstants.EVENT_ONLINE_KEEPLIVE);
			}else{
				logger.warn("收到[ "+deviceId+" ]心跳信息, 但是设备不存在, 回复404");
				Response response = getMessageFactory().createResponse(Response.NOT_FOUND, evt.getRequest());
				ServerTransaction serverTransaction = getServerTransaction(evt);
				serverTransaction.sendResponse(response);
				if (serverTransaction.getDialog() != null) {
					serverTransaction.getDialog().delete();
				}
			}

//			if (device != null && device.getOnline() == 1) {
//
//				if (offLineDetector.isOnline(deviceId)) {
//					publisher.onlineEventPublish(device, VideoManagerConstants.EVENT_ONLINE_KEEPLIVE);
//				} else {
//				}
//			}else {
////				logger.warn("收到[ "+deviceId+" ]心跳信息, 但是设备" + (device == null? "不存在":"离线") + ", 回复401");
////				Response response = getMessageFactory().createResponse(Response.UNAUTHORIZED, evt.getRequest());
////				getServerTransaction(evt).sendResponse(response);
//				publisher.onlineEventPublish(device, VideoManagerConstants.EVENT_ONLINE_KEEPLIVE);
//
//			}
		} catch (ParseException | SipException | InvalidArgumentException | DocumentException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 处理RecordInfo设备录像列表Message请求 TODO 过期时间暂时写死180秒，后续与DeferredResult超时时间保持一致
	 * 
	 * @param evt
	 */
	private void processMessageRecordInfo(RequestEvent evt) {
		try {

			String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
			// 查询设备是否存在
			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				logger.warn("处理DeviceInfo设备信息Message时未找到设备信息");
				responseAck(evt, Response.NOT_FOUND);
				return;
			}

			// 回复200 OK
			responseAck(evt, Response.OK);
			String uuid = UUID.randomUUID().toString().replace("-", "");
			RecordInfo recordInfo = new RecordInfo();
			Element rootElement = getRootElement(evt);
			Element deviceIdElement = rootElement.element("DeviceID");
			String channelId = deviceIdElement.getText().toString();
			String key = DeferredResultHolder.CALLBACK_CMD_RECORDINFO + deviceId + channelId;
			if (device != null ) {
				rootElement = getRootElement(evt, device.getCharset());
			}
			recordInfo.setDeviceId(deviceId);
			recordInfo.setChannelId(channelId);
			recordInfo.setName(getText(rootElement, "Name"));
			if (getText(rootElement, "SumNum")== null || getText(rootElement, "SumNum") =="") {
				recordInfo.setSumNum(0);
			} else {
				recordInfo.setSumNum(Integer.parseInt(getText(rootElement, "SumNum")));
			}
			String sn = getText(rootElement, "SN");
			Element recordListElement = rootElement.element("RecordList");
			if (recordListElement == null || recordInfo.getSumNum() == 0) {
				logger.info("无录像数据");
				RequestMessage msg = new RequestMessage();
				msg.setKey(key);
				msg.setData(recordInfo);
				deferredResultHolder.invokeAllResult(msg);
			} else {
				Iterator<Element> recordListIterator = recordListElement.elementIterator();
				List<RecordItem> recordList = new ArrayList<RecordItem>();
				if (recordListIterator != null) {
					RecordItem record = new RecordItem();
					logger.info("处理录像列表数据...");
					// 遍历DeviceList
					while (recordListIterator.hasNext()) {
						Element itemRecord = recordListIterator.next();
						Element recordElement = itemRecord.element("DeviceID");
						if (recordElement == null) {
							logger.info("记录为空，下一个...");
							continue;
						}
						record = new RecordItem();
						record.setDeviceId(getText(itemRecord, "DeviceID"));
						record.setName(getText(itemRecord, "Name"));
						record.setFilePath(getText(itemRecord, "FilePath"));
						record.setAddress(getText(itemRecord, "Address"));
						record.setStartTime(
								DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(getText(itemRecord, "StartTime")));
						record.setEndTime(
								DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(getText(itemRecord, "EndTime")));
						record.setSecrecy(itemRecord.element("Secrecy") == null ? 0
								: Integer.parseInt(getText(itemRecord, "Secrecy")));
						record.setType(getText(itemRecord, "Type"));
						record.setRecorderId(getText(itemRecord, "RecorderID"));
						recordList.add(record);
					}
					recordInfo.setRecordList(recordList);
				}

				// 改用单独线程统计已获取录像文件数量，避免多包并行分别统计不完整的问题
				String cacheKey = CACHE_RECORDINFO_KEY + deviceId + sn;
				redis.set(cacheKey + "_" + uuid, recordList, 90);
				if (!threadNameList.contains(cacheKey)) {
					threadNameList.add(cacheKey);
					CheckForAllRecordsThread chk = new CheckForAllRecordsThread(cacheKey, recordInfo);
					chk.setName(cacheKey);
					chk.setDeferredResultHolder(deferredResultHolder);
					chk.setRedis(redis);
					chk.setLogger(logger);
					chk.start();
					if (logger.isDebugEnabled()) {
						logger.debug("Start Thread " + cacheKey + ".");
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Thread " + cacheKey + " already started.");
					}
				}

				// 存在录像且如果当前录像明细个数小于总条数，说明拆包返回，需要组装，暂不返回
				// if (recordInfo.getSumNum() > 0 && recordList.size() > 0 && recordList.size() < recordInfo.getSumNum()) {
				// 	// 为防止连续请求该设备的录像数据，返回数据错乱，特增加sn进行区分
				// 	String cacheKey = CACHE_RECORDINFO_KEY + deviceId + sn;

				// 	redis.set(cacheKey + "_" + uuid, recordList, 90);
				// 	List<Object> cacheKeys = redis.scan(cacheKey + "_*");
				// 	List<RecordItem> totalRecordList = new ArrayList<RecordItem>();
				// 	for (int i = 0; i < cacheKeys.size(); i++) {
				// 		totalRecordList.addAll((List<RecordItem>) redis.get(cacheKeys.get(i).toString()));
				// 	}
				// 	if (totalRecordList.size() < recordInfo.getSumNum()) {
				// 		logger.info("已获取" + totalRecordList.size() + "项录像数据，共" + recordInfo.getSumNum() + "项");
				// 		return;
				// 	}
				// 	logger.info("录像数据已全部获取，共" + recordInfo.getSumNum() + "项");
				// 	recordInfo.setRecordList(totalRecordList);
				// 	for (int i = 0; i < cacheKeys.size(); i++) {
				// 		redis.del(cacheKeys.get(i).toString());
				// 	}
				// }
				// // 自然顺序排序, 元素进行升序排列
				// recordInfo.getRecordList().sort(Comparator.naturalOrder());
			}
			// 走到这里，有以下可能：1、没有录像信息,第一次收到recordinfo的消息即返回响应数据，无redis操作
			// 2、有录像数据，且第一次即收到完整数据，返回响应数据，无redis操作
			// 3、有录像数据，在超时时间内收到多次包组装后数量足够，返回数据

			// RequestMessage msg = new RequestMessage();
			// msg.setDeviceId(deviceId);
			// msg.setType(DeferredResultHolder.CALLBACK_CMD_RECORDINFO);
			// msg.setData(recordInfo);
			// deferredResultHolder.invokeResult(msg);
			// logger.info("处理完成，返回结果");
		} catch (DocumentException | SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 收到MediaStatus消息处理
 	 *  
 	 * @param evt
 	 */
	private void processMessageMediaStatus(RequestEvent evt){
		try {

			String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
			// 查询设备是否存在
			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				logger.warn("处理DeviceInfo设备信息Message时未找到设备信息");
				responseAck(evt, Response.NOT_FOUND);
				return;
			}

			// 回复200 OK
			responseAck(evt, Response.OK);
			Element rootElement = getRootElement(evt);
			String channelId = getText(rootElement, "DeviceID");
			String NotifyType =getText(rootElement, "NotifyType");
			if (NotifyType.equals("121")){
				logger.info("媒体播放完毕，通知关流");
				StreamInfo streamInfo = redisCatchStorage.queryPlaybackByDevice(deviceId, "*");
				if (streamInfo != null) {
					redisCatchStorage.stopPlayback(streamInfo);
					cmder.streamByeCmd(streamInfo.getDeviceID(), streamInfo.getChannelId());
				}
			}
		} catch (ParseException | SipException | InvalidArgumentException | DocumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 处理AudioBroadcast语音广播Message
	 * 
	 * @param evt
	 */
	private void processMessageBroadcast(RequestEvent evt) {
		try {

			String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
			// 查询设备是否存在
			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				logger.warn("处理DeviceInfo设备信息Message时未找到设备信息");
				responseAck(evt, Response.NOT_FOUND);
				return;
			}

			Element rootElement = getRootElement(evt);
			String channelId = getText(rootElement, "DeviceID");
			String key = DeferredResultHolder.CALLBACK_CMD_BROADCAST + deviceId + channelId;
			// 回复200 OK
			responseAck(evt, Response.OK);
			if (rootElement.getName().equals("Response")) {
					// 此处是对本平台发出Broadcast指令的应答
				JSONObject json = new JSONObject();
				XmlUtil.node2Json(rootElement, json);
				if (logger.isDebugEnabled()) {
					logger.debug(json.toJSONString());
				}
				RequestMessage msg = new RequestMessage();
				msg.setKey(key);
				msg.setData(json);
				deferredResultHolder.invokeAllResult(msg);
			} else {
				// 此处是上级发出的Broadcast指令
			}
		} catch (ParseException | SipException | InvalidArgumentException | DocumentException e) {
			e.printStackTrace();
		}
	}
}
