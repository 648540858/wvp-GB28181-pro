package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import lombok.Data;

@Data
public class CommonChannelPlayInfo {

    private StreamInfo streamInfo;

    private MediaServer mediaServer;

    public static CommonChannelPlayInfo build(MediaServer mediaServer, StreamInfo data) {
        CommonChannelPlayInfo commonChannelPlayInfo = new CommonChannelPlayInfo();
        commonChannelPlayInfo.setMediaServer(mediaServer);
        commonChannelPlayInfo.setStreamInfo(data);
        return commonChannelPlayInfo;
    }
}
