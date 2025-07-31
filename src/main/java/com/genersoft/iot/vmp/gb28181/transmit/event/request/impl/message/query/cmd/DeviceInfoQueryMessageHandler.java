package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.cmd;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.QueryMessageHandler;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
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

@Slf4j
@Component
public class DeviceInfoQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "DeviceInfo";

    @Autowired
    private QueryMessageHandler queryMessageHandler;

    @Autowired
    private SIPCommanderForPlatform cmderFroPlatform;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Override
    public void afterPropertiesSet() throws Exception {
        queryMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {

    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform platform, Element rootElement) {
        log.info("[DeviceInfo查询]消息");
        SIPRequest request = (SIPRequest) evt.getRequest();
        FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);

        String sn = rootElement.element("SN").getText();

        /*根据WVP原有的数据结构，设备和通道是分开放置，设备信息都是存放在设备表里，通道表里的设备信息不可作为真实信息处理
        大部分NVR/IPC设备对他的通道信息实现都是返回默认的值没有什么参考价值。NVR/IPC通道我们统一使用设备表的设备信息来作为返回。
        我们这里使用查询数据库的方式来实现这个设备信息查询的功能，在其他地方对设备信息更新达到正确的目的。*/

        String channelId = getText(rootElement, "DeviceID");
        // 查询这是通道id还是设备id
        if (platform.getDeviceGBId().equals(channelId)) {
            // id指向平台的国标编号，那么就是查询平台的信息
            try {
                cmderFroPlatform.deviceInfoResponse(platform, null, sn, fromHeader.getTag());
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 国标级联 DeviceInfo查询回复: {}", e.getMessage());
            }
            return;
        }
        CommonGBChannel channel = channelService.queryOneWithPlatform(platform.getId(), channelId);
        if (channel == null) {
            // 不存在则回复404
            log.warn("[DeviceInfo] 通道不存在： 通道编号： {}", channelId);
            try {
                responseAck(request, Response.NOT_FOUND, "channel not found or offline");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] DeviceInfo查询回复: {}", e.getMessage());
                return;
            }
            return;
        }
        // 判断通道类型
        if (channel.getDataType() != ChannelDataType.GB28181) {
            // 非国标通道不支持录像回放控制
            log.warn("[DeviceInfo] 非国标通道不支持录像回放控制： 通道ID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] DeviceInfo查询回复: {}", e.getMessage());
                return;
            }
            return;
        }

        // 根据通道ID，获取所属设备
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // 不存在则回复404
            log.warn("[DeviceInfo] 通道所属设备不存在， 通道ID： {}", channel.getDataDeviceId());

            try {
                responseAck(request, Response.NOT_FOUND, "device not found ");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] DeviceInfo查询回复: {}", e.getMessage());
                return;
            }
            return;
        }
        try {
            // 回复200 OK
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] DeviceInfo查询回复: {}", e.getMessage());
            return;
        }
        try {
            cmderFroPlatform.deviceInfoResponse(platform, device, sn, fromHeader.getTag());
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 DeviceInfo查询回复: {}", e.getMessage());
        }
    }
}
