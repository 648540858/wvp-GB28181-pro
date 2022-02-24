package com.genersoft.iot.vmp.media.zlm.dto;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class StreamPushItem extends GbStream implements Comparable<StreamPushItem>{

    /**
     * id
     */
    private Integer id;

    /**
     * 应用名
     */
    private String app;

    /**
     * 流id
     */
    private String stream;

    /**
     * 观看总人数，包括hls/rtsp/rtmp/http-flv/ws-flv
     */
    private String totalReaderCount;

    /**
     * 协议 包括hls/rtsp/rtmp/http-flv/ws-flv
     */
    private List<MediaSchema> schemas;

    /**
     * 产生源类型，
     * unknown = 0,
     * rtmp_push=1,
     * rtsp_push=2,
     * rtp_push=3,
     * pull=4,
     * ffmpeg_pull=5,
     * mp4_vod=6,
     * device_chn=7
     */
    private int originType;

    /**
     * 客户端和服务器网络信息，可能为null类型
     */
    private MediaItem.OriginSock originSock;

    /**
     * 产生源类型的字符串描述
     */
    private String originTypeStr;

    /**
     * 产生源的url
     */
    private String originUrl;

    /**
     * 存活时间，单位秒
     */
    private Long aliveSecond;

    /**
     * 音视频轨道
     */
    private List<MediaItem.MediaTrack> tracks;

    /**
     * 音视频轨道
     */
    private String vhost;

    /**
     * 使用的流媒体ID
     */
    private String mediaServerId;

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }


    @Override
    public int compareTo(@NotNull StreamPushItem streamPushItem) {
        return Long.valueOf(super.createStamp - streamPushItem.getCreateStamp().intValue()).intValue();
    }

    public static class MediaSchema {
        private String schema;
        private Long bytesSpeed;

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public Long getBytesSpeed() {
            return bytesSpeed;
        }

        public void setBytesSpeed(Long bytesSpeed) {
            this.bytesSpeed = bytesSpeed;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getTotalReaderCount() {
        return totalReaderCount;
    }

    public void setTotalReaderCount(String totalReaderCount) {
        this.totalReaderCount = totalReaderCount;
    }

    public List<MediaSchema> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<MediaSchema> schemas) {
        this.schemas = schemas;
    }

    public int getOriginType() {
        return originType;
    }

    public void setOriginType(int originType) {
        this.originType = originType;
    }

    public MediaItem.OriginSock getOriginSock() {
        return originSock;
    }

    public void setOriginSock(MediaItem.OriginSock originSock) {
        this.originSock = originSock;
    }


    public String getOriginTypeStr() {
        return originTypeStr;
    }

    public void setOriginTypeStr(String originTypeStr) {
        this.originTypeStr = originTypeStr;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public Long getAliveSecond() {
        return aliveSecond;
    }

    public void setAliveSecond(Long aliveSecond) {
        this.aliveSecond = aliveSecond;
    }

    public List<MediaItem.MediaTrack> getTracks() {
        return tracks;
    }

    public void setTracks(List<MediaItem.MediaTrack> tracks) {
        this.tracks = tracks;
    }


    @Override
    public String getMediaServerId() {
        return mediaServerId;
    }

    @Override
    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }
}

