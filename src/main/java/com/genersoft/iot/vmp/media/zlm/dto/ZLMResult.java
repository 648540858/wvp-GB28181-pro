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

    public static <T> ZLMResult<T> getFailForMediaServer() {
        ZLMResult<T> zlmResult = new ZLMResult<>();
        zlmResult.setCode(-2);
        zlmResult.setMsg("流媒体调用失败");
        return zlmResult;
    }

    public static <T> ZLMResult<T> getMediaServer(int code, String msg) {
        return getMediaServer(code, msg, null);
    }

    public static <T> ZLMResult<T> getMediaServer(int code, String msg, T data) {
        ZLMResult<T> zlmResult = new ZLMResult<>();
        zlmResult.setCode(code);
        zlmResult.setMsg(msg);
        zlmResult.setData(data);
        return zlmResult;
    }

    @Override
    public String toString() {
        return "ZLMResult{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                (online != null ? (", online=" + online) : "") +
                (exist != null ? (", exist=" + exist) : "") +
                (peer_ip != null ? (", peer_ip=" + peer_ip) : "") +
                (peer_port != null ? (", peer_port=" + peer_port) : "") +
                (local_ip != null ? (", local_ip=" + local_ip) : "") +
                (local_port != null ? (", local_port=" + local_port) : "") +
                (changed != null ? (", changed=" + changed) : "") +
                (port != null ? (", port=" + port) : "") +
                (hit != null ? (", hit=" + hit) : "") +
                '}';
    }
}
