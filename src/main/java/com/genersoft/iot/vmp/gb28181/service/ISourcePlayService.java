package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

public interface ISourcePlayService {

    void play(CommonGBChannel channel, Boolean record, ErrorCallback<StreamInfo> callback);

    void stopPlay(CommonGBChannel channel, String stream);

}
