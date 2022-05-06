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
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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

    @Override
    public void afterPropertiesSet() throws Exception {
        controlMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {

    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {

        // 此处是上级发出的DeviceControl指令
        String targetGBId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
        String channelId = getText(rootElement, "DeviceID");
        // 远程启动功能
        if (!StringUtils.isEmpty(getText(rootElement, "TeleBoot"))) {
            if (parentPlatform.getServerGBId().equals(targetGBId)) {
                // 远程启动本平台：需要在重新启动程序后先对SipStack解绑
                logger.info("执行远程启动本平台命令");
                cmderFroPlatform.unregister(parentPlatform, null, null);

                Thread restartThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
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
                        } catch (InterruptedException ignored) {
                        } catch (ObjectInUseException e) {
                            e.printStackTrace();
                        }
                    }
                });

                restartThread.setDaemon(false);
                restartThread.start();
            } else {
                // 远程启动指定设备
            }
        }
        // 云台/前端控制命令
        if (!StringUtils.isEmpty(getText(rootElement,"PTZCmd")) && !parentPlatform.getServerGBId().equals(targetGBId)) {
            String cmdString = getText(rootElement,"PTZCmd");
            Device deviceForPlatform = storager.queryVideoDeviceByPlatformIdAndChannelId(parentPlatform.getServerGBId(), channelId);
            if (deviceForPlatform == null) {
                try {
                    responseAck(evt, Response.NOT_FOUND);
                    return;
                } catch (SipException e) {
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            cmder.fronEndCmd(deviceForPlatform, channelId, cmdString, eventResult -> {
                // 失败的回复
                try {
                    responseAck(evt, eventResult.statusCode, eventResult.msg);
                } catch (SipException e) {
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }, eventResult -> {
                // 成功的回复
                try {
                    responseAck(evt, eventResult.statusCode);
                } catch (SipException e) {
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
