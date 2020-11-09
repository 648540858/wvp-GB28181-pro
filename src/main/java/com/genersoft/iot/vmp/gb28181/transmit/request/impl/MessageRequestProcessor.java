package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.*;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.RecordItem;
import com.genersoft.iot.vmp.gb28181.event.DeviceOffLineDetector;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;
import com.genersoft.iot.vmp.gb28181.utils.DateUtil;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.springframework.util.StringUtils;

/**
 * @Description:MESSAGE请求处理器
 * @author: swwheihei
 * @date: 2020年5月3日 下午5:32:41
 */
public class MessageRequestProcessor extends SIPRequestAbstractProcessor {

	private final static Logger logger = LoggerFactory.getLogger(MessageRequestProcessor.class);

	private SIPCommander cmder;

	private IVideoManagerStorager storager;

	private EventPublisher publisher;

	private RedisUtil redis;

	private DeferredResultHolder deferredResultHolder;

	private DeviceOffLineDetector offLineDetector;

	private final static String CACHE_RECORDINFO_KEY = "CACHE_RECORDINFO_";

	private static final String MESSAGE_KEEP_ALIVE = "Keepalive";
	private static final String MESSAGE_CONFIG_DOWNLOAD = "ConfigDownload";
	private static final String MESSAGE_CATALOG = "Catalog";
	private static final String MESSAGE_DEVICE_INFO = "DeviceInfo";
	private static final String MESSAGE_ALARM = "Alarm";
	private static final String MESSAGE_RECORD_INFO = "RecordInfo";
	// private static final String MESSAGE_BROADCAST = "Broadcast";
	// private static final String MESSAGE_DEVICE_STATUS = "DeviceStatus";
	// private static final String MESSAGE_MOBILE_POSITION = "MobilePosition";
	// private static final String MESSAGE_MOBILE_POSITION_INTERVAL = "Interval";

