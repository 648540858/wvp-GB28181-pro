package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * 国标设备移动位置
 */

@Slf4j
@Getter
@Setter
public class DeviceMobilePosition extends MobilePosition{

    /**
     * 通道数据库自增Id
     */
    private String channelDeviceId;


    private Device device;


    public static List<DeviceMobilePosition> decode(Device device, Element rootElementAfterCharset) {

        List<DeviceMobilePosition> mobilePositions = new ArrayList<>();

        DeviceMobilePosition mobilePosition = new DeviceMobilePosition();
        mobilePosition.setCreateTime(DateUtil.getNow());
        mobilePosition.setDevice(device);

        String channelId = getText(rootElementAfterCharset, "DeviceID");

        mobilePosition.setChannelDeviceId(channelId);
        String time = getText(rootElementAfterCharset, "Time");
        if (ObjectUtils.isEmpty(time)){
            mobilePosition.setTimestamp(System.currentTimeMillis());
        }else {
            Long timestamp = SipUtils.parseTimeForTimestamp(time);
            if(timestamp == null) {
                log.warn("解析移动位置时间失败：{}， 使用当前时间", time);
                mobilePosition.setTimestamp(System.currentTimeMillis());
            }else {
                mobilePosition.setTimestamp(timestamp);
            }
        }
        mobilePosition.setLongitude(Double.parseDouble(getText(rootElementAfterCharset, "Longitude")));
        mobilePosition.setLatitude(Double.parseDouble(getText(rootElementAfterCharset, "Latitude")));
        if (NumericUtil.isDouble(getText(rootElementAfterCharset, "Speed"))) {
            mobilePosition.setSpeed(Double.parseDouble(getText(rootElementAfterCharset, "Speed")));
        } else {
            mobilePosition.setSpeed(0.0);
        }
        if (NumericUtil.isDouble(getText(rootElementAfterCharset, "Direction"))) {
            mobilePosition.setDirection(Double.parseDouble(getText(rootElementAfterCharset, "Direction")));
        } else {
            mobilePosition.setDirection(0.0);
        }
        if (NumericUtil.isDouble(getText(rootElementAfterCharset, "Altitude"))) {
            mobilePosition.setAltitude(Double.parseDouble(getText(rootElementAfterCharset, "Altitude")));
        } else {
            mobilePosition.setAltitude(0.0);
        }

        mobilePositions.add(mobilePosition);

        return mobilePositions;
    }

    @Override
    public String toString() {
        return "DeviceMobilePosition{" +
                "channelDeviceId='" + channelDeviceId + '\'' +
                ", deviceId='" + device.getDeviceId() + '\'' +
                "} " + super.toString();
    }
}
