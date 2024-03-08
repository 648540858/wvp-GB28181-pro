package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify;

import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.MessageHandlerAbstract;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.MessageRequestProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 命令类型： 通知命令， 参看 A.2.5 通知命令
 * 命令类型： 状态信息(心跳)报送, 报警通知, 媒体通知, 移动设备位置数据，语音广播通知(TODO), 设备预置位(TODO)
 * @author lin
 */
@Component
public class NotifyMessageHandler extends MessageHandlerAbstract implements InitializingBean  {

    private final String messageType = "Notify";

    @Autowired
    private MessageRequestProcessor messageRequestProcessor;

    @Override
    public void afterPropertiesSet() throws Exception {
        messageRequestProcessor.addHandler(messageType, this);
    }
}
