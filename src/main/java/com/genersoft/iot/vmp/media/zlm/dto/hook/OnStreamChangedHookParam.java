package com.genersoft.iot.vmp.media.zlm.dto.hook;

import com.genersoft.iot.vmp.vmanager.bean.StreamContent;

import java.util.List;

/**
 * @author lin
 */
public class OnStreamChangedHookParam extends HookParam{

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
     * 推流鉴权Id
     */
    private String callId;

    /**
     * 观看总人数，包括hls/rtsp/rtmp/http-flv/ws-flv
     */
    private int totalReaderCount;

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
    private String severId;

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
        private int codec_id;

        /**
         * 编码类型名称 CodecAAC CodecH264
         */
        private String codec_id_name;

        /**
         * Video = 0, Audio = 1
         */
        private int codec_type;

        /**
         * 轨道是否准备就绪
         */
        private boolean ready;

        /**
         * 音频采样位数
         */
        private int sample_bit;

        /**
         * 音频采样率
         */
        private int sample_rate;

        /**
         * 视频fps
         */
        private float fps;

        /**
         * 视频高
         */
        private int height;

        /**
         * 视频宽
         */
        private int width;

        /**
         * 帧数
         */
        private int frames;

        /**
         * 关键帧数
         */
        private int key_frames;

        /**
         * GOP大小
         */
        private int gop_size;

        /**
         * GOP间隔时长(ms)
         */
        private int gop_interval_ms;

        /**
         * 丢帧率
         */
        private float loss;

        public int getChannels() {
            return channels;
        }

        public void setChannels(int channels) {
            this.channels = channels;
        }

        public int getCodec_id() {
            return codec_id;
        }

        public void setCodec_id(int codec_id) {
            this.codec_id = codec_id;
        }

        public String getCodec_id_name() {
            return codec_id_name;
        }

        public void setCodec_id_name(String codec_id_name) {
            this.codec_id_name = codec_id_name;
        }

        public int getCodec_type() {
            return codec_type;
        }

        public void setCodec_type(int codec_type) {
            this.codec_type = codec_type;
        }

        public boolean isReady() {
            return ready;
        }

        public void setReady(boolean ready) {
            this.ready = ready;
        }

        public int getSample_bit() {
            return sample_bit;
        }

        public void setSample_bit(int sample_bit) {
            this.sample_bit = sample_bit;
        }

        public int getSample_rate() {
            return sample_rate;
        }

        public void setSample_rate(int sample_rate) {
            this.sample_rate = sample_rate;
        }

        public float getFps() {
            return fps;
        }

        public void setFps(float fps) {
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

        public int getFrames() {
            return frames;
        }

        public void setFrames(int frames) {
            this.frames = frames;
        }

        public int getKey_frames() {
            return key_frames;
        }

        public void setKey_frames(int key_frames) {
            this.key_frames = key_frames;
        }

        public int getGop_size() {
            return gop_size;
        }

        public void setGop_size(int gop_size) {
            this.gop_size = gop_size;
        }

        public int getGop_interval_ms() {
            return gop_interval_ms;
        }

        public void setGop_interval_ms(int gop_interval_ms) {
            this.gop_interval_ms = gop_interval_ms;
        }

        public float getLoss() {
            return loss;
        }

        public void setLoss(float loss) {
            this.loss = loss;
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

    private StreamContent streamInfo;

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

    public int getTotalReaderCount() {
        return totalReaderCount;
    }

    public void setTotalReaderCount(int totalReaderCount) {
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

    public StreamContent getStreamInfo() {
        return streamInfo;
    }

    public void setStreamInfo(StreamContent streamInfo) {
        this.streamInfo = streamInfo;
    }

    public String getSeverId() {
        return severId;
    }

    public void setSeverId(String severId) {
        this.severId = severId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    @Override
    public String toString() {
        return "OnStreamChangedHookParam{" +
                "regist=" + regist +
                ", app='" + app + '\'' +
                ", stream='" + stream + '\'' +
                ", severId='" + severId + '\'' +
                '}';
    }
}
