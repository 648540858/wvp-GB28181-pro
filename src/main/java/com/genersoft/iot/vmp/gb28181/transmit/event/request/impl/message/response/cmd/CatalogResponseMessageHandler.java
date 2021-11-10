package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.event.DeviceOffLineDetector;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
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
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Iterator;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

@Component
public class CatalogResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(CatalogResponseMessageHandler.class);
    private final String cmdType = "Catalog";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private DeferredResultHolder deferredResultHolder;

    @Autowired
    private DeviceOffLineDetector offLineDetector;

    @Autowired
    private SipConfig config;

    @Autowired
    private EventPublisher publisher;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        String key = DeferredResultHolder.CALLBACK_CMD_CATALOG + device.getDeviceId();
        Element rootElement = null;
        try {
            rootElement = getRootElement(evt, device.getCharset());
            Element deviceListElement = rootElement.element("DeviceList");
            Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
            if (deviceListIterator != null) {

                // 遍历DeviceList
                while (deviceListIterator.hasNext()) {
                    Element itemDevice = deviceListIterator.next();
                    Element channelDeviceElement = itemDevice.element("DeviceID");
                    if (channelDeviceElement == null) {
                        continue;
                    }
                    String channelDeviceId = channelDeviceElement.getText();
                    Element channdelNameElement = itemDevice.element("Name");
                    String channelName = channdelNameElement != null ? channdelNameElement.getTextTrim().toString() : "";
                    Element statusElement = itemDevice.element("Status");
                    String status = statusElement != null ? statusElement.getText().toString() : "ON";
                    DeviceChannel deviceChannel = new DeviceChannel();
                    deviceChannel.setName(channelName);
                    deviceChannel.setChannelId(channelDeviceId);
                    // ONLINE OFFLINE  HIKVISION DS-7716N-E4 NVR的兼容性处理
                    if (status.equals("ON") || status.equals("On") || status.equals("ONLINE")) {
                        deviceChannel.setStatus(1);
                    }
                    if (status.equals("OFF") || status.equals("Off") || status.equals("OFFLINE")) {
                        deviceChannel.setStatus(0);
                    }

                    deviceChannel.setManufacture(getText(itemDevice, "Manufacturer"));
                    deviceChannel.setModel(getText(itemDevice, "Model"));
                    deviceChannel.setOwner(getText(itemDevice, "Owner"));
                    deviceChannel.setCivilCode(getText(itemDevice, "CivilCode"));
                    deviceChannel.setBlock(getText(itemDevice, "Block"));
                    deviceChannel.setAddress(getText(itemDevice, "Address"));
                    if (getText(itemDevice, "Parental") == null || getText(itemDevice, "Parental") == "") {
                        deviceChannel.setParental(0);
                    } else {
                        deviceChannel.setParental(Integer.parseInt(getText(itemDevice, "Parental")));
                    }
                    deviceChannel.setParentId(getText(itemDevice, "ParentID"));
                    if (getText(itemDevice, "SafetyWay") == null || getText(itemDevice, "SafetyWay") == "") {
                        deviceChannel.setSafetyWay(0);
                    } else {
                        deviceChannel.setSafetyWay(Integer.parseInt(getText(itemDevice, "SafetyWay")));
                    }
                    if (getText(itemDevice, "RegisterWay") == null || getText(itemDevice, "RegisterWay") == "") {
                        deviceChannel.setRegisterWay(1);
                    } else {
                        deviceChannel.setRegisterWay(Integer.parseInt(getText(itemDevice, "RegisterWay")));
                    }
                    deviceChannel.setCertNum(getText(itemDevice, "CertNum"));
                    if (getText(itemDevice, "Certifiable") == null || getText(itemDevice, "Certifiable") == "") {
                        deviceChannel.setCertifiable(0);
                    } else {
                        deviceChannel.setCertifiable(Integer.parseInt(getText(itemDevice, "Certifiable")));
                    }
                    if (getText(itemDevice, "ErrCode") == null || getText(itemDevice, "ErrCode") == "") {
                        deviceChannel.setErrCode(0);
                    } else {
                        deviceChannel.setErrCode(Integer.parseInt(getText(itemDevice, "ErrCode")));
                    }
                    deviceChannel.setEndTime(getText(itemDevice, "EndTime"));
                    deviceChannel.setSecrecy(getText(itemDevice, "Secrecy"));
                    deviceChannel.setIpAddress(getText(itemDevice, "IPAddress"));
                    if (getText(itemDevice, "Port") == null || getText(itemDevice, "Port") == "") {
                        deviceChannel.setPort(0);
                    } else {
                        deviceChannel.setPort(Integer.parseInt(getText(itemDevice, "Port")));
                    }
                    deviceChannel.setPassword(getText(itemDevice, "Password"));
                    if (NumericUtil.isDouble(getText(itemDevice, "Longitude"))) {
                        deviceChannel.setLongitude(Double.parseDouble(getText(itemDevice, "Longitude")));
                    } else {
                        deviceChannel.setLongitude(0.00);
                    }
                    if (NumericUtil.isDouble(getText(itemDevice, "Latitude"))) {
                        deviceChannel.setLatitude(Double.parseDouble(getText(itemDevice, "Latitude")));
                    } else {
                        deviceChannel.setLatitude(0.00);
                    }
                    if (getText(itemDevice, "PTZType") == null || getText(itemDevice, "PTZType") == "") {
                        deviceChannel.setPTZType(0);
                    } else {
                        deviceChannel.setPTZType(Integer.parseInt(getText(itemDevice, "PTZType")));
                    }
                    deviceChannel.setHasAudio(true); // 默认含有音频，播放时再检查是否有音频及是否AAC
                    storager.updateChannel(device.getDeviceId(), deviceChannel);
                }

                RequestMessage msg = new RequestMessage();
                msg.setKey(key);
                msg.setData(device);
                deferredResultHolder.invokeAllResult(msg);
                // 回复200 OK
                responseAck(evt, Response.OK);
                if (offLineDetector.isOnline(device.getDeviceId())) {
                    publisher.onlineEventPublish(device, VideoManagerConstants.EVENT_ONLINE_MESSAGE);
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {

    }
}
