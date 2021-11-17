package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sip.RequestEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

public abstract class MessageHandlerAbstract extends SIPRequestProcessorParent implements IMessageHandler{

    public static Map<String, Map<String, IMessageHandler>> messageHandlerMap = new ConcurrentHashMap<>();

    @Autowired
    public MessageRequestProcessor messageRequestProcessor;

    public void addHandler(String messageType, String cmdType, IMessageHandler messageHandler) {
        if (!messageHandlerMap.containsKey(cmdType)){
            messageHandlerMap.put(cmdType, new ConcurrentHashMap<>());
        }else{
            Map<String, IMessageHandler> messageAllHandlerMap = messageHandlerMap.get(cmdType);
            messageAllHandlerMap.put(messageType, messageHandler);
        }
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) throws DocumentException {
        String name = getRootElement(evt).getName();
        String cmd = getText(element, "CmdType");
        IMessageHandler messageHandler = messageHandlerMap.get(cmd).get(name);
        if (messageHandler != null) {
            messageHandler.handForDevice(evt, device, element);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element element) throws DocumentException {
        String name = getRootElement(evt).getName();
        String cmd = getText(element, "CmdType");
        IMessageHandler messageHandler = messageHandlerMap.get(cmd).get(name);
        if (messageHandler != null) {
            messageHandler.handForPlatform(evt, parentPlatform, element);
        }
    }
}
