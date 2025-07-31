package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.service.ISourceDownloadService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service(ChannelDataType.DOWNLOAD_SERVICE + ChannelDataType.GB28181)
public class SourceDownloadServiceForGbImpl implements ISourceDownloadService {

    @Autowired
    private IPlayService deviceChannelPlayService;

    @Override
    public void download(CommonGBChannel channel, Long startTime, Long stopTime, Integer downloadSpeed, ErrorCallback<StreamInfo> callback) {

    }

    @Override
    public void stopDownload(CommonGBChannel channel, String stream) {
        // 国标通道
        try {
            deviceChannelPlayService.stop(InviteSessionType.DOWNLOAD, channel, stream);
        }  catch (Exception e) {
            log.error("[停止下载失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
        }
    }
}
