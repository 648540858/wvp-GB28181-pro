package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SIP命令类型： NOTIFY请求中的目录请求处理
 */
@Slf4j
@Component
public class NotifyRequestForCatalogProcessor extends SIPRequestProcessorParent {

    private final ConcurrentLinkedQueue<NotifyCatalogChannel> channelList = new ConcurrentLinkedQueue<>();

	private ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IDeviceChannelService deviceChannelService;

//	@Scheduled(fixedRate = 2000)   //每400毫秒执行一次
//	public void showSize(){
//		log.warn("[notify-目录订阅] 待处理消息数量： {}", taskQueue.size() );
//	}

	public void process(RequestEvent evt) {
		if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
			log.error("[notify-目录订阅] 待处理消息队列已满 {}，返回486 BUSY_HERE，消息不做处理", userSetting.getMaxNotifyCountQueue());
			return;
		}
		taskQueue.offer(new HandlerCatchData(evt, null, null));
	}

	@Scheduled(fixedDelay = 400)   //每400毫秒执行一次
	public void executeTaskQueue(){
		if (taskQueue.isEmpty()) {
			return;
		}
		List<HandlerCatchData> handlerCatchDataList = new ArrayList<>();
		int size = taskQueue.size();
		for (int i = 0; i < size; i++) {
			HandlerCatchData poll = taskQueue.poll();
			if (poll != null) {
				handlerCatchDataList.add(poll);
			}
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

				Device device = redisCatchStorage.getDevice(deviceId);
				if (device == null || !device.isOnLine()) {
					log.warn("[收到目录订阅]：{}, 但是设备已经离线", (device != null ? device.getDeviceId() : ""));
					continue;
				}
				Element rootElement = getRootElement(evt, device.getCharset());
				if (rootElement == null) {
					log.warn("[ 收到目录订阅 ] content cannot be null, {}", evt.getRequest());
					continue;
				}
				Element deviceListElement = rootElement.element("DeviceList");
				if (deviceListElement == null) {
					log.warn("[ 收到目录订阅 ] content cannot be null, {}", evt.getRequest());
					continue;
				}
				Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
				if (deviceListIterator != null) {

					// 遍历DeviceList
					while (deviceListIterator.hasNext()) {
						Element itemDevice = deviceListIterator.next();
						CatalogChannelEvent catalogChannelEvent = null;
                        try {
                            catalogChannelEvent = CatalogChannelEvent.decode(itemDevice);
							if (catalogChannelEvent.getChannel() == null) {
								log.info("[解析CatalogChannelEvent]成功：但是解析通道信息失败， 原文如下： \n{}", new String(evt.getRequest().getRawContent()));
								continue;
							}
							catalogChannelEvent.getChannel().setDeviceDbId(device.getId());
                        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                                 IllegalAccessException e) {
                            log.error("[解析CatalogChannelEvent]失败，", e);
                            log.error("[解析CatalogChannelEvent]失败原文: \n{}", new String(evt.getRequest().getRawContent(), Charset.forName(device.getCharset())));
							continue;
                        }
						if (log.isDebugEnabled()){
							log.debug("[收到目录订阅]：{}/{}-{}", device.getDeviceId(),
									catalogChannelEvent.getChannel().getDeviceId(), catalogChannelEvent.getEvent());
						}
						DeviceChannel channel = catalogChannelEvent.getChannel();
						switch (catalogChannelEvent.getEvent()) {
							case CatalogEvent.ON:
								// 上线
								log.info("[收到通道上线通知] 来自设备: {}, 通道 {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								channel.setStatus("ON");
								channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel));
								if (userSetting.getDeviceStatusNotify()) {
									// 发送redis消息
									redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), true);
								}
								break;
							case CatalogEvent.OFF:
								// 离线
								log.info("[收到通道离线通知] 来自设备: {}, 通道 {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
									log.info("[收到通道离线通知] 但是平台已配置拒绝此消息，来自设备: {}, 通道 {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								} else {
									channel.setStatus("OFF");
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel));
									if (userSetting.getDeviceStatusNotify()) {
										// 发送redis消息
										redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), false);
									}
								}
								break;
							case CatalogEvent.VLOST:
								// 视频丢失
								log.info("[收到通道视频丢失通知] 来自设备: {}, 通道 {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
									log.info("[收到通道视频丢失通知] 但是平台已配置拒绝此消息，来自设备: {}, 通道 {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								} else {
									channel.setStatus("OFF");
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel));
									if (userSetting.getDeviceStatusNotify()) {
										// 发送redis消息
										redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), false);
									}
								}
								break;
							case CatalogEvent.DEFECT:
								// 故障
								log.info("[收到通道视频故障通知] 来自设备: {}, 通道 {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
									log.info("[收到通道视频故障通知] 但是平台已配置拒绝此消息，来自设备: {}, 通道 {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								} else {
									channel.setStatus("OFF");
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel));
									if (userSetting.getDeviceStatusNotify()) {
										// 发送redis消息
										redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), false);
									}
								}
								break;
							case CatalogEvent.ADD:
								// 增加
								log.info("[收到增加通道通知] 来自设备: {}, 通道 {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								// 判断此通道是否存在
								DeviceChannel deviceChannel = deviceChannelService.getOneForSource(device.getId(), catalogChannelEvent.getChannel().getDeviceId());
								if (deviceChannel != null) {
									log.info("[增加通道] 已存在，不发送通知只更新，设备: {}, 通道 {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
									channel.setId(deviceChannel.getId());
									channel.setHasAudio(deviceChannel.isHasAudio());
									channel.setUpdateTime(DateUtil.getNow());
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.UPDATE, channel));
								} else {
									catalogChannelEvent.getChannel().setUpdateTime(DateUtil.getNow());
									catalogChannelEvent.getChannel().setCreateTime(DateUtil.getNow());
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.ADD, channel));
									if (userSetting.getDeviceStatusNotify()) {
										// 发送redis消息
										redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), true);
									}
								}

								break;
							case CatalogEvent.DEL:
								// 删除
								log.info("[收到删除通道通知] 来自设备: {}, 通道 {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.DELETE, channel));
								if (userSetting.getDeviceStatusNotify()) {
									// 发送redis消息
									redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), false);
								}
								break;
							case CatalogEvent.UPDATE:
								// 更新
								log.info("[收到更新通道通知] 来自设备: {}, 通道 {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								// 判断此通道是否存在
								DeviceChannel deviceChannelForUpdate = deviceChannelService.getOneForSource(device.getId(), catalogChannelEvent.getChannel().getDeviceId());
								if (deviceChannelForUpdate != null) {
									channel.setId(deviceChannelForUpdate.getId());
									channel.setHasAudio(deviceChannelForUpdate.isHasAudio());
									channel.setUpdateTime(DateUtil.getNow());
									channel.setUpdateTime(DateUtil.getNow());
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.UPDATE, channel));
								} else {
									catalogChannelEvent.getChannel().setCreateTime(DateUtil.getNow());
									catalogChannelEvent.getChannel().setUpdateTime(DateUtil.getNow());
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.ADD, channel));
									if (userSetting.getDeviceStatusNotify()) {
										// 发送redis消息
										redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), true);
									}
								}
								break;
							default:
								log.warn("[ NotifyCatalog ] event not found ： {}", catalogChannelEvent.getEvent());

						}
						// 转发变化信息
						eventPublisher.catalogEventPublish(null, catalogChannelEvent.getChannel(), catalogChannelEvent.getEvent());
					}
				}

			} catch (DocumentException e) {
				log.error("未处理的异常 ", e);
			}
		}
		if (!channelList.isEmpty()) {
			executeSave();
		}
	}

	@Transactional
	public void executeSave() {
		int size = channelList.size();
		List<NotifyCatalogChannel> channelListForSave = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			channelListForSave.add(channelList.poll());
		}

		for (NotifyCatalogChannel notifyCatalogChannel : channelListForSave) {
			try {
				switch (notifyCatalogChannel.getType()) {
					case STATUS_CHANGED:
						deviceChannelService.updateChannelStatus(notifyCatalogChannel.getChannel());
						break;
					case ADD:
						deviceChannelService.addChannel(notifyCatalogChannel.getChannel());
						break;
					case UPDATE:
						deviceChannelService.updateChannelForNotify(notifyCatalogChannel.getChannel());
						break;
					case DELETE:
						deviceChannelService.delete(notifyCatalogChannel.getChannel());
						break;
				}
			}catch (Exception e) {
				log.error("[存储收到的通道]类型：{}，编号：{}", notifyCatalogChannel.getType(),
						notifyCatalogChannel.getChannel().getDeviceId(), e);
			}
		}
	}
}
