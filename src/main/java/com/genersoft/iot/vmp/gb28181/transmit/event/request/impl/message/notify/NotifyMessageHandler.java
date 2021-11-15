package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify;

import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.MessageHandlerAbstract;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class NotifyMessageHandler extends MessageHandlerAbstract implements InitializingBean  {

    private final String messageType = "Notify";

    @Override
    public void afterPropertiesSet() throws Exception {
        messageRequestProcessor.addHandler(messageType, this);
    }
}
