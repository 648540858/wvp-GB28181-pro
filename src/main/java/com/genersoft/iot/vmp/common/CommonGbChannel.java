package com.genersoft.iot.vmp.common;

import io.swagger.v3.oas.annotations.media.Schema;

public class CommonGbChannel {

    /**
     * 国标字段：归属
     */
    @Schema(description = "归属")
    private String owner;

    /**
     * 国标字段：行政区划
     */
    @Schema(description = "行政区划")
    private String civilCode;

    /**
     * 国标字段：安装地址
     */
    @Schema(description = "安装地址")
    private String address;

    /**
     * 国标字段：经度
     */
    @Schema(description = "经度")
    private Double longitude;

    /**
     * 国标字段：纬度
     */
    @Schema(description = "纬度")
    private Double latitude;

    /**
     * 国标字段：摄像机类型:
     * 1-球机;
     * 2-半球;
     * 3-固定枪机;
     * 4-遥控枪机
     */
    @Schema(description = "摄像机类型")
    private Integer ptzType;

    /**
     * 国标字段：摄像机位置类型扩展。
     * 1-省际检查站、
     * 2-党政机关、
     * 3-车站码头、
     * 4-中心广场、
     * 5-体育场馆、
     * 6-商业中心、
     * 7-宗教场所、
     * 8-校园周边、
     * 9-治安复杂区域、
     * 10-交通干线
     */
    @Schema(description = "摄像机位置类型扩展")
    private Integer positionType;

    /**
     * 国标字段：安装位置室外、室内属性
     * 1-室外、
     * 2-室内
     */
    @Schema(description = "安装位置室外、室内属性")
    private Integer roomType;

    /**
     * 国标字段：用途
     * 1-治安、
     * 2-交通、
     * 3-重点、
     */
    @Schema(description = "用途")
    private Integer useType;

    /**
     * 国标字段：补光属性
     * 1-无补光、
     * 2-红外补光、
     * 3-白光补光
     */
    @Schema(description = "补光属性")
    private Integer supplyLightType;

    /**
     * 摄像机监视方位属性。
     * 1-东、
     * 2-西、
     * 3-南、
     * 4-北、
     * 5-东南、
     * 6-东北、
     * 7-西南、
     * 8-西北
     *
     */
    @Schema(description = "方位")
    private Integer directionType;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCivilCode() {
        return civilCode;
    }

    public void setCivilCode(String civilCode) {
        this.civilCode = civilCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public Integer getPtzType() {
        return ptzType;
    }

    public void setPtzType(Integer ptzType) {
        this.ptzType = ptzType;
    }

    public Integer getPositionType() {
        return positionType;
    }

    public void setPositionType(Integer positionType) {
        this.positionType = positionType;
    }

    public Integer getRoomType() {
        return roomType;
    }

    public void setRoomType(Integer roomType) {
        this.roomType = roomType;
    }

    public Integer getUseType() {
        return useType;
    }

    public void setUseType(Integer useType) {
        this.useType = useType;
    }

    public Integer getSupplyLightType() {
        return supplyLightType;
    }

    public void setSupplyLightType(Integer supplyLightType) {
        this.supplyLightType = supplyLightType;
    }

    public Integer getDirectionType() {
        return directionType;
    }

    public void setDirectionType(Integer directionType) {
        this.directionType = directionType;
    }
}
