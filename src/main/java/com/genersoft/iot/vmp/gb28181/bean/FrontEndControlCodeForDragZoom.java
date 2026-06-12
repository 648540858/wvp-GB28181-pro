package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForDragZoom implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.DRAG_ZOOM;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * 辅助开关控制指令： 1为zoomIn 拉框放大， 2为zoomOut 拉框缩小
     */
    @Getter
    @Setter
    private Integer code;

    /**
     * 播放窗口长度像素值(必选)
     */
    @Getter
    @Setter
    protected Integer length;

    /**
     * 播放窗口长度像素值(必选)
     */
    @Getter
    @Setter
    protected Integer width;

    /**
     * 拉框中心的横轴坐标像素值(必选)
     */
    @Getter
    @Setter
    protected Integer midPointX;

    /**
     * 拉框中心的纵轴坐标像素值(必选)
     */
    @Getter
    @Setter
    protected Integer midPointY;

    /**
     * 拉框长度像素值(必选)
     */
    @Getter
    @Setter
    protected Integer lengthX;

    /**
     * 拉框宽度像素值(必选)
     */
    @Getter
    @Setter
    protected Integer lengthY;


    @Override
    public String encode() {
        return "";
    }
}
