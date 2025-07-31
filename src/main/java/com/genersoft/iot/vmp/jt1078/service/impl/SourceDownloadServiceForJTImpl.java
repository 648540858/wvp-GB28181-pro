package com.genersoft.iot.vmp.jt1078.service.impl;

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
@Service(ChannelDataType.DOWNLOAD_SERVICE + ChannelDataType.JT_1078)
public class SourceDownloadServiceForJTImpl implements ISourceDownloadService {


    @Override
    public void download(CommonGBChannel channel, Long startTime, Long stopTime, Integer downloadSpeed, ErrorCallback<StreamInfo> callback) {

    }

    @Override
    public void stopDownload(CommonGBChannel channel, String stream) {

    }
}
