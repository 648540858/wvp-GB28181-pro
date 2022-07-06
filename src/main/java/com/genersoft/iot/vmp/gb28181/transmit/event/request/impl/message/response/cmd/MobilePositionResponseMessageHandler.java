package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.Coordtransform;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.GpsUtil;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

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

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {

        try {
            rootElement = getRootElement(evt, device.getCharset());

            MobilePosition mobilePosition = new MobilePosition();
            mobilePosition.setCreateTime(DateUtil.getNow());
            if (!StringUtils.isEmpty(device.getName())) {
                mobilePosition.setDeviceName(device.getName());
            }
            mobilePosition.setDeviceId(device.getDeviceId());
            mobilePosition.setChannelId(getText(rootElement, "DeviceID"));
            mobilePosition.setTime(getText(rootElement, "Time"));
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
            if ("WGS84".equals(device.getGeoCoordSys())) {
                mobilePosition.setLongitudeWgs84(mobilePosition.getLongitude());
                mobilePosition.setLatitudeWgs84(mobilePosition.getLatitude());
                Double[] position = Coordtransform.WGS84ToGCJ02(mobilePosition.getLongitude(), mobilePosition.getLatitude());
                mobilePosition.setLongitudeGcj02(position[0]);
                mobilePosition.setLatitudeGcj02(position[1]);
            }else if ("GCJ02".equals(device.getGeoCoordSys())) {
                mobilePosition.setLongitudeGcj02(mobilePosition.getLongitude());
                mobilePosition.setLatitudeGcj02(mobilePosition.getLatitude());
                Double[] position = Coordtransform.GCJ02ToWGS84(mobilePosition.getLongitude(), mobilePosition.getLatitude());
                mobilePosition.setLongitudeWgs84(position[0]);
                mobilePosition.setLatitudeWgs84(position[1]);
            }else {
                mobilePosition.setLongitudeGcj02(0.00);
                mobilePosition.setLatitudeGcj02(0.00);
                mobilePosition.setLongitudeWgs84(0.00);
                mobilePosition.setLatitudeWgs84(0.00);
            }
            if (userSetting.getSavePositionHistory()) {
                storager.insertMobilePosition(mobilePosition);
            }
            // 更新device channel 的经纬度
            DeviceChannel deviceChannel = new DeviceChannel();
            deviceChannel.setDeviceId(device.getDeviceId());
            deviceChannel.setChannelId(mobilePosition.getChannelId());
            deviceChannel.setLongitude(mobilePosition.getLongitude());
            deviceChannel.setLatitude(mobilePosition.getLatitude());
            deviceChannel.setLongitudeWgs84(mobilePosition.getLongitudeWgs84());
            deviceChannel.setLatitudeWgs84(mobilePosition.getLatitudeWgs84());
            deviceChannel.setLongitudeGcj02(mobilePosition.getLongitudeGcj02());
            deviceChannel.setLatitudeGcj02(mobilePosition.getLatitudeGcj02());
            deviceChannel.setGpsTime(mobilePosition.getTime());
            storager.updateChannelPosition(deviceChannel);
            //回复 200 OK
            responseAck(evt, Response.OK);
        } catch (DocumentException | SipException | InvalidArgumentException | ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element element) {

    }
}
