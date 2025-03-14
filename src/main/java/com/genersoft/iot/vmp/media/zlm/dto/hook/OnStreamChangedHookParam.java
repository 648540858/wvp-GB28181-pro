package com.genersoft.iot.vmp.media.zlm.dto.hook;

import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * @author lin
 */
@EqualsAndHashCode(callSuper = true)
@Data
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

    /**
     * 额外的参数字符串
     */
    private String params;

    /**
     * 额外的参数
     */
    private Map<String, String> paramMap;

    /**
     * 是否是docker部署， docker部署不会自动更新zlm使用的端口，需要自己手动修改
     */
    private boolean docker;

    @Data
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
    }

    @Data
    public static class OriginSock{
        private String identifier;
        private String local_ip;
        private int local_port;
        private String peer_ip;
        private int peer_port;

    }

    private StreamContent streamInfo;

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
