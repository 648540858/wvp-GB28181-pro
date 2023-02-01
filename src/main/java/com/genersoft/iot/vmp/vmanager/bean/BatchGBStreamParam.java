package com.genersoft.iot.vmp.vmanager.bean;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * @author lin
 */
@Schema(description = "多个推流信息")
public class BatchGBStreamParam {
    @Schema(description = "推流信息列表")
    private List<GbStream> gbStreams;

    public List<GbStream> getGbStreams() {
        return gbStreams;
    }

    public void setGbStreams(List<GbStream> gbStreams) {
        this.gbStreams = gbStreams;
    }
}
