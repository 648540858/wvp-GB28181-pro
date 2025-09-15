package com.genersoft.iot.vmp.media.zlm.dto;

import lombok.Data;

@Data
public class ZLMResult<T> {
    private int code;
    private String msg;
    private T data;


    private Boolean online;
    private Boolean exist;
    private String peer_ip;
    private Integer peer_port;
    private String local_ip;
    private Integer local_port;
    private Integer changed;
    private Integer port;
    private Integer hit;

    public static ZLMResult<?> getFailForMediaServer() {
        ZLMResult<?> zlmResult = new ZLMResult<>();
        zlmResult.setCode(-2);
        zlmResult.setMsg("流媒体调用失败");
        return zlmResult;
    }

    public static ZLMResult<?> getMediaServer(int code, String msg) {
        return getMediaServer(code, msg, null);
    }

    public static ZLMResult<?> getMediaServer(int code, String msg, Object data) {
        ZLMResult<Object> zlmResult = new ZLMResult<>();
        zlmResult.setCode(code);
        zlmResult.setMsg(msg);
        zlmResult.setData(data);
        return zlmResult;
    }


}
