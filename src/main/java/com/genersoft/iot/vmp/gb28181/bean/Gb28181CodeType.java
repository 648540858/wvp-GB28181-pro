package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.common.enums.DeviceControlType;
import org.dom4j.Element;
import org.springframework.util.ObjectUtils;

public enum Gb28181CodeType {

    CIVIL_CODE_PROVINCE("CIVIL_CODE_PROVINCE","省级编号"),
    CIVIL_CODE_CITY("CIVIL_CODE_CITY","市级编号"),
    CIVIL_CODE_COUNTY("CIVIL_CODE_GRASS_ROOTS","区级编号"),
    CIVIL_CODE_GRASS_ROOTS("CIVIL_CODE_GRASS_ROOTS","基层接入单位编号"),
    BUSINESS_GROUP("BUSINESS_GROUP","业务分组"),
    VIRTUAL_ORGANIZATION("VIRTUAL_ORGANIZATION","虚拟组织")
    ;


    private final String val;

    private final String desc;

    Gb28181CodeType(String val, String desc) {
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
            if (!ObjectUtils.isEmpty(rootElement.element(item.getVal())) || !ObjectUtils.isEmpty(rootElement.elements(item.getVal()))) {
                return item;
            }
        }
        return null;
    }
}
