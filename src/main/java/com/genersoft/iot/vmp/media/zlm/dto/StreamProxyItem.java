package com.genersoft.iot.vmp.media.zlm.dto;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author lin
 */
@Schema(description = "拉流代理的信息")
public class StreamProxyItem extends GbStream {

    @Schema(description = "类型")
    private String type;
    @Schema(description = "应用名")
    private String app;
    @Schema(description = "流ID")
    private String stream;
    @Schema(description = "流媒体服务ID")
    private String mediaServerId;
    @Schema(description = "拉流地址")
    private String url;
    @Schema(description = "拉流地址")
    private String src_url;
    @Schema(description = "目标地址")
    private String dst_url;
    @Schema(description = "超时时间")
    private int timeout_ms;
    @Schema(description = "ffmpeg模板KEY")
    private String ffmpeg_cmd_key;
    @Schema(description = "rtsp拉流时，拉流方式，0：tcp，1：udp，2：组播")
    private String rtp_type;
    @Schema(description = "是否启用")
    private boolean enable;
    @Schema(description = "是否启用HLS")
    private boolean enable_hls;
    @Schema(description = "是否启用MP4")
    private boolean enable_mp4;
    @Schema(description = "是否 无人观看时删除")
    private boolean enable_remove_none_reader;

    @Schema(description = "是否 无人观看时自动停用")
    private boolean enable_disable_none_reader;
    @Schema(description = "上级平台国标ID")
    private String platformGbId;
    @Schema(description = "创建时间")
    private String createTime;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    @Override
    public String getMediaServerId() {
        return mediaServerId;
    }

    @Override
    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSrc_url() {
        return src_url;
    }

    public void setSrc_url(String src_url) {
        this.src_url = src_url;
    }

    public String getDst_url() {
        return dst_url;
    }

    public void setDst_url(String dst_url) {
        this.dst_url = dst_url;
    }

    public int getTimeout_ms() {
        return timeout_ms;
    }

    public void setTimeout_ms(int timeout_ms) {
        this.timeout_ms = timeout_ms;
    }

    public String getFfmpeg_cmd_key() {
        return ffmpeg_cmd_key;
    }

    public void setFfmpeg_cmd_key(String ffmpeg_cmd_key) {
        this.ffmpeg_cmd_key = ffmpeg_cmd_key;
    }

    public String getRtp_type() {
        return rtp_type;
    }

    public void setRtp_type(String rtp_type) {
        this.rtp_type = rtp_type;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isEnable_hls() {
        return enable_hls;
    }

    public void setEnable_hls(boolean enable_hls) {
        this.enable_hls = enable_hls;
    }

    public boolean isEnable_mp4() {
        return enable_mp4;
    }

    public void setEnable_mp4(boolean enable_mp4) {
        this.enable_mp4 = enable_mp4;
    }


    public String getPlatformGbId() {
        return platformGbId;
    }

    public void setPlatformGbId(String platformGbId) {
        this.platformGbId = platformGbId;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public boolean isEnable_remove_none_reader() {
        return enable_remove_none_reader;
    }

    public void setEnable_remove_none_reader(boolean enable_remove_none_reader) {
        this.enable_remove_none_reader = enable_remove_none_reader;
    }

    public boolean isEnable_disable_none_reader() {
        return enable_disable_none_reader;
    }

    public void setEnable_disable_none_reader(boolean enable_disable_none_reader) {
        this.enable_disable_none_reader = enable_disable_none_reader;
    }
}
