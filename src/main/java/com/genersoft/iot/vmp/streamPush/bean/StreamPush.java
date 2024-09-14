package com.genersoft.iot.vmp.streamPush.bean;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.ObjectUtils;


@Data
@Schema(description = "推流信息")
@EqualsAndHashCode(callSuper = true)
public class StreamPush extends CommonGBChannel implements Comparable<StreamPush>{

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
    private boolean pushing;

    /**
     * 拉起离线推流
     */
    @Schema(description = "拉起离线推流")
    private boolean startOfflinePush;

    private String uniqueKey;

    @Override
    public int compareTo(@NotNull StreamPush streamPushItem) {
        return Long.valueOf(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(this.createTime)
                - DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(streamPushItem.getCreateTime())).intValue();
    }

    public StreamPush getInstance(StreamInfo streamInfo) {
        StreamPush streamPush = new StreamPush();
        streamPush.setApp(streamInfo.getApp());
        if (streamInfo.getMediaServer() != null) {
            streamPush.setMediaServerId(streamInfo.getMediaServer().getId());
        }

        streamPush.setStream(streamInfo.getStream());
        streamPush.setCreateTime(DateUtil.getNow());
        streamPush.setServerId(streamInfo.getServerId());
        return streamPush;

    }

    public static StreamPush getInstance(MediaArrivalEvent event, String serverId){
        StreamPush streamPushItem = new StreamPush();
        streamPushItem.setApp(event.getApp());
        streamPushItem.setMediaServerId(event.getMediaServer().getId());
        streamPushItem.setStream(event.getStream());
        streamPushItem.setCreateTime(DateUtil.getNow());
        streamPushItem.setServerId(serverId);
        return streamPushItem;
    }

    public CommonGBChannel buildCommonGBChannel() {
        if (ObjectUtils.isEmpty(this.getGbDeviceId())) {
            return null;
        }
        if (ObjectUtils.isEmpty(this.getGbName())) {
            this.setGbName( app+ "-" +stream);
        }
        this.setStreamPushId(this.getId());
        return this;
    }


}

