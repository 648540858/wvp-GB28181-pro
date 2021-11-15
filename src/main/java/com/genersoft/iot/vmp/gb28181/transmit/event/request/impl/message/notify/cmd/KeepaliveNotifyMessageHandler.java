package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
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

@Component
public class KeepaliveNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(KeepaliveNotifyMessageHandler.class);
    private final String cmdType = "Keepalive";

    @Autowired
    private NotifyMessageHandler notifyMessageHandler;

    @Autowired
    private EventPublisher publisher;

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        // 检查设备是否存在并在线， 不在线则设置为在线
        try {
            if (device != null ) {
                // 回复200 OK
                responseAck(evt, Response.OK);
                publisher.onlineEventPublish(device, VideoManagerConstants.EVENT_ONLINE_KEEPLIVE);
            }
        } catch (SipException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element element) {
        // 不会收到上级平台的心跳信息

    }
}
