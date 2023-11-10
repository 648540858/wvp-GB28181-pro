package com.genersoft.iot.vmp.service.bean;

import org.jetbrains.annotations.NotNull;

public class IndustryCodeType implements Comparable<IndustryCodeType>{

    /**
     * 接入类型码
     */
    private String name;

    /**
     * 名称
     */
    private String code;

    /**
     * 备注
     */
    private String notes;

    public static IndustryCodeType getInstance(IndustryCodeTypeEnum typeEnum) {
        IndustryCodeType industryCodeType = new IndustryCodeType();
        industryCodeType.setName(typeEnum.getName());
        industryCodeType.setCode(typeEnum.getCode());
        industryCodeType.setNotes(typeEnum.getNotes());
        return industryCodeType;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public int compareTo(@NotNull IndustryCodeType industryCodeType) {
        return Integer.compare(Integer.parseInt(this.code), Integer.parseInt(industryCodeType.getCode()));
    }
}
