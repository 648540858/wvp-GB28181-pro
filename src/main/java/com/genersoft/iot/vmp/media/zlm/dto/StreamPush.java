package com.genersoft.iot.vmp.media.zlm.dto;

import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;


@Schema(description = "推流信息")
public class StreamPush implements Comparable<StreamPush>{

    /**
     * id
     */
    @Schema(description = "id")
    private Integer id;

    @Schema(description = "名称")
    private String name;

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
     * 存活时间，单位秒
     */
    @Schema(description = "存活时间，单位秒")
    private Long aliveSecond;

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

    /**
     * 国标通用信息ID
     */
    @Schema(description = "国标通用信息ID")
    private int commonGbChannelId;

    @Schema(description = "国标ID")
    private String gbId;

    @Schema(description = "经度")
    private double longitude;

    @Schema(description = "纬度")
    private double latitude;

    @Schema(description = "状态")
    private boolean status;

    @Schema(description = "分组国标编号")
    private String groupDeviceId;

    public static StreamPush getInstance(StreamPushExcelDto streamPushExcelDto) {
        StreamPush streamPush = new StreamPush();
        streamPush.setApp(streamPushExcelDto.getApp());
        streamPush.setStream(streamPushExcelDto.getStream());
        streamPush.setStatus(streamPushExcelDto.isStatus());
        streamPush.setCreateTime(DateUtil.getNow());
        streamPush.setUpdateTime(DateUtil.getNow());
        streamPush.setGbId(streamPushExcelDto.getGbId());
        streamPush.setName(streamPushExcelDto.getName());
        streamPush.setGroupDeviceId(streamPushExcelDto.getCatalogId());
        return streamPush;
    }


    @Override
    public int compareTo(@NotNull StreamPush streamPushItem) {
        return Long.valueOf(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(this.createTime)
                - DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(streamPushItem.getCreateTime())).intValue();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Long getAliveSecond() {
        return aliveSecond;
    }

    public void setAliveSecond(Long aliveSecond) {
        this.aliveSecond = aliveSecond;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

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

    public String getCreateTime() {
        return createTime;
    }

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

    public int getCommonGbChannelId() {
        return commonGbChannelId;
    }

    public void setCommonGbChannelId(int commonGbChannelId) {
        this.commonGbChannelId = commonGbChannelId;
    }

    public String getGbId() {
        return gbId;
    }

    public void setGbId(String gbId) {
        this.gbId = gbId;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getGroupDeviceId() {
        return groupDeviceId;
    }

    public void setGroupDeviceId(String groupDeviceId) {
        this.groupDeviceId = groupDeviceId;
    }
}

