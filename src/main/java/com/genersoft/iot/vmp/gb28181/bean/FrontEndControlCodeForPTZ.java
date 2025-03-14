package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForPTZ implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.PTZ;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * 镜头变倍，0为缩小 1为放大
     */
    @Getter
    @Setter
    private Integer zoom;

    /**
     * 云台垂直方向控制 0 为上， 1为下
     */
    @Getter
    @Setter
    private Integer tilt;

    /**
     * 云台水平方向控制 0 为左， 1为右
     */
    @Getter
    @Setter
    private Integer pan;

    /**
     * 水平控制速度相对值
     */
    @Getter
    @Setter
    private Integer panSpeed;

    /**
     * 垂直控制速度相对值
     */
    @Getter
    @Setter
    private Integer tiltSpeed;

    /**
     * 变倍控制速度相对值
     */
    @Getter
    @Setter
    private Integer zoomSpeed;

    @Override
    public String encode() {
        return "";
    }
}
