package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.event.MessageSubscribe;
import com.genersoft.iot.vmp.gb28181.event.sip.MessageEvent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.MessageHandlerAbstract;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.MessageRequestProcessor;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * 命令类型： 请求动作的应答
 * 命令类型： 设备控制, 报警通知, 设备目录信息查询, 目录信息查询, 目录收到, 设备信息查询, 设备状态信息查询 ......
 */
@Component
public class ResponseMessageHandler extends MessageHandlerAbstract implements InitializingBean  {

    private final String messageType = "Response";

    @Autowired
    private MessageRequestProcessor messageRequestProcessor;

    @Autowired
    private MessageSubscribe messageSubscribe;

    @Override
    public void afterPropertiesSet() throws Exception {
        messageRequestProcessor.addHandler(messageType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        super.handForDevice(evt, device, element);
        handMessageEvent(element, null);
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
