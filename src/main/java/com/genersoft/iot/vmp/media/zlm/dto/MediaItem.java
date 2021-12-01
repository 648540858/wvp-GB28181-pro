package com.genersoft.iot.vmp.media.zlm.dto;

import java.util.List;

public class MediaItem {

    /**
     * 注册/注销
     */
    private boolean regist;

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
    private String schema;


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
    private OriginSock originSock;

    /**
     * 产生源类型的字符串描述
     */
    private String originTypeStr;

    /**
     * 产生源的url
     */
    private String originUrl;

    /**
     * 服务器id
     */
    private String mediaServerId;

    /**
     * GMT unix系统时间戳，单位秒
     */
    private Long createStamp;

    /**
     * 存活时间，单位秒
     */
    private Long aliveSecond;

    /**
     * 数据产生速度，单位byte/s
     */
    private Long bytesSpeed;

    /**
     * 音视频轨道
     */
    private List<MediaTrack> tracks;

    /**
     * 音视频轨道
     */
    private String vhost;

    public boolean isRegist() {
        return regist;
    }

    public void setRegist(boolean regist) {
        this.regist = regist;
    }

    /**
     * 是否是docker部署， docker部署不会自动更新zlm使用的端口，需要自己手动修改
     */
    private boolean docker;

    public static class MediaTrack {
        /**
         * 音频通道数
         */
        private int channels;

        /**
         *  H264 = 0, H265 = 1, AAC = 2, G711A = 3, G711U = 4
         */
        private int codecId;

        /**
         * 编码类型名称 CodecAAC CodecH264
         */
        private String codecIdName;

        /**
         * Video = 0, Audio = 1
         */
        private int codecType;

        /**
         * 轨道是否准备就绪
         */
        private boolean ready;

        /**
         * 音频采样位数
         */
        private int sampleBit;

        /**
         * 音频采样率
         */
        private int sampleRate;

        /**
         * 视频fps
         */
        private int fps;

        /**
         * 视频高
         */
        private int height;

        /**
         * 视频宽
         */
        private int width;

        public int getChannels() {
            return channels;
        }

        public void setChannels(int channels) {
            this.channels = channels;
        }

        public int getCodecId() {
            return codecId;
        }

        public void setCodecId(int codecId) {
            this.codecId = codecId;
        }

        public String getCodecIdName() {
            return codecIdName;
        }

        public void setCodecIdName(String codecIdName) {
            this.codecIdName = codecIdName;
        }

        public int getCodecType() {
            return codecType;
        }

        public void setCodecType(int codecType) {
            this.codecType = codecType;
        }

        public boolean isReady() {
            return ready;
        }

        public void setReady(boolean ready) {
            this.ready = ready;
        }

        public int getSampleBit() {
            return sampleBit;
        }

        public void setSampleBit(int sampleBit) {
            this.sampleBit = sampleBit;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public int getFps() {
            return fps;
        }

        public void setFps(int fps) {
            this.fps = fps;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }
    }

    public static class OriginSock{
        private String identifier;
        private String local_ip;
        private int local_port;
        private String peer_ip;
        private int peer_port;

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public String getLocal_ip() {
            return local_ip;
        }

        public void setLocal_ip(String local_ip) {
            this.local_ip = local_ip;
        }

        public int getLocal_port() {
            return local_port;
        }

        public void setLocal_port(int local_port) {
            this.local_port = local_port;
        }

        public String getPeer_ip() {
            return peer_ip;
        }

        public void setPeer_ip(String peer_ip) {
            this.peer_ip = peer_ip;
        }

        public int getPeer_port() {
            return peer_port;
        }

        public void setPeer_port(int peer_port) {
            this.peer_port = peer_port;
        }
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


    public int getOriginType() {
        return originType;
    }

    public void setOriginType(int originType) {
        this.originType = originType;
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

    public Long getCreateStamp() {
        return createStamp;
    }

    public void setCreateStamp(Long createStamp) {
        this.createStamp = createStamp;
    }

    public Long getAliveSecond() {
        return aliveSecond;
    }

    public void setAliveSecond(Long aliveSecond) {
        this.aliveSecond = aliveSecond;
    }

    public List<MediaTrack> getTracks() {
        return tracks;
    }

    public void setTracks(List<MediaTrack> tracks) {
        this.tracks = tracks;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void setOriginSock(OriginSock originSock) {
        this.originSock = originSock;
    }

    public Long getBytesSpeed() {
        return bytesSpeed;
    }

    public void setBytesSpeed(Long bytesSpeed) {
        this.bytesSpeed = bytesSpeed;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    public OriginSock getOriginSock() {
        return originSock;
    }

    public boolean isDocker() {
        return docker;
    }

    public void setDocker(boolean docker) {
        this.docker = docker;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }
}
