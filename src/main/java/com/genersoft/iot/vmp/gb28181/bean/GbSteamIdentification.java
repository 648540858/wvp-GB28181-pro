package com.genersoft.iot.vmp.gb28181.bean;

/**
 * 码流索引标识
 */
public enum GbSteamIdentification {
    /**
     * 主码流 stream:0
     * 子码流 stream:1s
     */
    streamMain("stream", new String[]{"0","1"}),
    /**
     * 国标28181-2022定义的方式
     * 主码流 streamnumber:0
     * 子码流 streamnumber:1
     */
    streamnumber("streamnumber", new String[]{"0","1"}),
    /**
     * 主码流 streamprofile:0
     * 子码流 streamprofile:1
     */
    streamprofile("streamprofile", new String[]{"0","1"}),
    /**
     * 适用的品牌： TP-LINK
     */
    streamMode("streamMode", new String[]{"main","sub"}),
    ;

    GbSteamIdentification(String value, String[] indexArray) {
        this.value = value;
        this.indexArray = indexArray;
    }

    private String value;
    private String[] indexArray;

    public String getValue() {
        return value;
    }

    public String[] getIndexArray() {
        return indexArray;
    }
}
