package com.genersoft.iot.vmp.vmanager.bean;

import com.genersoft.iot.vmp.media.zlm.dto.StreamPush;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * @author lin
 */
@Schema(description = "多个推流信息")
public class BatchGBStreamParam {
    @Schema(description = "推流信息列表")
    private List<StreamPush> streamPushes;
    @Schema(description = "推流信息列表")
    private List<Integer> streamPushIds;

    public List<StreamPush> getStreamPushes() {
        return streamPushes;
    }

    public void setStreamPushes(List<StreamPush> streamPushes) {
        this.streamPushes = streamPushes;
    }

    public List<Integer> getStreamPushIds() {
        return streamPushIds;
    }

    public void setStreamPushIds(List<Integer> streamPushIds) {
        this.streamPushIds = streamPushIds;
    }
}
