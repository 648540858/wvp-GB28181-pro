package com.genersoft.iot.vmp.media.zlm.dto;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Schema(description = "推流信息")
public class StreamPushItem extends GbStream implements Comparable<StreamPushItem>{

    /**
     * id
     */
    @Schema(description = "id")
    private Integer id;

    /**
     * 应用名
     */
    @Schema(description = "应用名")
    private String app;

    /**
     * 流id
     */
    @Schema(description = "流id")
    private String stream;

    /**
     * 观看总人数，包括hls/rtsp/rtmp/http-flv/ws-flv
     */
    @Schema(description = "观看总人数")
    private String totalReaderCount;

    /**
     * 协议 包括hls/rtsp/rtmp/http-flv/ws-flv
     */
    @Schema(description = "协议 包括hls/rtsp/rtmp/http-flv/ws-flv")
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
    @Schema(description = "产生源类型")
    private int originType;

    /**
     * 客户端和服务器网络信息，可能为null类型
     */
    @Schema(description = "客户端和服务器网络信息，可能为null类型")
    private OnStreamChangedHookParam.OriginSock originSock;

    /**
     * 产生源类型的字符串描述
     */
    @Schema(description = "产生源类型的字符串描述")
    private String originTypeStr;

    /**
     * 产生源的url
     */
    @Schema(description = "产生源的url")
    private String originUrl;

    /**
     * 存活时间，单位秒
     */
    @Schema(description = "存活时间，单位秒")
    private Long aliveSecond;

    /**
     * 音视频轨道
     */
    @Schema(description = "音视频轨道")
    private List<OnStreamChangedHookParam.MediaTrack> tracks;

    /**
     * 音视频轨道
     */
    @Schema(description = "音视频轨道")
    private String vhost;

    /**
     * 使用的流媒体ID
     */
    @Schema(description = "使用的流媒体ID")
    private String mediaServerId;

    /**
     * 使用的服务ID
     */
    @Schema(description = "使用的服务ID")
    private String serverId;

    /**
     * 推流时间
     */
    @Schema(description = "推流时间")
    private String pushTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private String updateTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private String createTime;

    /**
     * 是否正在推流
     */
    @Schema(description = "是否正在推流")
    private boolean pushIng;

    /**
     * 是否自己平台的推流
     */
    @Schema(description = "是否自己平台的推流")
    private boolean self;



    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }


    @Override
    public int compareTo(@NotNull StreamPushItem streamPushItem) {
        return Long.valueOf(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(super.createTime)
                - DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(streamPushItem.getCreateTime())).intValue();
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

    @Override
    public String getApp() {
        return app;
    }

    @Override
    public void setApp(String app) {
        this.app = app;
    }

    @Override
    public String getStream() {
        return stream;
    }

    @Override
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

    public OnStreamChangedHookParam.OriginSock getOriginSock() {
        return originSock;
    }

    public void setOriginSock(OnStreamChangedHookParam.OriginSock originSock) {
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

    public List<OnStreamChangedHookParam.MediaTrack> getTracks() {
        return tracks;
    }

    public void setTracks(List<OnStreamChangedHookParam.MediaTrack> tracks) {
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

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }


    public String getPushTime() {
        return pushTime;
    }

    public void setPushTime(String pushTime) {
        this.pushTime = pushTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public boolean isPushIng() {
        return pushIng;
    }

    public void setPushIng(boolean pushIng) {
        this.pushIng = pushIng;
    }

    public boolean isSelf() {
        return self;
    }

    public void setSelf(boolean self) {
        this.self = self;
    }

//    @Override
//    public Integer getGbStreamId() {
//        return super.getGbStreamId();
//    }
//
//    @Override
//    public void setGbStreamId(Integer gbStreamId) {
//        super.setGbStreamId(gbStreamId);
//    }
//
//
//    public String getGbId() {
//        return super.getGbId();
//    }
//
//    public void setGbId(String gbId) {
//       super.setGbId(gbId);
//    }
//
//    public String getName() {
//        return super.getName();
//    }
//
//    public void setName(String name) {
//        super.setName(name);
//    }
//
//    public double getLongitude() {
//        return super.getLongitude();
//    }
//
//    public void setLongitude(double longitude) {
//        super.setLongitude(longitude);
//    }
//
//    public double getLatitude() {
//        return super.getLatitude();
//    }
//
//    public void setLatitude(double latitude) {
//        super.setLatitude(latitude);
//    }
//
//    public String getStreamType() {
//        return super.getStreamType();
//    }
//
//    public void setStreamType(String streamType) {
//        super.setStreamType(streamType);
//    }
//
//    public boolean isStatus() {
//        return super.isStatus();
//    }
//
//    public void setStatus(boolean status) {
//        super.setStatus(status);
//    }

}

