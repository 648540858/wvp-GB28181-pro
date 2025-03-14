package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForFI implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.FI;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * 光圈，0为缩小 1为放大
     */
    @Getter
    @Setter
    private Integer iris;

    /**
     * 聚焦 0 近， 1远
     */
    @Getter
    @Setter
    private Integer focus;

    /**
     * 聚焦速度
     */
    @Getter
    @Setter
    private Integer focusSpeed;

    /**
     * 光圈速度
     */
    @Getter
    @Setter
    private Integer irisSpeed;

    @Override
    public String encode() {
        return "";
    }
}
