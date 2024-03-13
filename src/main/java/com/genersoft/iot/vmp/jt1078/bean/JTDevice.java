package com.genersoft.iot.vmp.jt1078.bean;

/**
 * JT 设备
 */
public class JTDevice {

    /**
     * 省域ID
     */
    private int provinceId;

    /**
     * 市县域ID
     */
    private int cityId;

    /**
     * 制造商ID
     */
    private String makerId;

    /**
     * 终端型号
     */
    private String deviceModel;

    /**
     * 终端ID
     */
    private String deviceId;

    /**
     * 车牌颜色
     */
    private int plateColor;

    /**
     * 车牌
     */
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
