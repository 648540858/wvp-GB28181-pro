package com.genersoft.iot.vmp.streamPush.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.service.ISourceOtherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service(ChannelDataType.OTHER_SERVICE + ChannelDataType.STREAM_PUSH)
@RequiredArgsConstructor
public class SourceOtherServiceForStreamPushImpl implements ISourceOtherService {


    @Override
    public Boolean closeStreamOnNoneReader(String mediaServerId, String app, String stream, String schema) {
        // 推流服务不需要关闭流
        return false;
    }

    @Override
    public Boolean addChannelIdForMobilePosition(List<? extends MobilePosition> mobilePositionList) {
        // 推流服务不需要处理移动位置的通道ID
        return true;
    }
}
