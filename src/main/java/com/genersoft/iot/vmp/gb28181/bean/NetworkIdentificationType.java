package com.genersoft.iot.vmp.gb28181.bean;

import org.jetbrains.annotations.NotNull;

public class NetworkIdentificationType implements Comparable<NetworkIdentificationType>{

    /**
     * 接入类型码
     */
    private String name;

    /**
     * 名称
     */
    private String code;

    public static NetworkIdentificationType getInstance(NetworkIdentificationTypeEnum typeEnum) {
        NetworkIdentificationType industryCodeType = new NetworkIdentificationType();
        industryCodeType.setName(typeEnum.getName());
        industryCodeType.setCode(typeEnum.getCode());
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

    @Override
    public int compareTo(@NotNull NetworkIdentificationType networkIdentificationType) {
        return Integer.compare(Integer.parseInt(this.code), Integer.parseInt(networkIdentificationType.getCode()));
    }
}
