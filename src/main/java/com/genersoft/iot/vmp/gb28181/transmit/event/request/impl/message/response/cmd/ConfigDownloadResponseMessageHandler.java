package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

@Slf4j
@Component
public class ConfigDownloadResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "ConfigDownload";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private DeferredResultHolder deferredResultHolder;

    @Autowired
    private IDeviceService deviceService;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }


    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        String channelId = getText(element, "DeviceID");
        String key;
        if (device.getDeviceId().equals(channelId)) {
            key = DeferredResultHolder.CALLBACK_CMD_CONFIGDOWNLOAD + device.getDeviceId();
        }else {
            key = DeferredResultHolder.CALLBACK_CMD_CONFIGDOWNLOAD + device.getDeviceId() + channelId;
        }
        try {
            // 回复200 OK
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 设备配置查询: {}", e.getMessage());
        }
        // 此处是对本平台发出DeviceControl指令的应答
        JSONObject json = new JSONObject();
        XmlUtil.node2Json(element, json);
        if (log.isDebugEnabled()) {
            log.debug(json.toJSONString());
        }
        JSONObject basicParam = json.getJSONObject("BasicParam");
        Integer heartBeatInterval = basicParam.getInteger("HeartBeatInterval");
        Integer heartBeatCount = basicParam.getInteger("HeartBeatCount");
        Integer positionCapability = basicParam.getInteger("PositionCapability");
        device.setHeartBeatInterval(heartBeatInterval);
        device.setHeartBeatCount(heartBeatCount);
        device.setPositionCapability(positionCapability);

        deviceService.updateDeviceHeartInfo(device);



        RequestMessage msg = new RequestMessage();
        msg.setKey(key);
        msg.setData(json);
        deferredResultHolder.invokeAllResult(msg);


    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {
        // 不会收到上级平台的心跳信息

    }
}
