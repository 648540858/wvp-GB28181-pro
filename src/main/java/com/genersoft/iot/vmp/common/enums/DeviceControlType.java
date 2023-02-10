package com.genersoft.iot.vmp.common.enums;

import org.dom4j.Element;
import org.springframework.util.ObjectUtils;


/**
 * @author gaofuwang
 * @date 2023/01/18/ 10:09:00
 * @since 1.0
 */
public enum DeviceControlType {

    /**
     * 云台控制
     * 上下左右，预置位，扫描，辅助功能，巡航
     */
    PTZ("PTZCmd","云台控制"),
    /**
     * 远程启动
     */
    TELE_BOOT("TeleBoot","远程启动"),
    /**
     * 录像控制
     */
    RECORD("RecordCmd","录像控制"),
    /**
     * 布防撤防
     */
    GUARD("GuardCmd","布防撤防"),
    /**
     * 告警控制
     */
    ALARM("AlarmCmd","告警控制"),
    /**
     * 强制关键帧
     */
    I_FRAME("IFameCmd","强制关键帧"),
    /**
     * 拉框放大
     */
    DRAG_ZOOM_IN("DragZoomIn","拉框放大"),
    /**
     * 拉框缩小
     */
    DRAG_ZOOM_OUT("DragZoomOut","拉框缩小"),
    /**
     * 看守位
     */
    HOME_POSITION("HomePosition","看守位");

    private final String val;

    private final String desc;

    DeviceControlType(String val, String desc) {
        this.val = val;
        this.desc = desc;
    }

    public String getVal() {
        return val;
    }

    public String getDesc() {
        return desc;
    }

    public static DeviceControlType typeOf(Element rootElement) {
        for (DeviceControlType item : DeviceControlType.values()) {
            if (!ObjectUtils.isEmpty(rootElement.element(item.val)) || !ObjectUtils.isEmpty(rootElement.elements(item.val))) {
                return item;
            }
        }
        return null;
    }
}
