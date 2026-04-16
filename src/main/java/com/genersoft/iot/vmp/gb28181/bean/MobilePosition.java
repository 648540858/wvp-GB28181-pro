package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.Data;
import org.dom4j.Element;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * @description: 移动位置bean
 * @author: lawrencehj
 * @date: 2021年1月23日
 */

@Data
public class MobilePosition {
    /**
     * 设备Id
     */
    private String deviceId;

    /**
     * 通道Id
     */
    private Integer channelId;

    /**
     * 通道国标编号
     */
    private String channelDeviceId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 通知时间
     */
    private String time;

    /**
     * 经度
     */
    private double longitude;

    /**
     * 纬度
     */
    private double latitude;

    /**
     * 海拔高度
     */
    private double altitude;

    /**
     * 速度
     */
    private double speed;

    /**
     * 方向
     */
    private double direction;

    /**
     * 位置信息上报来源（Mobile Position、GPS Alarm）
     */
    private String reportSource;
    /**
     * 创建时间
     */
    private String createTime;

    public static List<MobilePosition> decode(String deviceName, String deviceId, Element rootElementAfterCharset) {

        List<MobilePosition> mobilePositions = new ArrayList<>();

        MobilePosition mobilePosition = new MobilePosition();
        mobilePosition.setCreateTime(DateUtil.getNow());
        if (!ObjectUtils.isEmpty(deviceName)) {
            mobilePosition.setDeviceName(deviceName);
        }
        mobilePosition.setDeviceId(deviceId);

        String channelId = getText(rootElementAfterCharset, "DeviceID");

        mobilePosition.setChannelDeviceId(channelId);
        String time = getText(rootElementAfterCharset, "Time");
        if (ObjectUtils.isEmpty(time)){
            mobilePosition.setTime(DateUtil.getNow());
        }else {
            mobilePosition.setTime(SipUtils.parseTime(time));
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
        mobilePosition.setReportSource("Mobile Position");

        mobilePositions.add(mobilePosition);

        return mobilePositions;
    }

    @Override
    public String toString() {
        return "MobilePosition{" +
                "deviceId='" + deviceId + '\'' +
                ", channelId=" + channelId +
                ", channelDeviceId='" + channelDeviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", time='" + time + '\'' +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", altitude=" + altitude +
                ", speed=" + speed +
                ", direction=" + direction +
                ", reportSource='" + reportSource + '\'' +
                ", createTime='" + createTime + '\'' +
                '}';
    }
}
