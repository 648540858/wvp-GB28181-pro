package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JT 设备
 */
@Schema(description = "jt808设备")
public class JTDevice {

    private int id;

    /**
     * 省域ID
     */
    @Schema(description = "省域ID")
    private String provinceId;

    /**
     * 省域文字描述
     */
    @Schema(description = "省域文字描述")
    private String provinceText;

    /**
     * 市县域ID
     */
    @Schema(description = "市县域ID")
    private String cityId;

    /**
     * 市县域文字描述
     */
    @Schema(description = "市县域文字描述")
    private String cityText;

    /**
     * 制造商ID
     */
    @Schema(description = "制造商ID")
    private String makerId;

    /**
     * 终端型号
     */
    @Schema(description = "终端型号")
    private String deviceModel;

    /**
     * 终端手机号
     */
    @Schema(description = "终端手机号")
    private Integer phoneNumber;

    /**
     * 设备ID
     */
    @Schema(description = "设备ID")
    private String deviceId;

    /**
     * 车牌颜色
     */
    @Schema(description = "车牌颜色")
    private int plateColor;

    /**
     * 车牌
     */
    @Schema(description = "车牌")
    private String plateNo;

    /**
     * 鉴权码
     */
    @Schema(description = "鉴权码")
    private String authenticationCode;

    /**
     * 经度
     */
    @Schema(description = "经度")
    private Double longitude;

    /**
     * 纬度
     */
    @Schema(description = "纬度")
    private Double latitude;


    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;

    @Schema(description = "状态")
    private boolean status;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(String provinceId) {
        this.provinceId = provinceId;
    }

    public String getProvinceText() {
        return provinceText;
    }

    public void setProvinceText(String provinceText) {
        this.provinceText = provinceText;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getCityText() {
        return cityText;
    }

    public void setCityText(String cityText) {
        this.cityText = cityText;
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

    public Integer getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(Integer phoneNumber) {
        this.phoneNumber = phoneNumber;
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

    public String getAuthenticationCode() {
        return authenticationCode;
    }

    public void setAuthenticationCode(String authenticationCode) {
        this.authenticationCode = authenticationCode;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "JTDevice{" +
                ", 省域ID='" + provinceId + '\'' +
                ", 省域文字描述='" + provinceText + '\'' +
                ", 市县域ID='" + cityId + '\'' +
                ", 市县域文字描述='" + cityText + '\'' +
                ", 制造商ID='" + makerId + '\'' +
                ", 终端型号='" + deviceModel + '\'' +
                ", 终端手机号='" + phoneNumber + '\'' +
                ", 设备ID='" + deviceId + '\'' +
                ", 车牌颜色=" + plateColor +
                ", 车牌='" + plateNo + '\'' +
                ", 鉴权码='" + authenticationCode + '\'' +
                ", status=" + status +
                '}';
    }
}
