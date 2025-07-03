package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForPreset implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.PRESET;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * 预置位指令： 1为设置预置位， 2为调用预置位， 3为删除预置位
     */
    @Getter
    @Setter
    private Integer code;

    /**
     * 预置位编号
     */
    @Getter
    @Setter
    private Integer presetId;


    @Override
    public String encode() {
        return "";
    }
}
