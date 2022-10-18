package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control.cmd;

import com.genersoft.iot.vmp.VManageBootstrap;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control.ControlMessageHandler;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.SpringBeanFactory;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.SIPRequest;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.sip.*;
import javax.sip.address.SipURI;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Iterator;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

@Component
public class DeviceControlQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(DeviceControlQueryMessageHandler.class);
    private final String cmdType = "DeviceControl";

    @Autowired
    private ControlMessageHandler controlMessageHandler;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private SIPCommanderFroPlatform cmderFroPlatform;

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void afterPropertiesSet() throws Exception {
        controlMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {

    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {

        SIPRequest request = (SIPRequest) evt.getRequest();

        // 此处是上级发出的DeviceControl指令
        String targetGBId = ((SipURI) request.getToHeader().getAddress().getURI()).getUser();
        String channelId = getText(rootElement, "DeviceID");
        // 远程启动功能
        if (!ObjectUtils.isEmpty(getText(rootElement, "TeleBoot"))) {
            if (parentPlatform.getServerGBId().equals(targetGBId)) {
                // 远程启动本平台：需要在重新启动程序后先对SipStack解绑
                logger.info("执行远程启动本平台命令");
                try {
                    cmderFroPlatform.unregister(parentPlatform, null, null);
                } catch (InvalidArgumentException | ParseException | SipException e) {
                    logger.error("[命令发送失败] 国标级联 注销: {}", e.getMessage());
                }
                taskExecutor.execute(()->{
                    try {
                        Thread.sleep(3000);
                        SipProvider up = (SipProvider) SpringBeanFactory.getBean("udpSipProvider");
                        SipStackImpl stack = (SipStackImpl)up.getSipStack();
                        stack.stop();
                        Iterator listener = stack.getListeningPoints();
                        while (listener.hasNext()) {
                            stack.deleteListeningPoint((ListeningPoint) listener.next());
                        }
                        Iterator providers = stack.getSipProviders();
                        while (providers.hasNext()) {
                            stack.deleteSipProvider((SipProvider) providers.next());
                        }
                        VManageBootstrap.restart();
                    } catch (InterruptedException | ObjectInUseException e) {
                        logger.error("[任务执行失败] 服务重启: {}", e.getMessage());
                    }
                });
            } else {
                // 远程启动指定设备
            }
        }
        // 云台/前端控制命令
        if (!ObjectUtils.isEmpty(getText(rootElement,"PTZCmd")) && !parentPlatform.getServerGBId().equals(targetGBId)) {
            String cmdString = getText(rootElement,"PTZCmd");
            Device deviceForPlatform = storager.queryVideoDeviceByPlatformIdAndChannelId(parentPlatform.getServerGBId(), channelId);
            if (deviceForPlatform == null) {
                try {
                    responseAck(request, Response.NOT_FOUND);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    logger.error("[命令发送失败] 错误信息: {}", e.getMessage());
                }
                return;
            }
            try {
                cmder.fronEndCmd(deviceForPlatform, channelId, cmdString, eventResult -> {
                    // 失败的回复
                    try {
                        responseAck(request, eventResult.statusCode, eventResult.msg);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] 云台/前端回复: {}", e.getMessage());
                    }
                }, eventResult -> {
                    // 成功的回复
                    try {
                        responseAck(request, eventResult.statusCode);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] 云台/前端回复: {}", e.getMessage());
                    }
                });
            } catch (InvalidArgumentException | SipException | ParseException e) {
                logger.error("[命令发送失败] 云台/前端: {}", e.getMessage());
            }
        }
    }
}
