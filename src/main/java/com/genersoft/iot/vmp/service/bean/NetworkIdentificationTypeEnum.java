package com.genersoft.iot.vmp.service.bean;

/**
 * 收录行业编码
 */
public enum NetworkIdentificationTypeEnum {
    PUBLIC_SECURITY_VIDEO_TRANSMISSION_NETWORK("0", "公安视频传输网"),
    PUBLIC_SECURITY_VIDEO_TRANSMISSION_NETWORK2("1", "公安视频传输网"),
    INDUSTRY_SPECIFIC_NETWORK("2", "行业专网"),
    POLITICAL_AND_LEGAL_INFORMATION_NETWORK("3", "政法信息网"),
    PUBLIC_SECURITY_MOBILE_INFORMATION_NETWORK("4", "公安移动信息网"),
    PUBLIC_SECURITY_INFORMATION_NETWORK("5", "公安信息网"),
    ELECTRONIC_GOVERNMENT_EXTRANET("6", "电子政务外网"),
    PUBLIC_NETWORKS_SUCH_AS_THE_INTERNET("7", "互联网等公共网络"),
    Dedicated_Line("8", "专线"),
    RESERVE("9", "预留"),
    ;

    /**
     * 接入类型码
     */
    private String name;

    /**
     * 名称
     */
    private String code;


    NetworkIdentificationTypeEnum(String code, String name) {
        this.name = name;
        this.code = code;
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

}
