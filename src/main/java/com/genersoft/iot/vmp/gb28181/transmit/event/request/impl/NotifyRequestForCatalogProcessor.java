package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.conf.CivilCodeFileConf;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SIP命令类型： NOTIFY请求中的目录请求处理
 */
@Component
public class NotifyRequestForCatalogProcessor extends SIPRequestProcessorParent {


    private final static Logger logger = LoggerFactory.getLogger(NotifyRequestForCatalogProcessor.class);

	private final List<DeviceChannel> updateChannelOnlineList = new CopyOnWriteArrayList<>();
	private final List<DeviceChannel> updateChannelOfflineList = new CopyOnWriteArrayList<>();
	private final Map<String, DeviceChannel> updateChannelMap = new ConcurrentHashMap<>();

	private final Map<String, DeviceChannel> addChannelMap = new ConcurrentHashMap<>();
	private final List<DeviceChannel> deleteChannelList = new CopyOnWriteArrayList<>();


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

	private final static String talkKey = "notify-request-for-catalog-task";

	public void process(RequestEvent evt) {
		try {
			long start = System.currentTimeMillis();
			FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
			String deviceId = SipUtils.getUserIdFromFromHeader(fromHeader);

			Device device = redisCatchStorage.getDevice(deviceId);
			if (device == null || !device.isOnLine()) {
				logger.warn("[收到目录订阅]：{}, 但是设备已经离线", (device != null ? device.getDeviceId():"" ));
				return;
			}
			Element rootElement = getRootElement(evt, device.getCharset());
			if (rootElement == null) {
				logger.warn("[ 收到目录订阅 ] content cannot be null, {}", evt.getRequest());
				return;
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
					String event;
					if (eventElement == null) {
						logger.warn("[收到目录订阅]：{}, 但是Event为空, 设为默认值 ADD", (device != null ? device.getDeviceId():"" ));
						event = CatalogEvent.ADD;
					}else {
						event = eventElement.getText().toUpperCase();
					}
					DeviceChannel channel = XmlUtil.channelContentHandler(itemDevice, device, event, civilCodeFileConf);
					if (channel == null) {
						logger.info("[收到目录订阅]：但是解析失败 {}", new String(evt.getRequest().getRawContent()));
						continue;
					}
					if (channel.getParentId() != null && channel.getParentId().equals(sipConfig.getId())) {
						channel.setParentId(null);
					}
					channel.setDeviceId(device.getDeviceId());
					logger.info("[收到目录订阅]：{}/{}", device.getDeviceId(), channel.getChannelId());
					switch (event) {
						case CatalogEvent.ON:
							// 上线
							logger.info("[收到通道上线通知] 来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
							updateChannelOnlineList.add(channel);
							if (updateChannelOnlineList.size() > 300) {
								executeSaveForOnline();
							}
							if (userSetting.getDeviceStatusNotify()) {
								// 发送redis消息
								redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), channel.getChannelId(), true);
							}

							break;
						case CatalogEvent.OFF :
							// 离线
							logger.info("[收到通道离线通知] 来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
							if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
								logger.info("[收到通道离线通知] 但是平台已配置拒绝此消息，来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
							}else {
								updateChannelOfflineList.add(channel);
								if (updateChannelOfflineList.size() > 300) {
									executeSaveForOffline();
								}
								if (userSetting.getDeviceStatusNotify()) {
									// 发送redis消息
									redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), channel.getChannelId(), false);
								}
							}
							break;
						case CatalogEvent.VLOST:
							// 视频丢失
							logger.info("[收到通道视频丢失通知] 来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
							if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
								logger.info("[收到通道视频丢失通知] 但是平台已配置拒绝此消息，来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
							}else {
								updateChannelOfflineList.add(channel);
								if (updateChannelOfflineList.size() > 300) {
									executeSaveForOffline();
								}
								if (userSetting.getDeviceStatusNotify()) {
									// 发送redis消息
									redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), channel.getChannelId(), false);
								}
							}
							break;
						case CatalogEvent.DEFECT:
							// 故障
							logger.info("[收到通道视频故障通知] 来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
							if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
								logger.info("[收到通道视频故障通知] 但是平台已配置拒绝此消息，来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
							}else {
								updateChannelOfflineList.add(channel);
								if (updateChannelOfflineList.size() > 300) {
									executeSaveForOffline();
								}
								if (userSetting.getDeviceStatusNotify()) {
									// 发送redis消息
									redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), channel.getChannelId(), false);
								}
							}
							break;
						case CatalogEvent.ADD:
							// 增加
							logger.info("[收到增加通道通知] 来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
							// 判断此通道是否存在
							DeviceChannel deviceChannel = deviceChannelService.getOne(deviceId, channel.getChannelId());
							if (deviceChannel != null) {
								channel.setId(deviceChannel.getId());
								updateChannelMap.put(channel.getChannelId(), channel);
								if (updateChannelMap.keySet().size() > 300) {
									executeSaveForUpdate();
								}
							}else {
								addChannelMap.put(channel.getChannelId(), channel);
								if (userSetting.getDeviceStatusNotify()) {
									// 发送redis消息
									redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), channel.getChannelId(), true);
								}

