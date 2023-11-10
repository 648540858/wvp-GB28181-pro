package com.genersoft.iot.vmp.service.bean;

import org.jetbrains.annotations.NotNull;

public class DeviceType implements Comparable<DeviceType>{

    /**
     * 编号
     */
    private String name;

    /**
     * 名称
     */
    private String code;

    /**
     * 归属名称
     */
    private String ownerName;
    public static DeviceType getInstance(DeviceTypeEnum typeEnum) {
        DeviceType deviceType = new DeviceType();
        deviceType.setName(typeEnum.getName());
        deviceType.setCode(typeEnum.getCode());
        deviceType.setOwnerName(typeEnum.getOwnerName());
        return deviceType;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    @Override
    public int compareTo(@NotNull DeviceType deviceType) {
        return Integer.compare(Integer.parseInt(this.code), Integer.parseInt(deviceType.getCode()));
    }
}
