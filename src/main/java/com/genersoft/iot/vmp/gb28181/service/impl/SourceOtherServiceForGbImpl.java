package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.service.ISourceOtherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service(ChannelDataType.OTHER_SERVICE + ChannelDataType.GB28181)
@RequiredArgsConstructor
public class SourceOtherServiceForGbImpl implements ISourceOtherService {


    private final IInviteStreamService inviteStreamService;

    private final IDeviceChannelService deviceChannelService;

    private final UserSetting userSetting;

    @Override
    public Boolean closeStreamOnNoneReader(String mediaServerId, String app, String stream, String schema) {
        if (MediaStreamUtil.GB28181_TALK.equals(app) ||  MediaStreamUtil.GB28181_BROADCAST.equals(app)) {
            // 国标对讲/广播流， 直接关闭
            return false;
        }
        if (!MediaStreamUtil.isGB28181(app, stream)) {
            return null;
        }
        // 国标流， 点播/录像回放/录像下载
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(null, stream);
        if (inviteInfo == null) {
            return null;
        }
        if (inviteInfo.getStatus() == InviteSessionStatus.ok) {
            // 录像下载
            if (inviteInfo.getType() == InviteSessionType.DOWNLOAD) {
                return false;
            }
            DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(inviteInfo.getChannelId());
            if (deviceChannel == null) {
                return false;
            }
        }
        return userSetting.getStreamOnDemand();
    }
}