								if (addChannelMap.keySet().size() > 300) {
									executeSaveForAdd();
								}
							}

							break;
						case CatalogEvent.DEL:
							// 删除
							logger.info("[收到删除通道通知] 来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
							deleteChannelList.add(channel);
							if (userSetting.getDeviceStatusNotify()) {
								// 发送redis消息
								redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), channel.getChannelId(), false);
							}
							if (deleteChannelList.size() > 300) {
								executeSaveForDelete();
							}
							break;
						case CatalogEvent.UPDATE:
							// 更新
							logger.info("[收到更新通道通知] 来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
							// 判断此通道是否存在
							DeviceChannel deviceChannelForUpdate = deviceChannelService.getOne(deviceId, channel.getChannelId());
							if (deviceChannelForUpdate != null) {
								channel.setId(deviceChannelForUpdate.getId());
								updateChannelMap.put(channel.getChannelId(), channel);
								if (updateChannelMap.keySet().size() > 300) {
									executeSaveForUpdate();
								}
							}else {
								addChannelMap.put(channel.getChannelId(), channel);
								if (addChannelMap.keySet().size() > 300) {
									executeSaveForAdd();
								}
								if (userSetting.getDeviceStatusNotify()) {
									// 发送redis消息
									redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), channel.getChannelId(), true);
								}
							}
							break;
						default:
							logger.warn("[ NotifyCatalog ] event not found ： {}", event );

					}
					// 转发变化信息
					eventPublisher.catalogEventPublish(null, channel, event);

					if (updateChannelMap.keySet().size() > 0
							|| addChannelMap.keySet().size() > 0
							|| updateChannelOnlineList.size() > 0
							|| updateChannelOfflineList.size() > 0
							|| deleteChannelList.size() > 0) {

						if (!dynamicTask.contains(talkKey)) {
							dynamicTask.startDelay(talkKey, this::executeSave, 1000);
						}
					}
				}
			}
		} catch (DocumentException e) {
			logger.error("未处理的异常 ", e);
		}
	}

	private void executeSave(){
		executeSaveForAdd();
		executeSaveForUpdate();
		executeSaveForDelete();
		executeSaveForOnline();
		executeSaveForOffline();
		dynamicTask.stop(talkKey);
	}

	private void executeSaveForUpdate(){
		if (updateChannelMap.values().size() > 0) {
			ArrayList<DeviceChannel> deviceChannels = new ArrayList<>(updateChannelMap.values());
			updateChannelMap.clear();
			deviceChannelService.batchUpdateChannel(deviceChannels);
		}

	}

	private void executeSaveForAdd(){
		if (addChannelMap.values().size() > 0) {
			ArrayList<DeviceChannel> deviceChannels = new ArrayList<>(addChannelMap.values());
			addChannelMap.clear();
			deviceChannelService.batchAddChannel(deviceChannels);
		}
	}

	private void executeSaveForDelete(){
		if (deleteChannelList.size() > 0) {
			deviceChannelService.deleteChannels(deleteChannelList);
			deleteChannelList.clear();
		}
	}

	private void executeSaveForOnline(){
		if (updateChannelOnlineList.size() > 0) {
			deviceChannelService.channelsOnline(updateChannelOnlineList);
			updateChannelOnlineList.clear();
		}
	}

	private void executeSaveForOffline(){
		if (updateChannelOfflineList.size() > 0) {
			deviceChannelService.channelsOffline(updateChannelOfflineList);
			updateChannelOfflineList.clear();
		}
	}

}
