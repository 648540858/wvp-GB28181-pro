package com.genersoft.iot.vmp.common;

public class CivilCodePo {

    private String code;

    private String name;

    private String parentCode;

    public static CivilCodePo getInstance(String[] infoArray) {
        CivilCodePo civilCodePo = new CivilCodePo();
        civilCodePo.setCode(infoArray[0]);
        civilCodePo.setName(infoArray[1]);
        civilCodePo.setParentCode(infoArray[2]);
        return civilCodePo;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }
}
