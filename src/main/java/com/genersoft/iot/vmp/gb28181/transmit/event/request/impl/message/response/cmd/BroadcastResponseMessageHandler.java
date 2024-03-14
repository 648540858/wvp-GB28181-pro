package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.AudioBroadcastCatch;
import com.genersoft.iot.vmp.gb28181.bean.AudioBroadcastCatchStatus;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.service.IPlayService;
import gov.nist.javax.sip.message.SIPRequest;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

@Component
public class BroadcastResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(BroadcastResponseMessageHandler.class);
    private final String cmdType = "Broadcast";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private IPlayService playService;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {

        SIPRequest request = (SIPRequest) evt.getRequest();
        try {
            String channelId = getText(rootElement, "DeviceID");
            if (!audioBroadcastManager.exit(device.getDeviceId(), channelId)) {
                // 回复410
                responseAck((SIPRequest) evt.getRequest(), Response.GONE);
                return;
            }
            String result = getText(rootElement, "Result");
            Element infoElement = rootElement.element("Info");
            String reason = null;
            if (infoElement != null) {
                reason = getText(infoElement, "Reason");
            }
            logger.info("[语音广播]回复：{}, {}/{}", reason == null? result : result + ": " + reason, device.getDeviceId(), channelId );

            // 回复200 OK
            responseAck(request, Response.OK);
            if (result.equalsIgnoreCase("OK")) {
                AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(device.getDeviceId(), channelId);
                audioBroadcastCatch.setStatus(AudioBroadcastCatchStatus.WaiteInvite);
                audioBroadcastManager.update(audioBroadcastCatch);
            }else {
                playService.stopAudioBroadcast(device.getDeviceId(), channelId);
            }
        } catch (ParseException | SipException | InvalidArgumentException e) {
            logger.error("[命令发送失败] 国标级联 语音喊话: {}", e.getMessage());
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element element) {

    }
}
