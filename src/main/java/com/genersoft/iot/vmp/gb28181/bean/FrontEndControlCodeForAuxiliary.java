package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForAuxiliary implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.AUXILIARY;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * 辅助开关控制指令： 1为开， 2为关， 3为设置自动扫描右边界， 4为设置自动扫描速度
     */
    @Getter
    @Setter
    private Integer code;

    /**
     * 辅助开关编号
     */
    @Getter
    @Setter
    private Integer auxiliaryId;

    @Override
    public String encode() {
        return "";
    }
}
