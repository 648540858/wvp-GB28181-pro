package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.cmd;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.QueryMessageHandler;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import gov.nist.javax.sip.message.SIPRequest;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.message.Response;
import java.text.ParseException;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

@Component
public class DeviceInfoQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(DeviceInfoQueryMessageHandler.class);
    private final String cmdType = "DeviceInfo";

    @Autowired
    private QueryMessageHandler queryMessageHandler;

    @Autowired
    private SIPCommanderFroPlatform cmderFroPlatform;
    @Autowired
    private IVideoManagerStorage storager;

    @Override
    public void afterPropertiesSet() throws Exception {
        queryMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {

    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {
        logger.info("[DeviceInfo查询]消息");
        FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
        try {
            // 回复200 OK
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[命令发送失败] DeviceInfo查询回复: {}", e.getMessage());
            return;
        }
        String sn = rootElement.element("SN").getText();

        /*根据WVP原有的数据结构，设备和通道是分开放置，设备信息都是存放在设备表里，通道表里的设备信息不可作为真实信息处理
        大部分NVR/IPC设备对他的通道信息实现都是返回默认的值没有什么参考价值。NVR/IPC通道我们统一使用设备表的设备信息来作为返回。
        我们这里使用查询数据库的方式来实现这个设备信息查询的功能，在其他地方对设备信息更新达到正确的目的。*/

        String channelId = getText(rootElement, "DeviceID");
        // 查询这是通道id还是设备id
        Device device = null;
        // 如果id指向平台的国标编号，那么就是查询平台的信息
        if (!parentPlatform.getDeviceGBId().equals(channelId)) {
            device = storager.queryDeviceInfoByPlatformIdAndChannelId(parentPlatform.getServerGBId(), channelId);
            if (device ==null){
                logger.error("[平台没有该通道的使用权限]:platformId"+parentPlatform.getServerGBId()+"  deviceID:"+channelId);
                return;
            }
        }
        try {
            cmderFroPlatform.deviceInfoResponse(parentPlatform, device, sn, fromHeader.getTag());
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[命令发送失败] 国标级联 DeviceInfo查询回复: {}", e.getMessage());
        }
    }
}
