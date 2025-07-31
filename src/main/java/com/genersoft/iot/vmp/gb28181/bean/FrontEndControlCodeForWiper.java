package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForWiper implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.AUXILIARY;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * 辅助开关控制指令： 1为开， 2为关
     */
    @Getter
    @Setter
    private Integer code;

    @Override
    public String encode() {
        return "";
    }
}
