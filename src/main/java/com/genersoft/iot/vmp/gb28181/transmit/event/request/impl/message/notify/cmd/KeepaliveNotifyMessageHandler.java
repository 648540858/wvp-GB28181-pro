package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.ViaHeader;
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

    @Autowired
    private IVideoManagerStorage videoManagerStorager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

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
                // 判断RPort是否改变，改变则说明路由nat信息变化，修改设备信息
                // 获取到通信地址等信息
                ViaHeader viaHeader = (ViaHeader) evt.getRequest().getHeader(ViaHeader.NAME);
                String received = viaHeader.getReceived();
                int rPort = viaHeader.getRPort();
                // 解析本地地址替代
                if (StringUtils.isEmpty(received) || rPort == -1) {
                    received = viaHeader.getHost();
                    rPort = viaHeader.getPort();
                }
                if (device.getPort() != rPort) {
                    device.setPort(rPort);
                    device.setHostAddress(received.concat(":").concat(String.valueOf(rPort)));
                    videoManagerStorager.updateDevice(device);
                    redisCatchStorage.updateDevice(device);
                }
                if (!redisCatchStorage.deviceIsOnline(device.getDeviceId())) {
                    publisher.onlineEventPublish(device, VideoManagerConstants.EVENT_ONLINE_KEEPLIVE);
                }
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
