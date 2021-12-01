package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control;

import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.MessageHandlerAbstract;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.MessageRequestProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 命令类型： 控制命令
 * 命令类型： 设备控制： 远程启动, 录像控制（TODO）, 报警布防/撤防命令（TODO）, 报警复位命令（TODO）,
 *                   强制关键帧命令（TODO）, 拉框放大/缩小控制命令（TODO）, 看守位控制（TODO）, 报警复位（TODO）
 * 命令类型： 设备配置： SVAC编码配置（TODO）, 音频参数（TODO）, SVAC解码配置（TODO）
 */
@Component
public class ControlMessageHandler extends MessageHandlerAbstract implements InitializingBean  {

    private final String messageType = "Control";

    @Autowired
    private MessageRequestProcessor messageRequestProcessor;

    @Override
    public void afterPropertiesSet() throws Exception {
        messageRequestProcessor.addHandler(messageType, this);
    }
}
