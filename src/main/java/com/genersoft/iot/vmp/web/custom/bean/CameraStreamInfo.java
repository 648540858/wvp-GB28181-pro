package com.genersoft.iot.vmp.web.custom.bean;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CameraStreamInfo {


    private CommonGBChannel channel;


    private StreamInfo streamInfo;

    public CameraStreamInfo(CommonGBChannel channel, StreamInfo streamInfo) {
        this.channel = channel;
        this.streamInfo = streamInfo;
    }
}
