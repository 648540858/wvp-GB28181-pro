package com.genersoft.iot.vmp.jt1078.event;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import org.springframework.context.ApplicationEvent;

/**
 * 注册事件
 */

public class RegisterEvent extends ApplicationEvent {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public RegisterEvent(Object source) {
        super(source);
    }


    private int provinceId;

    private int cityId;

    private String makerId;

    private String deviceModel;

    private String deviceId;

    private int plateColor;

    private String plateNo;


    public int getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(int provinceId) {
        this.provinceId = provinceId;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    public String getMakerId() {
        return makerId;
    }

    public void setMakerId(String makerId) {
        this.makerId = makerId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getPlateColor() {
        return plateColor;
    }

    public void setPlateColor(int plateColor) {
        this.plateColor = plateColor;
    }

    public String getPlateNo() {
        return plateNo;
    }

    public void setPlateNo(String plateNo) {
        this.plateNo = plateNo;
    }
}
