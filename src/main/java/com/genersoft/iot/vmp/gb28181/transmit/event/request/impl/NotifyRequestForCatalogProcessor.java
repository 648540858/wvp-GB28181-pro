package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.HandlerCatchData;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
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

import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
	private DynamicTask dynamicTask;

	@Autowired
	private SipConfig sipConfig;

	@Transactional
	public void process(RequestEvent evt) {
		if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
			logger.error("[notify-目录订阅] 待处理消息队列已满 {}，返回486 BUSY_HERE，消息不做处理", userSetting.getMaxNotifyCountQueue());
			return;
		}
		taskQueue.offer(new HandlerCatchData(evt, null, null));
	}

	@Scheduled(fixedRate = 400)   //每400毫秒执行一次
	public void executeTaskQueue(){
		if (taskQueue.isEmpty()) {
			return;
		}
		for (HandlerCatchData take : taskQueue) {
			if (take == null) {
				continue;
			}
			RequestEvent evt = take.getEvt();
			try {
				long start = System.currentTimeMillis();
				FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
				String deviceId = SipUtils.getUserIdFromFromHeader(fromHeader);

				Device device = redisCatchStorage.getDevice(deviceId);
				if (device == null || !device.isOnLine()) {
					logger.warn("[收到目录订阅]：{}, 但是设备已经离线", (device != null ? device.getDeviceId() : ""));
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
						Element eventElement = itemDevice.element("Event");
						String event;
						if (eventElement == null) {
							logger.warn("[收到目录订阅]：{}, 但是Event为空, 设为默认值 ADD", (device != null ? device.getDeviceId() : ""));
							event = CatalogEvent.ADD;
						} else {
							event = eventElement.getText().toUpperCase();
						}
						DeviceChannel channel = XmlUtil.channelContentHandler(itemDevice, device, event);
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
								if (userSetting.getDeviceStatusNotify()) {
									// 发送redis消息
									redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), channel.getChannelId(), true);
								}
								break;
							case CatalogEvent.OFF:
								// 离线
								logger.info("[收到通道离线通知] 来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
								if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
									logger.info("[收到通道离线通知] 但是平台已配置拒绝此消息，来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
								} else {
									updateChannelOfflineList.add(channel);
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
								} else {
									updateChannelOfflineList.add(channel);
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
								} else {
									updateChannelOfflineList.add(channel);
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
									logger.info("[增加通道] 已存在，不发送通知只更新，设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
									channel.setId(deviceChannel.getId());
									channel.setHasAudio(null);
									updateChannelMap.put(channel.getChannelId(), channel);
								} else {
									addChannelMap.put(channel.getChannelId(), channel);
									if (userSetting.getDeviceStatusNotify()) {
										// 发送redis消息
										redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), channel.getChannelId(), true);
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
								break;
							case CatalogEvent.UPDATE:
								// 更新
								logger.info("[收到更新通道通知] 来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
								// 判断此通道是否存在
								DeviceChannel deviceChannelForUpdate = deviceChannelService.getOne(deviceId, channel.getChannelId());
								if (deviceChannelForUpdate != null) {
									channel.setId(deviceChannelForUpdate.getId());
									channel.setUpdateTime(DateUtil.getNow());
									channel.setHasAudio(null);
									updateChannelMap.put(channel.getChannelId(), channel);
								} else {
									addChannelMap.put(channel.getChannelId(), channel);
									if (userSetting.getDeviceStatusNotify()) {
										// 发送redis消息
										redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), channel.getChannelId(), true);
									}
								}
								break;
							default:
								logger.warn("[ NotifyCatalog ] event not found ： {}", event);

						}
						// 转发变化信息
						eventPublisher.catalogEventPublish(null, channel, event);
					}
				}

			} catch (DocumentException e) {
				logger.error("未处理的异常 ", e);
			}
		}
		taskQueue.clear();
		if (!updateChannelMap.keySet().isEmpty()
				|| !addChannelMap.keySet().isEmpty()
				|| !updateChannelOnlineList.isEmpty()
				|| !updateChannelOfflineList.isEmpty()
				|| !deleteChannelList.isEmpty()) {
			executeSave();
		}
	}

	public void executeSave(){
		try {
			executeSaveForAdd();
		} catch (Exception e) {
			logger.error("[存储收到的增加通道] 异常： ", e );
		}
		try {
			executeSaveForOnline();
		} catch (Exception e) {
			logger.error("[存储收到的通道上线] 异常： ", e );
		}
		try {
			executeSaveForOffline();
		} catch (Exception e) {
			logger.error("[存储收到的通道离线] 异常： ", e );
		}
		try {
			executeSaveForUpdate();
		} catch (Exception e) {
			logger.error("[存储收到的更新通道] 异常： ", e );
		}
		try {
			executeSaveForDelete();
		} catch (Exception e) {
			logger.error("[存储收到的删除通道] 异常： ", e );
		}
	}

	private void executeSaveForUpdate(){
		if (!updateChannelMap.values().isEmpty()) {
			logger.info("[存储收到的更新通道], 数量： {}", updateChannelMap.size());
			ArrayList<DeviceChannel> deviceChannels = new ArrayList<>(updateChannelMap.values());
			deviceChannelService.batchUpdateChannel(deviceChannels);
			updateChannelMap.clear();
		}
	}

	private void executeSaveForAdd(){
		if (!addChannelMap.values().isEmpty()) {
			ArrayList<DeviceChannel> deviceChannels = new ArrayList<>(addChannelMap.values());
			addChannelMap.clear();
			deviceChannelService.batchAddChannel(deviceChannels);
		}
	}

	private void executeSaveForDelete(){
		if (!deleteChannelList.isEmpty()) {
			deviceChannelService.deleteChannels(deleteChannelList);
			deleteChannelList.clear();
		}
	}

	private void executeSaveForOnline(){
		if (!updateChannelOnlineList.isEmpty()) {
			deviceChannelService.channelsOnline(updateChannelOnlineList);
			updateChannelOnlineList.clear();
		}
	}

	private void executeSaveForOffline(){
		if (!updateChannelOfflineList.isEmpty()) {
			deviceChannelService.channelsOffline(updateChannelOfflineList);
			updateChannelOfflineList.clear();
		}
	}

	@Scheduled(fixedRate = 10000)   //每1秒执行一次
	public void execute(){
		logger.info("[待处理Notify-目录订阅消息数量]: {}", taskQueue.size());
	}
}
