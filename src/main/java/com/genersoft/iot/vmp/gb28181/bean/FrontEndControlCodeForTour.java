package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForTour implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.TOUR;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * 巡航指令： 1为加入巡航点， 2为删除一个巡航点， 3为设置巡航速度， 4为设置巡航停留时间， 5为开始巡航
     */
    @Getter
    @Setter
    private Integer code;

    /**
     * 巡航点
     */
    @Getter
    @Setter
    private Integer tourId;

    /**
     * 巡航停留时间
     */
    @Getter
    @Setter
    private Integer tourTime;

    /**
     * 巡航速度
     */
    @Getter
    @Setter
    private Integer tourSpeed;

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
