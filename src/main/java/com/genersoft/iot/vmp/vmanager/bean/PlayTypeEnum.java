package com.genersoft.iot.vmp.vmanager.bean;

public enum PlayTypeEnum {

    PLAY("0", "直播"),
    PLAY_BACK("1", "回放");

    private String value;
    private String name;

    PlayTypeEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }
}
