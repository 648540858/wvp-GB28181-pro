package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.cmd.CatalogQueryMessageHandler;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import gov.nist.javax.sip.message.SIPRequest;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

public abstract class MessageHandlerAbstract extends SIPRequestProcessorParent implements IMessageHandler{

    private Logger logger = LoggerFactory.getLogger(MessageHandlerAbstract.class);

    public Map<String, IMessageHandler> messageHandlerMap = new ConcurrentHashMap<>();

    @Autowired
    private IVideoManagerStorage storage;

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
                logger.error("[命令发送失败] 回复200 OK: {}", e.getMessage());
            }
            return;
        }
        IMessageHandler messageHandler = messageHandlerMap.get(cmd);

        if (messageHandler != null) {
            //两个国标平台互相级联时由于上一步判断导致本该在平台处理的消息 放到了设备的处理逻辑
            //所以对目录查询单独做了校验
            if(messageHandler instanceof CatalogQueryMessageHandler){
                ParentPlatform parentPlatform = storage.queryParentPlatByServerGBId(device.getDeviceId());
                messageHandler.handForPlatform(evt, parentPlatform, element);
                return;
            }
            messageHandler.handForDevice(evt, device, element);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element element) {
        String cmd = getText(element, "CmdType");
        IMessageHandler messageHandler = messageHandlerMap.get(cmd);
        if (messageHandler != null) {
            messageHandler.handForPlatform(evt, parentPlatform, element);
        }
    }
}
