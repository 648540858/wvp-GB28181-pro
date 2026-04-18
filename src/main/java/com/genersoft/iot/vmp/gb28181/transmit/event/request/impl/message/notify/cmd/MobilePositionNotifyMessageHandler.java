package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 移动设备位置数据通知，设备主动发起，不需要上级订阅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MobilePositionNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final NotifyMessageHandler notifyMessageHandler;

    private final ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

    private final EventPublisher eventPublisher;

    private final UserSetting userSetting;


    @Override
    public void afterPropertiesSet() throws Exception {
        String cmdType = "MobilePosition";
        notifyMessageHandler.addHandler(cmdType, this);
    }


    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
            log.error("[message-notify-移动位置] 待处理消息队列已满 {}，返回486 BUSY_HERE", userSetting.getMaxNotifyCountQueue());
            return;
        }
        taskQueue.offer(new HandlerCatchData(evt, device, rootElement));
        // 回复200 OK
        try {
            responseAckAsync((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 移动位置通知回复: {}", e.getMessage());
        }

    }
    @Scheduled(fixedDelay = 400)   //每400毫秒执行一次
    @Async
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
        List<DeviceMobilePosition> mobilePositionList = new ArrayList<>();
        for (HandlerCatchData take : handlerCatchDataList) {
            if (take == null) {
                continue;
            }
            Device device = take.getDevice();
            try {
                Element rootElementAfterCharset = getRootElement(take.getEvt(), device.getCharset());
                if (rootElementAfterCharset == null) {
                    log.warn("[移动位置通知] {}处理失败，未识别到信息体", device.getDeviceId());
                    continue;
                }
                List<DeviceMobilePosition> mobilePositions = DeviceMobilePosition.decode(device, rootElementAfterCharset);
                for (DeviceMobilePosition mobilePosition : mobilePositions) {
                    try {
                        log.info("[收到移动位置订阅通知]：{}/{}->{}.{}, 时间： {}", device.getDeviceId(), mobilePosition.getChannelDeviceId(),
                                mobilePosition.getLongitude(), mobilePosition.getLatitude(), mobilePosition.getTimestamp());
                        mobilePositionList.add(mobilePosition);
                    }catch (Exception e) {
                        log.error("未处理的异常 ", e);
                    }
                }
            }catch (Exception e) {
                log.warn("[移动位置通知] 发现未处理的异常, \r\n{}", take.getEvt().getRequest());
                log.error("[移动位置通知] 异常内容： ", e);
            }
        }
        // 向关联了该通道并且开启移动位置订阅的上级平台发送移动位置订阅消息
        if (!mobilePositionList.isEmpty()) {
            try {
                eventPublisher.mobilePositionsEventPublish(mobilePositionList);
            }catch (Exception e) {
                log.error("[MobilePositionEvent] 发送失败：  ", e);
            }
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {

    }
}
