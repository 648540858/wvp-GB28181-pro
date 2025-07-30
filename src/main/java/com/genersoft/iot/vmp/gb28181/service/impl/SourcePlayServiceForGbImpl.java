package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.service.ISourcePlayService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import org.springframework.stereotype.Service;

@Service("SourcePlayService" + ChannelDataType.GB28181)
public class SourcePlayServiceForGbImpl implements ISourcePlayService {

    @Override
    public void play(CommonGBChannel channel, Boolean record, ErrorCallback<StreamInfo> callback) {

    }

    @Override
    public void stopPlay(CommonGBChannel channel, String stream) {

    }
}
