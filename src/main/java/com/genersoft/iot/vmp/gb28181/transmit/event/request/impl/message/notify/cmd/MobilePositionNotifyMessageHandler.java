package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.service.IMobilePositionService;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 移动设备位置数据通知，设备主动发起，不需要上级订阅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MobilePositionNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "MobilePosition";

    private final NotifyMessageHandler notifyMessageHandler;

    private final IMobilePositionService mobilePositionService;

    private final IDeviceChannelService deviceChannelService;

    private final ConcurrentLinkedQueue<SipMsgInfo> taskQueue = new ConcurrentLinkedQueue<>();

    private final TaskExecutor taskExecutor;

    private final EventPublisher eventPublisher;

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {

        boolean isEmpty = taskQueue.isEmpty();
        taskQueue.offer(new SipMsgInfo(evt, device, rootElement));
        // 回复200 OK
        try {
            responseAckAsync((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 移动位置通知回复: {}", e.getMessage());
        }
        if (isEmpty) {
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    SipMsgInfo sipMsgInfo = taskQueue.poll();
                    try {
                        Element rootElementAfterCharset = getRootElement(sipMsgInfo.getEvt(), sipMsgInfo.getDevice().getCharset());
                        if (rootElementAfterCharset == null) {
                            log.warn("[移动位置通知] {}处理失败，未识别到信息体", device.getDeviceId());
                            continue;
                        }

                        List<MobilePosition> mobilePositionList = MobilePosition.decode(sipMsgInfo.getDevice().getName(), sipMsgInfo.getDevice().getDeviceId(), rootElementAfterCharset);
                        mobilePositionList.forEach(mobilePosition -> {
                            try {
                                // 更新device channel 的经纬度
                                DeviceChannel deviceChannel = deviceChannelService.getOne(device.getDeviceId(), mobilePosition.getChannelDeviceId());
                                if (deviceChannel == null) {
                                    log.warn("[解析移动位置通知] 未找到通道：{}/{}", device.getDeviceId(), mobilePosition.getChannelDeviceId());
                                    return;
                                }
                                mobilePosition.setChannelId(deviceChannel.getId());
                                mobilePosition.setReportSource("Mobile Position");

                                log.info("[收到移动位置订阅通知]：{}/{}->{}.{}, 时间： {}", mobilePosition.getDeviceId(), mobilePosition.getChannelDeviceId(),
                                        mobilePosition.getLongitude(), mobilePosition.getLatitude(), mobilePosition.getTime());

                                mobilePositionService.add(mobilePosition);
                                // 向关联了该通道并且开启移动位置订阅的上级平台发送移动位置订阅消息
                                try {
                                    eventPublisher.mobilePositionEventPublish(mobilePosition);
                                }catch (Exception e) {
                                    log.error("[MobilePositionEvent] 发送失败：  ", e);
                                }

                                deviceChannel.setLongitude(mobilePosition.getLongitude());
                                deviceChannel.setLatitude(mobilePosition.getLatitude());
                                deviceChannel.setGpsTime(mobilePosition.getTime());
                                deviceChannelService.updateChannelGPS(device, deviceChannel, mobilePosition);
                            }catch (Exception e) {
                                log.error("未处理的异常 ", e);
                            }
                        });

                    } catch (DocumentException e) {
                        log.error("未处理的异常 ", e);
                    } catch (Exception e) {
                        log.warn("[移动位置通知] 发现未处理的异常, \r\n{}", evt.getRequest());
                        log.error("[移动位置通知] 异常内容： ", e);
                    }
                }
            });
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {

    }
}
