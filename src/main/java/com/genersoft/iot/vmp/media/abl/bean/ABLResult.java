package com.genersoft.iot.vmp.media.abl.bean;

import com.alibaba.fastjson2.JSONArray;
import lombok.Data;

import java.util.List;

@Data
public class ABLResult {
    private int code;
    private String memo;


    private String key;
    private Integer port;
    private JSONArray params;
    private List<ABLMedia> mediaList;

    private String app;
    private String stream;
    private String starttime;
    private String endtime;
    private ABLUrls url;
    private List<ABLRecordFile> recordFileList;

    public static ABLResult getFailForMediaServer() {
        ABLResult zlmResult = new ABLResult();
        zlmResult.setCode(-2);
        zlmResult.setMemo("流媒体调用失败");
        return zlmResult;
    }

    public static ABLResult getMediaServer(int code, String msg) {
        ABLResult zlmResult = new ABLResult();
        zlmResult.setCode(code);
        zlmResult.setMemo(msg);
        return zlmResult;
    }
    @Override
    public String toString() {
        return "ZLMResult{" +
                "code=" + code +
                ", memo='" + memo + '\'' +
                (key != null ? (", key=" + key) : "") +
                (port != null ? (", port=" + port) : "") +
                '}';
    }
}