	/**
	 * 处理MESSAGE请求
	 * 
	 * @param evt
	 */
	@Override
	public void process(RequestEvent evt) {

		try {
			Element rootElement = getRootElement(evt);
			String cmd = XmlUtil.getText(rootElement, "CmdType");

			if (MESSAGE_KEEP_ALIVE.equals(cmd)) {
				logger.info("接收到KeepAlive消息");
				processMessageKeepAlive(evt);
			} else if (MESSAGE_CONFIG_DOWNLOAD.equals(cmd)) {
				logger.info("接收到ConfigDownload消息");
			} else if (MESSAGE_CATALOG.equals(cmd)) {
				logger.info("接收到Catalog消息");
				processMessageCatalogList(evt);
			} else if (MESSAGE_DEVICE_INFO.equals(cmd)) {
				logger.info("接收到DeviceInfo消息");
				processMessageDeviceInfo(evt);
			} else if (MESSAGE_ALARM.equals(cmd)) {
				logger.info("接收到Alarm消息");
				processMessageAlarm(evt);
			} else if (MESSAGE_RECORD_INFO.equals(cmd)) {
				logger.info("接收到RecordInfo消息");
				processMessageRecordInfo(evt);
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 收到deviceInfo设备信息请求 处理
	 * 
	 * @param evt
	 */
	private void processMessageDeviceInfo(RequestEvent evt) {
		try {
			Element rootElement = getRootElement(evt);
			Element deviceIdElement = rootElement.element("DeviceID");
			String deviceId = deviceIdElement.getText().toString();

			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				return;
			}
			device.setName(XmlUtil.getText(rootElement, "DeviceName"));
			device.setManufacturer(XmlUtil.getText(rootElement, "Manufacturer"));
			device.setModel(XmlUtil.getText(rootElement, "Model"));
			device.setFirmware(XmlUtil.getText(rootElement, "Firmware"));
			if (StringUtils.isEmpty(device.getStreamMode())) {
				device.setStreamMode("UDP");
			}
			storager.updateDevice(device);

			RequestMessage msg = new RequestMessage();
			msg.setDeviceId(deviceId);
			msg.setType(DeferredResultHolder.CALLBACK_CMD_DEVICEINFO);
			msg.setData(device);
			deferredResultHolder.invokeResult(msg);
			// 回复200 OK
			responseAck(evt);
			if (offLineDetector.isOnline(deviceId)) {
				publisher.onlineEventPublish(deviceId, VideoManagerConstants.EVENT_ONLINE_KEEPLIVE);
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
			Element rootElement = getRootElement(evt);
			Element deviceIdElement = rootElement.element("DeviceID");
			String deviceId = deviceIdElement.getText();
			Element deviceListElement = rootElement.element("DeviceList");
			if (deviceListElement == null) {
				return;
			}
			Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
			if (deviceListIterator != null) {
				Device device = storager.queryVideoDevice(deviceId);
				if (device == null) {
					return;
				}
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
					if (status.equals("ON") || status.equals("On")) {
						deviceChannel.setStatus(1);
					}
					if (status.equals("OFF") || status.equals("Off")) {
						deviceChannel.setStatus(0);
					}

					deviceChannel.setManufacture(XmlUtil.getText(itemDevice, "Manufacturer"));
					deviceChannel.setModel(XmlUtil.getText(itemDevice, "Model"));
					deviceChannel.setOwner(XmlUtil.getText(itemDevice, "Owner"));
					deviceChannel.setCivilCode(XmlUtil.getText(itemDevice, "CivilCode"));
					deviceChannel.setBlock(XmlUtil.getText(itemDevice, "Block"));
					deviceChannel.setAddress(XmlUtil.getText(itemDevice, "Address"));
					deviceChannel.setParental(itemDevice.element("Parental") == null ? 0
							: Integer.parseInt(XmlUtil.getText(itemDevice, "Parental")));
					deviceChannel.setParentId(XmlUtil.getText(itemDevice, "ParentID"));
					deviceChannel.setSafetyWay(itemDevice.element("SafetyWay") == null ? 0
							: Integer.parseInt(XmlUtil.getText(itemDevice, "SafetyWay")));
					deviceChannel.setRegisterWay(itemDevice.element("RegisterWay") == null ? 1
							: Integer.parseInt(XmlUtil.getText(itemDevice, "RegisterWay")));
					deviceChannel.setCertNum(XmlUtil.getText(itemDevice, "CertNum"));
					deviceChannel.setCertifiable(itemDevice.element("Certifiable") == null ? 0
							: Integer.parseInt(XmlUtil.getText(itemDevice, "Certifiable")));
					deviceChannel.setErrCode(itemDevice.element("ErrCode") == null ? 0
							: Integer.parseInt(XmlUtil.getText(itemDevice, "ErrCode")));
					deviceChannel.setEndTime(XmlUtil.getText(itemDevice, "EndTime"));
					deviceChannel.setSecrecy(XmlUtil.getText(itemDevice, "Secrecy"));
					deviceChannel.setIpAddress(XmlUtil.getText(itemDevice, "IPAddress"));
					deviceChannel.setPort(itemDevice.element("Port") == null ? 0
							: Integer.parseInt(XmlUtil.getText(itemDevice, "Port")));
					deviceChannel.setPassword(XmlUtil.getText(itemDevice, "Password"));
					deviceChannel.setLongitude(itemDevice.element("Longitude") == null ? 0.00
							: Double.parseDouble(XmlUtil.getText(itemDevice, "Longitude")));
					deviceChannel.setLatitude(itemDevice.element("Latitude") == null ? 0.00
							: Double.parseDouble(XmlUtil.getText(itemDevice, "Latitude")));
					deviceChannel.setPTZType(itemDevice.element("PTZType") == null ? 0
							: Integer.parseInt(XmlUtil.getText(itemDevice, "PTZType")));
					deviceChannel.setHasAudio(true); // 默认含有音频，播放时再检查是否有音频及是否AAC
					storager.updateChannel(device.getDeviceId(), deviceChannel);
				}

				RequestMessage msg = new RequestMessage();
				msg.setDeviceId(deviceId);
				msg.setType(DeferredResultHolder.CALLBACK_CMD_CATALOG);
				msg.setData(device);
				deferredResultHolder.invokeResult(msg);
				// 回复200 OK
				responseAck(evt);
				if (offLineDetector.isOnline(deviceId)) {
					publisher.onlineEventPublish(deviceId, VideoManagerConstants.EVENT_ONLINE_KEEPLIVE);
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
			Element rootElement = getRootElement(evt);
			Element deviceIdElement = rootElement.element("DeviceID");
			String deviceId = deviceIdElement.getText().toString();

			Device device = storager.queryVideoDevice(deviceId);
			if (device == null) {
				// TODO 也可能是通道
				// storager.queryChannel(deviceId)
				return;
			}
			device.setName(XmlUtil.getText(rootElement, "DeviceName"));
			device.setManufacturer(XmlUtil.getText(rootElement, "Manufacturer"));
			device.setModel(XmlUtil.getText(rootElement, "Model"));
			device.setFirmware(XmlUtil.getText(rootElement, "Firmware"));
			if (StringUtils.isEmpty(device.getStreamMode())) {
				device.setStreamMode("UDP");
			}
			storager.updateDevice(device);
			cmder.catalogQuery(device);
			// 回复200 OK
			responseAck(evt);
			if (offLineDetector.isOnline(deviceId)) {
				publisher.onlineEventPublish(deviceId, VideoManagerConstants.EVENT_ONLINE_KEEPLIVE);
			}
		} catch (DocumentException | SipException | InvalidArgumentException | ParseException e) {
			// } catch (DocumentException e) {
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
			Element rootElement = getRootElement(evt);
			String deviceId = XmlUtil.getText(rootElement, "DeviceID");
			// 回复200 OK
			responseAck(evt);
			if (offLineDetector.isOnline(deviceId)) {
				publisher.onlineEventPublish(deviceId, VideoManagerConstants.EVENT_ONLINE_KEEPLIVE);
			} else {
			}
		} catch (ParseException | SipException | InvalidArgumentException | DocumentException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 收到catalog设备目录列表请求 处理 TODO 过期时间暂时写死180秒，后续与DeferredResult超时时间保持一致
	 * 
	 * @param evt
	 */
	private void processMessageRecordInfo(RequestEvent evt) {
		try {
			// 回复200 OK
			responseAck(evt);
			RecordInfo recordInfo = new RecordInfo();
			Element rootElement = getRootElement(evt);
			Element deviceIdElement = rootElement.element("DeviceID");
			String deviceId = deviceIdElement.getText().toString();
			recordInfo.setDeviceId(deviceId);
			recordInfo.setName(XmlUtil.getText(rootElement, "Name"));
			recordInfo.setSumNum(Integer.parseInt(XmlUtil.getText(rootElement, "SumNum")));
			String sn = XmlUtil.getText(rootElement, "SN");
			Element recordListElement = rootElement.element("RecordList");
			if (recordListElement == null || recordInfo.getSumNum() == 0) {
				logger.info("无录像数据");
				// responseAck(evt);
				// return;
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
						record.setDeviceId(XmlUtil.getText(itemRecord, "DeviceID"));
						record.setName(XmlUtil.getText(itemRecord, "Name"));
						record.setFilePath(XmlUtil.getText(itemRecord, "FilePath"));
						record.setAddress(XmlUtil.getText(itemRecord, "Address"));
						record.setStartTime(
								DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(XmlUtil.getText(itemRecord, "StartTime")));
						record.setEndTime(
								DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(XmlUtil.getText(itemRecord, "EndTime")));
						record.setSecrecy(itemRecord.element("Secrecy") == null ? 0
								: Integer.parseInt(XmlUtil.getText(itemRecord, "Secrecy")));
						record.setType(XmlUtil.getText(itemRecord, "Type"));
						record.setRecorderId(XmlUtil.getText(itemRecord, "RecorderID"));
						recordList.add(record);
					}
					// recordList.sort(Comparator.naturalOrder());
					recordInfo.setRecordList(recordList);
				}

				// 存在录像且如果当前录像明细个数小于总条数，说明拆包返回，需要组装，暂不返回
				if (recordInfo.getSumNum() > 0 && recordList.size() > 0 && recordList.size() < recordInfo.getSumNum()) {
					// 为防止连续请求该设备的录像数据，返回数据错乱，特增加sn进行区分
					String cacheKey = CACHE_RECORDINFO_KEY + deviceId + sn;
					// TODO 暂时直接操作redis存储，后续封装专用缓存接口，改为本地内存缓存
					if (redis.hasKey(cacheKey)) {
						List<RecordItem> previousList = (List<RecordItem>) redis.get(cacheKey);
						if (previousList != null && previousList.size() > 0) {
							recordList.addAll(previousList);
						}
						// 本分支表示录像列表被拆包，且加上之前的数据还是不够,保存缓存返回，等待下个包再处理
						if (recordList.size() < recordInfo.getSumNum()) {
							logger.info("已获取" + recordList.size() + "项录像数据，共" + recordInfo.getSumNum() + "项");
							redis.set(cacheKey, recordList, 90);
							return;
						} else {
							// 本分支表示录像被拆包，但加上之前的数据够足够，返回响应
							// 因设备心跳有监听redis过期机制，为提高性能，此处手动删除
							logger.info("录像数据已全部获取");
							redis.del(cacheKey);
						}
					} else {
						// 本分支有两种可能：1、录像列表被拆包，且是第一个包,直接保存缓存返回，等待下个包再处理
						// 2、之前有包，但超时清空了，那么这次sn批次的响应数据已经不完整，等待过期时间后redis自动清空数据
						logger.info("已获取" + recordList.size() + "项录像数据，共" + recordInfo.getSumNum() + "项");
						logger.info("等待后续的包...");

						redis.set(cacheKey, recordList, 90);
						return;
					}
				}
				// 自然顺序排序, 元素进行升序排列
				recordInfo.getRecordList().sort(Comparator.naturalOrder());
			}
			// 走到这里，有以下可能：1、没有录像信息,第一次收到recordinfo的消息即返回响应数据，无redis操作
			// 2、有录像数据，且第一次即收到完整数据，返回响应数据，无redis操作
			// 3、有录像数据，在超时时间内收到多次包组装后数量足够，返回数据

			// 对记录进行排序
			RequestMessage msg = new RequestMessage();
			msg.setDeviceId(deviceId);
			msg.setType(DeferredResultHolder.CALLBACK_CMD_RECORDINFO);
			// // 自然顺序排序, 元素进行升序排列
			// recordInfo.getRecordList().sort(Comparator.naturalOrder());
			msg.setData(recordInfo);
			deferredResultHolder.invokeResult(msg);
			logger.info("处理完成，返回结果");
		} catch (DocumentException | SipException | InvalidArgumentException | ParseException e) {
			e.printStackTrace();
		}
	}

	private void responseAck(RequestEvent evt) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(Response.OK, evt.getRequest());
		getServerTransaction(evt).sendResponse(response);
	}

	private Element getRootElement(RequestEvent evt) throws DocumentException {
		Request request = evt.getRequest();
		SAXReader reader = new SAXReader();
		reader.setEncoding("gbk");
		Document xml = reader.read(new ByteArrayInputStream(request.getRawContent()));
		return xml.getRootElement();
	}

	public void setCmder(SIPCommander cmder) {
		this.cmder = cmder;
	}

	public void setStorager(IVideoManagerStorager storager) {
		this.storager = storager;
	}

	public void setPublisher(EventPublisher publisher) {
		this.publisher = publisher;
	}

	public void setRedis(RedisUtil redis) {
		this.redis = redis;
	}

	public void setDeferredResultHolder(DeferredResultHolder deferredResultHolder) {
		this.deferredResultHolder = deferredResultHolder;
	}

	public void setOffLineDetector(DeviceOffLineDetector offLineDetector) {
		this.offLineDetector = offLineDetector;
	}

}
