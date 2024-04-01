package com.genersoft.iot.vmp.media.event.hook;

import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.RecordInfo;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaEvent;
import com.genersoft.iot.vmp.media.event.media.MediaPublishEvent;
import com.genersoft.iot.vmp.media.event.media.MediaRecordMp4Event;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Hook返回的内容
 */
public class HookData {
    /**
     * 应用名
     */
    private String app;
    /**
     * 流ID
     */
    private String stream;
    /**
     * 流媒体节点
     */
    private MediaServer mediaServer;
    /**
     * 协议
     */
    private String schema;

    /**
     * 流信息
     */
    private MediaInfo mediaInfo;

    /**
     * 录像信息
     */
    private RecordInfo recordInfo;

    @Schema(description = "推流的额外参数")
    private String params;
    public static HookData getInstance(MediaEvent mediaEvent) {
        HookData hookData = new HookData();
        if (mediaEvent instanceof MediaPublishEvent) {
            MediaPublishEvent event = (MediaPublishEvent) mediaEvent;
            hookData.setApp(event.getApp());
            hookData.setStream(event.getStream());
            hookData.setSchema(event.getSchema());
            hookData.setMediaServer(event.getMediaServer());
            hookData.setParams(event.getParams());
        }else if (mediaEvent instanceof MediaArrivalEvent) {
            MediaArrivalEvent event = (MediaArrivalEvent) mediaEvent;
            hookData.setApp(event.getApp());
            hookData.setStream(event.getStream());
            hookData.setSchema(event.getSchema());
            hookData.setMediaServer(event.getMediaServer());
            hookData.setMediaInfo(event.getMediaInfo());
        }else if (mediaEvent instanceof MediaRecordMp4Event) {
            MediaRecordMp4Event event = (MediaRecordMp4Event) mediaEvent;
            hookData.setApp(event.getApp());
            hookData.setStream(event.getStream());
            hookData.setSchema(event.getSchema());
            hookData.setMediaServer(event.getMediaServer());
            hookData.setRecordInfo(event.getRecordInfo());
        }else {
            hookData.setApp(mediaEvent.getApp());
            hookData.setStream(mediaEvent.getStream());
            hookData.setSchema(mediaEvent.getSchema());
            hookData.setMediaServer(mediaEvent.getMediaServer());
        }
        return hookData;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public MediaServer getMediaServer() {
        return mediaServer;
    }

    public void setMediaServer(MediaServer mediaServer) {
        this.mediaServer = mediaServer;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public MediaInfo getMediaInfo() {
        return mediaInfo;
    }

    public void setMediaInfo(MediaInfo mediaInfo) {
        this.mediaInfo = mediaInfo;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public RecordInfo getRecordInfo() {
        return recordInfo;
    }

    public void setRecordInfo(RecordInfo recordInfo) {
        this.recordInfo = recordInfo;
    }
}
