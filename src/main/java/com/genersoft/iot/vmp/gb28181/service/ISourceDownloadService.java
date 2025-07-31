package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

/**
 * 资源能力接入-录像下载
 */
public interface ISourceDownloadService {

    void download(CommonGBChannel channel, Long startTime, Long stopTime, Integer downloadSpeed, ErrorCallback<StreamInfo> callback);

    void stopDownload(CommonGBChannel channel, String stream);
}
