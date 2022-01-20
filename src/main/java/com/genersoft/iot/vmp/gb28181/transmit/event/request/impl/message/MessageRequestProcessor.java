package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceNotFoundEvent;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

    private final static Logger logger = LoggerFactory.getLogger(MessageRequestProcessor.class);

    private final String method = "MESSAGE";

    private static Map<String, IMessageHandler> messageHandlerMap = new ConcurrentHashMap<>();

    @Autowired
    private SIPProcessorObserver sipProcessorObserver;

    @Autowired
    private IVideoManagerStorager storage;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 添加消息处理的订阅
        sipProcessorObserver.addRequestProcessor(method, this);
    }

    public void addHandler(String name, IMessageHandler handler) {
        messageHandlerMap.put(name, handler);
    }

    @Override
    public void process(RequestEvent evt) {
        logger.debug("接收到消息：" + evt.getRequest());
        String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
        CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);
        // 查询设备是否存在
        Device device = redisCatchStorage.getDevice(deviceId);
        // 查询上级平台是否存在
        ParentPlatform parentPlatform = storage.queryParentPlatByServerGBId(deviceId);
        try {
            if (device == null && parentPlatform == null) {
                // 不存在则回复404
                responseAck(evt, Response.NOT_FOUND, "device "+ deviceId +" not found");
                logger.warn("[设备未找到 ]： {}", deviceId);
                if (sipSubscribe.getErrorSubscribe(callIdHeader.getCallId()) != null){
                    SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult(new DeviceNotFoundEvent(evt.getDialog()));
                    sipSubscribe.getErrorSubscribe(callIdHeader.getCallId()).response(eventResult);
                };
            }else {
                Element rootElement = getRootElement(evt);
                String name = rootElement.getName();
                IMessageHandler messageHandler = messageHandlerMap.get(name);
                if (messageHandler != null) {
                    if (device != null) {
                        messageHandler.handForDevice(evt, device, rootElement);
                    }else { // 由于上面已经判断都为null则直接返回，所以这里device和parentPlatform必有一个不为null
                        messageHandler.handForPlatform(evt, parentPlatform, rootElement);
                    }
                }else {
                    // 不支持的message
                    // 不存在则回复415
                    responseAck(evt, Response.UNSUPPORTED_MEDIA_TYPE, "Unsupported message type, must Control/Notify/Query/Response");
                }
            }
        } catch (SipException e) {
            logger.warn("SIP 回复错误", e);
        } catch (InvalidArgumentException e) {
            logger.warn("参数无效", e);
        } catch (ParseException e) {
            logger.warn("SIP回复时解析异常", e);
        } catch (DocumentException e) {
            logger.warn("解析XML消息内容异常", e);
        }
    }


}
