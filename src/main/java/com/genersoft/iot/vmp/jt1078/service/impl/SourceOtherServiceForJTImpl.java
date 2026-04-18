package com.genersoft.iot.vmp.jt1078.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.service.ISourceOtherService;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078PlayService;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service(ChannelDataType.OTHER_SERVICE + ChannelDataType.JT_1078)
@RequiredArgsConstructor
public class SourceOtherServiceForJTImpl implements ISourceOtherService {

    private final UserSetting userSetting;

    private final Ijt1078PlayService jt1078PlayService;

    @Override
    public Boolean closeStreamOnNoneReader(String mediaServerId, String app, String stream, String schema) {
        if (!MediaStreamUtil.isJT1078(app, stream)) {
            return null;
        }
        if (userSetting.getStreamOnDemand()) {
            String[] streamParamArray =  MediaStreamUtil.getJT1078StreamInfo(app, stream);
            if (streamParamArray == null || streamParamArray.length < 2) {
                return true;
            }
            String phoneNumber = streamParamArray[0];
            Integer channelId  = Integer.parseInt(streamParamArray[1]);
            // 判断是否是1078播放类型
            if (MediaStreamUtil.isJT1078Play(app, stream)) {
                jt1078PlayService.stopPlay(phoneNumber, channelId);
            } else if (MediaStreamUtil.isJT1078Playback(app, stream)) {
                jt1078PlayService.stopPlayback(phoneNumber, channelId);
            }
            return true;
        }
        return false;
    }

    @Override
    public Boolean addChannelIdForMobilePosition(List<? extends MobilePosition> mobilePositionList) {

        return null;
    }
}
