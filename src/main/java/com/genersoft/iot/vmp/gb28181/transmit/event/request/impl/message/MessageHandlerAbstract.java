package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.event.MessageSubscribe;
import com.genersoft.iot.vmp.gb28181.event.sip.MessageEvent;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.cmd.CatalogQueryMessageHandler;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

@Slf4j
public abstract class MessageHandlerAbstract extends SIPRequestProcessorParent implements IMessageHandler{

    public Map<String, IMessageHandler> messageHandlerMap = new ConcurrentHashMap<>();

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private MessageSubscribe messageSubscribe;

    public void addHandler(String cmdType, IMessageHandler messageHandler) {
        messageHandlerMap.put(cmdType, messageHandler);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        String cmd = getText(element, "CmdType");
        if (cmd == null) {
            try {
                responseAck((SIPRequest) evt.getRequest(), Response.OK);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 回复200 OK: {}", e.getMessage());
            }
            return;
        }
        IMessageHandler messageHandler = messageHandlerMap.get(cmd);

        if (messageHandler != null) {
            //两个国标平台互相级联时由于上一步判断导致本该在平台处理的消息 放到了设备的处理逻辑
            //所以对目录查询单独做了校验
            if(messageHandler instanceof CatalogQueryMessageHandler){
                Platform parentPlatform = platformService.queryPlatformByServerGBId(device.getDeviceId());
                messageHandler.handForPlatform(evt, parentPlatform, element);
                return;
            }
            messageHandler.handForDevice(evt, device, element);
        }else {
            handMessageEvent(element, null);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {
        String cmd = getText(element, "CmdType");
        IMessageHandler messageHandler = messageHandlerMap.get(cmd);
        if (messageHandler != null) {
            messageHandler.handForPlatform(evt, parentPlatform, element);
        }
    }


    public void handMessageEvent(Element element, Object data) {

        String cmd = getText(element, "CmdType");
        String sn = getText(element, "SN");
        MessageEvent<Object> subscribe = (MessageEvent<Object>)messageSubscribe.getSubscribe(cmd + sn);
        if (subscribe != null && subscribe.getCallback() != null) {
            String result = getText(element, "Result");
            if (result == null || "OK".equalsIgnoreCase(result) || data != null) {
                subscribe.getCallback().run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
            }else {
                subscribe.getCallback().run(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg(), result);
            }
            messageSubscribe.removeSubscribe(cmd + sn);
        }
    }
}
