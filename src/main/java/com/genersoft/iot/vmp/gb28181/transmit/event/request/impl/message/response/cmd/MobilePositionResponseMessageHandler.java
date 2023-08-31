package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import gov.nist.javax.sip.message.SIPRequest;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * 移动设备位置数据查询回复
 * @author lin
 */
@Component
public class MobilePositionResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(MobilePositionResponseMessageHandler.class);
    private final String cmdType = "MobilePosition";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        SIPRequest request = (SIPRequest) evt.getRequest();

        try {
            rootElement = getRootElement(evt, device.getCharset());
            if (rootElement == null) {
                logger.warn("[ 移动设备位置数据查询回复 ] content cannot be null, {}", evt.getRequest());
                try {
                    responseAck(request, Response.BAD_REQUEST);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    logger.error("[命令发送失败] 移动设备位置数据查询 BAD_REQUEST: {}", e.getMessage());
                }
                return;
            }
            MobilePosition mobilePosition = new MobilePosition();
            mobilePosition.setCreateTime(DateUtil.getNow());
            if (!ObjectUtils.isEmpty(device.getName())) {
                mobilePosition.setDeviceName(device.getName());
            }
            mobilePosition.setDeviceId(device.getDeviceId());
            mobilePosition.setChannelId(getText(rootElement, "DeviceID"));
            //兼容ISO 8601格式时间
            String time = getText(rootElement, "Time");
            if (ObjectUtils.isEmpty(time)){
                mobilePosition.setTime(DateUtil.getNow());
            }else {
                mobilePosition.setTime(SipUtils.parseTime(time));
            }
            mobilePosition.setLongitude(Double.parseDouble(getText(rootElement, "Longitude")));
            mobilePosition.setLatitude(Double.parseDouble(getText(rootElement, "Latitude")));
            if (NumericUtil.isDouble(getText(rootElement, "Speed"))) {
                mobilePosition.setSpeed(Double.parseDouble(getText(rootElement, "Speed")));
            } else {
                mobilePosition.setSpeed(0.0);
            }
            if (NumericUtil.isDouble(getText(rootElement, "Direction"))) {
                mobilePosition.setDirection(Double.parseDouble(getText(rootElement, "Direction")));
            } else {
                mobilePosition.setDirection(0.0);
            }
            if (NumericUtil.isDouble(getText(rootElement, "Altitude"))) {
                mobilePosition.setAltitude(Double.parseDouble(getText(rootElement, "Altitude")));
            } else {
                mobilePosition.setAltitude(0.0);
            }
            mobilePosition.setReportSource("Mobile Position");

            // 更新device channel 的经纬度
            DeviceChannel deviceChannel = new DeviceChannel();
            deviceChannel.setDeviceId(device.getDeviceId());
            deviceChannel.setChannelId(mobilePosition.getChannelId());
            deviceChannel.setLongitude(mobilePosition.getLongitude());
            deviceChannel.setLatitude(mobilePosition.getLatitude());
            deviceChannel.setGpsTime(mobilePosition.getTime());

            deviceChannel = deviceChannelService.updateGps(deviceChannel, device);

            mobilePosition.setLongitudeWgs84(deviceChannel.getLongitudeWgs84());
            mobilePosition.setLatitudeWgs84(deviceChannel.getLatitudeWgs84());
            mobilePosition.setLongitudeGcj02(deviceChannel.getLongitudeGcj02());
            mobilePosition.setLatitudeGcj02(deviceChannel.getLatitudeGcj02());

            if (userSetting.getSavePositionHistory()) {
                storager.insertMobilePosition(mobilePosition);
            }

            storager.updateChannelPosition(deviceChannel);

            String key = DeferredResultHolder.CALLBACK_CMD_MOBILE_POSITION + device.getDeviceId();
            RequestMessage msg = new RequestMessage();
            msg.setKey(key);
            msg.setData(mobilePosition);
            resultHolder.invokeAllResult(msg);

            // 发送redis消息。 通知位置信息的变化
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("time", DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(mobilePosition.getTime()));
            jsonObject.put("serial", deviceChannel.getDeviceId());
            jsonObject.put("code", deviceChannel.getChannelId());
            jsonObject.put("longitude", mobilePosition.getLongitude());
            jsonObject.put("latitude", mobilePosition.getLatitude());
            jsonObject.put("altitude", mobilePosition.getAltitude());
            jsonObject.put("direction", mobilePosition.getDirection());
            jsonObject.put("speed", mobilePosition.getSpeed());
            redisCatchStorage.sendMobilePositionMsg(jsonObject);
            //回复 200 OK
            try {
                responseAck(request, Response.OK);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] 移动设备位置数据查询 200: {}", e.getMessage());
            }

        } catch (DocumentException e) {
            logger.error("未处理的异常 ", e);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element element) {

    }
}
