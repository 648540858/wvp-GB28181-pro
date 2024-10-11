package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.HandlerCatchData;
import com.genersoft.iot.vmp.gb28181.bean.NotifyCatalogChannel;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import gov.nist.javax.sip.message.SIPRequest;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sip.RequestEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.header.FromHeader;
import javax.sip.message.Request;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SIP命令类型： NOTIFY请求中的目录请求处理
 */
@Component
public class NotifyRequestForCatalogProcessor extends SIPRequestProcessorParent {

    private final static Logger logger = LoggerFactory.getLogger(NotifyRequestForCatalogProcessor.class);

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
//	@Scheduled(fixedRate = 2000)   //每400毫秒执行一次
//	public void showSize(){
//		logger.warn("[notify-目录订阅] 待处理消息数量： {}", taskQueue.size() );
//	}

    @Scheduled(fixedRate = 400)   //每400毫秒执行一次
    @Transactional
    public void executeTaskQueue() {
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
//        while (!taskQueue.isEmpty()) {
//            handlerCatchDataList.add(taskQueue.poll());
//        }
        if (handlerCatchDataList.isEmpty()) {
            return;
        }
        for (HandlerCatchData take : handlerCatchDataList) {
            if (take == null) {
                logger.warn("[收到目录订阅]：但是队列内任务为空");
                continue;
            }
            RequestEvent evt = take.getEvt();
            try {
                FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
                String deviceId = SipUtils.getUserIdFromFromHeader(fromHeader);

                Device device = redisCatchStorage.getDevice(deviceId);
                if (device == null || !device.isOnLine()) {
                    logger.warn("[收到目录订阅]：{}, 但是设备已经离线", (device != null ? device.getDeviceId() : ""));
                    continue;
                }
                Element rootElement = getRootElement(evt, device.getCharset());
                if (rootElement == null) {
                    logger.warn("[ 收到目录订阅 ] content cannot be null, {}", evt.getRequest());
                    continue;
                }
                Element deviceListElement = rootElement.element("DeviceList");
                if (deviceListElement == null) {
                    logger.warn("[ 收到目录订阅 ] 解析xml获取DeviceList失败, {}", evt.getRequest());
                    continue;
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
                        if (logger.isDebugEnabled()) {
                            logger.debug("[收到目录订阅]：{}/{}", device.getDeviceId(), channel.getChannelId());
                        }
                        switch (event) {
                            case CatalogEvent.ON:
                                // 上线
                                logger.info("[收到通道上线通知] 来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
                                channel.setStatus(true);
                                channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel));
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
                                    channel.setStatus(false);
                                    channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel));
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
                                    channel.setStatus(false);
                                    channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel));
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
                                    channel.setStatus(false);
                                    channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel));
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
                                    channel.setHasAudio(deviceChannel.getHasAudio());
                                    channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.UPDATE, channel));
                                } else {
                                    channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.ADD, channel));
                                    if (userSetting.getDeviceStatusNotify()) {
                                        // 发送redis消息
                                        redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), channel.getChannelId(), true);
                                    }
                                }

                                break;
                            case CatalogEvent.DEL:
                                // 删除
                                logger.info("[收到删除通道通知] 来自设备: {}, 通道 {}", device.getDeviceId(), channel.getChannelId());
                                channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.DELETE, channel));
                                if (userSetting.getDeviceStatusNotify()) {
                                    // 发送redis消息
                                    redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), channel.getChannelId(), false);
                                }
                                break;
                            case CatalogEvent.UPDATE:
                                // 更新
                                logger.info("[收到更新通道通知] 来自设备: {}, 通道 {}, 状态: {}", device.getDeviceId(), channel.getChannelId(), channel.isStatus());

                                // 判断此通道是否存在
                                DeviceChannel deviceChannelInDb = deviceChannelService.getOne(deviceId, channel.getChannelId());
                                if (deviceChannelInDb != null) {
                                    channel.setId(deviceChannelInDb.getId());
                                    channel.setUpdateTime(DateUtil.getNow());
                                    channel.setHasAudio(deviceChannelInDb.getHasAudio());
                                    channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.UPDATE, channel));
                                } else {
                                    channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.ADD, channel));
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
				logger.error("[存储收到的通道]类型：{}，编号：{}", notifyCatalogChannel.getType(),
						notifyCatalogChannel.getChannel().getDeviceId(), e);
			}
        }
    }
}
