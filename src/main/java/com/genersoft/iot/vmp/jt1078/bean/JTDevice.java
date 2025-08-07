package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * JT 设备
 */
@Data
@Schema(description = "jt808设备")
public class JTDevice {

    private int id;

    @Schema(description = "省域ID")
    private String provinceId;

    @Schema(description = "省域文字描述")
    private String provinceText;

    @Schema(description = "市县域ID")
    private String cityId;

    @Schema(description = "市县域文字描述")
    private String cityText;

    @Schema(description = "制造商ID")
    private String makerId;

    @Schema(description = "终端型号")
    private String model;

    @Schema(description = "终端手机号")
    private String phoneNumber;

    @Schema(description = "终端ID")
    private String terminalId;

    @Schema(description = "车牌颜色")
    private int plateColor;

    @Schema(description = "车牌")
    private String plateNo;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "纬度")
    private Double latitude;

    @Schema(description = "注册时间")
    private String registerTime;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;

    @Schema(description = "状态")
    private boolean status;

    @Schema(description = "设备使用的媒体id, 默认为null")
    private String mediaServerId;

    @Schema(description = "地理坐标系， 目前支持 WGS84,GCJ02")
    private String geoCoordSys;

    @Schema(description = "收流IP")
    private String sdpIp;

    @Override
    public String toString() {
        return "JTDevice{" +
                "  终端手机号='" + phoneNumber + '\'' +
                ", 省域ID='" + provinceId + '\'' +
                ", 省域文字描述='" + provinceText + '\'' +
                ", 市县域ID='" + cityId + '\'' +
                ", 市县域文字描述='" + cityText + '\'' +
                ", 制造商ID='" + makerId + '\'' +
                ", 终端型号='" + model + '\'' +
                ", 设备ID='" + terminalId + '\'' +
                ", 车牌颜色=" + plateColor +
                ", 车牌='" + plateNo + '\'' +
                ", 注册时间='" + registerTime + '\'' +
                ", status=" + status +
                '}';
    }
}
