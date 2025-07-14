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
    private String model;

    /**
     * 终端手机号
     */
    @Schema(description = "终端手机号")
    private String phoneNumber;

    /**
     * 终端ID
     */
    @Schema(description = "终端ID")
    private String terminalId;

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


    @Schema(description = "注册时间")
    private String registerTime;


    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;

    @Schema(description = "状态")
    private boolean status;

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
                ", 鉴权码='" + authenticationCode + '\'' +
                ", status=" + status +
                '}';
    }
}
