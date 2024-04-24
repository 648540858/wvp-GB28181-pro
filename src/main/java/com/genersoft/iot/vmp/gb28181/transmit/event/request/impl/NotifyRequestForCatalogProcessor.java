package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

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
import com.genersoft.iot.vmp.utils.DateUtil;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
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
	private SipConfig sipConfig;

	@Transactional
	public void process(List<RequestEvent> evtList) {
		if (evtList.isEmpty()) {
			return;
		}
		for (RequestEvent evt : evtList) {
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
						Element eventElement = itemDevice.element("Event");
						String event;
						if (eventElement == null) {
							logger.warn("[收到目录订阅]：{}, 但是Event为空, 设为默认值 ADD", (device != null ? device.getDeviceId():"" ));
							event = CatalogEvent.ADD;
						}else {
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
						logger.info("[收到目录订阅]：{}, {}/{}",event, device.getDeviceId(), channel.getChannelId());
						switch (event) {
							case CatalogEvent.ON:
								// 上线
								deviceChannelService.online(channel);
								if (userSetting.getDeviceStatusNotify()) {
									// 发送redis消息
									redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), channel.getChannelId(), true);
								}

								break;
							case CatalogEvent.OFF :
							case CatalogEvent.VLOST:
							case CatalogEvent.DEFECT:
								// 离线
								if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
									logger.info("[目录订阅] 离线 但是平台已配置拒绝此消息，来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
								}else {
									deviceChannelService.offline(channel);
									if (userSetting.getDeviceStatusNotify()) {
										// 发送redis消息
										redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), channel.getChannelId(), false);
									}
								}
								break;
							case CatalogEvent.DEL:
								// 删除
								deviceChannelService.delete(channel);
								if (userSetting.getDeviceStatusNotify()) {
									// 发送redis消息
									redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), channel.getChannelId(), false);
								}
								break;
							case CatalogEvent.ADD:
							case CatalogEvent.UPDATE:
								// 更新
								channel.setUpdateTime(DateUtil.getNow());
								channel.setHasAudio(null);
								deviceChannelService.updateChannel(deviceId,channel);
								if (userSetting.getDeviceStatusNotify()) {
									// 发送redis消息
									redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), channel.getChannelId(), true);
								}
								break;
							default:
								logger.warn("[ NotifyCatalog ] event not found ： {}", event );

						}
						// 转发变化信息
						eventPublisher.catalogEventPublish(null, channel, event);
					}
				}
			} catch (DocumentException e) {
				logger.error("未处理的异常 ", e);
			}
		}
	}
}
