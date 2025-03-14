package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForScan implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.SCAN;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * 预置位指令： 1为开始自动扫描， 2为设置自动扫描左边界， 3为设置自动扫描右边界， 4为设置自动扫描速度
     */
    @Getter
    @Setter
    private Integer code;

    /**
     * 自动扫描速度
     */
    @Getter
    @Setter
    private Integer scanSpeed;

    /**
     * 扫描组号
     */
    @Getter
    @Setter
    private Integer scanId;

    @Override
    public String encode() {
        return "";
    }
}
