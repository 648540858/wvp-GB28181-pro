package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.utils.DateUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * 移动设备位置数据通知，设备主动发起，不需要上级订阅
 */
@Slf4j
@Component
public class MobilePositionNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "MobilePosition";

    @Autowired
    private NotifyMessageHandler notifyMessageHandler;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    private ConcurrentLinkedQueue<SipMsgInfo> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

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
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
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
                        String channelId = getText(rootElementAfterCharset, "DeviceID");
                        DeviceChannel deviceChannel = deviceChannelService.getOne(device.getDeviceId(), channelId);
                        if (deviceChannel == null) {
                            log.warn("[解析移动位置通知] 未找到通道：{}/{}", device.getDeviceId(), channelId);
                            continue;
                        }

                        MobilePosition mobilePosition = new MobilePosition();
                        mobilePosition.setCreateTime(DateUtil.getNow());
                        if (!ObjectUtils.isEmpty(sipMsgInfo.getDevice().getName())) {
                            mobilePosition.setDeviceName(sipMsgInfo.getDevice().getName());
                        }
                        mobilePosition.setDeviceId(sipMsgInfo.getDevice().getDeviceId());

                        mobilePosition.setChannelId(deviceChannel.getId());
                        String time = getText(rootElementAfterCharset, "Time");
                        if (ObjectUtils.isEmpty(time)){
                            mobilePosition.setTime(DateUtil.getNow());
                        }else {
                            mobilePosition.setTime(SipUtils.parseTime(time));
                        }
                        mobilePosition.setLongitude(Double.parseDouble(getText(rootElementAfterCharset, "Longitude")));
                        mobilePosition.setLatitude(Double.parseDouble(getText(rootElementAfterCharset, "Latitude")));
                        if (NumericUtil.isDouble(getText(rootElementAfterCharset, "Speed"))) {
                            mobilePosition.setSpeed(Double.parseDouble(getText(rootElementAfterCharset, "Speed")));
                        } else {
                            mobilePosition.setSpeed(0.0);
                        }
                        if (NumericUtil.isDouble(getText(rootElementAfterCharset, "Direction"))) {
                            mobilePosition.setDirection(Double.parseDouble(getText(rootElementAfterCharset, "Direction")));
                        } else {
                            mobilePosition.setDirection(0.0);
                        }
                        if (NumericUtil.isDouble(getText(rootElementAfterCharset, "Altitude"))) {
                            mobilePosition.setAltitude(Double.parseDouble(getText(rootElementAfterCharset, "Altitude")));
                        } else {
                            mobilePosition.setAltitude(0.0);
                        }
                        mobilePosition.setReportSource("Mobile Position");

                        // 更新device channel 的经纬度
                        deviceChannel.setLongitude(mobilePosition.getLongitude());
                        deviceChannel.setLatitude(mobilePosition.getLatitude());
                        deviceChannel.setGpsTime(mobilePosition.getTime());

                        deviceChannelService.updateChannelGPS(device, deviceChannel, mobilePosition);

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
