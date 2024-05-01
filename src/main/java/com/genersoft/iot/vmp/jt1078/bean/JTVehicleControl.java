package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;

import java.util.Objects;

/**
 * 车辆控制类型
 */
public class JTVehicleControl {

    private int length;

    private void setLength(Object value) {
        if (Objects.isNull(value)) {
            length--;
        }else {
            length ++;
        }
    }
    public int getLength() {
        return length;
    }

    @ConfigAttribute(id = 0X0001, type="Byte", description = "车门, 0：车门锁闭 1：车门开启")
    private Integer controlCarDoor;

    public Integer getControlCarDoor() {
        return controlCarDoor;
    }

    public void setControlCarDoor(Integer controlCarDoor) {
        this.controlCarDoor = controlCarDoor;
        setLength(controlCarDoor);
    }
}
