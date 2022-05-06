package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.BaiduPoint;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.Coordtransform;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
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
            // 默认来源坐标系为WGS-84处理
            Double[] gcj02Point = Coordtransform.WGS84ToGCJ02(mobilePosition.getLongitude(), mobilePosition.getLatitude());
            logger.info("GCJ02坐标：" + gcj02Point[0] + ", " + gcj02Point[1]);
            mobilePosition.setGeodeticSystem("GCJ-02");
            mobilePosition.setCnLng(gcj02Point[0] + "");
            mobilePosition.setCnLat(gcj02Point[1] + "");
            if (!userSetting.getSavePositionHistory()) {
                storager.clearMobilePositionsByDeviceId(device.getDeviceId());
            }
            storager.insertMobilePosition(mobilePosition);
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
