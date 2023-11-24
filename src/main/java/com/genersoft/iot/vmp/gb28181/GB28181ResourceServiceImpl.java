package com.genersoft.iot.vmp.gb28181;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.IResourcePlayCallback;
import com.genersoft.iot.vmp.service.IResourceService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import com.genersoft.iot.vmp.storager.impl.RedisCatchStorageImpl;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * 国标的资源实现类
 */
@Service("28181")
public class GB28181ResourceServiceImpl implements IResourceService {

    private final Logger logger = LoggerFactory.getLogger(GB28181ResourceServiceImpl.class);

    public static final String resourceType = "28181";

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    @Autowired
    private IPlayService playService;

    @Override
    public boolean deleteChannel(CommonGbChannel commonGbChannel) {
        if (!GB28181ResourceServiceImpl.resourceType.equals(commonGbChannel.getType())) {
            logger.warn("[资源类-国标28181] 收到移除通道： {} 时发现类型不为28181", commonGbChannel.getCommonGbId());
            return false;
        }
        return deviceChannelMapper.removeCommonChannelId(commonGbChannel.getCommonGbId()) > 0;
    }

    @Override
    public void startPlay(CommonGbChannel commonGbChannel, IResourcePlayCallback callback) {
        assert callback != null;
        if (!GB28181ResourceServiceImpl.resourceType.equals(commonGbChannel.getType())) {
            logger.warn("[资源类-国标28181] 收到播放通道： {} 时发现类型不为28181", commonGbChannel.getCommonGbId());
            callback.call(commonGbChannel, ErrorCode.ERROR500.getCode(), ErrorCode.ERROR500.getMsg(), null);
            return;
        }
        DeviceChannel channel = deviceChannelMapper.getChannelByCommonChannelId(commonGbChannel.getCommonGbId());
        if (channel == null) {
            logger.warn("[资源类-国标28181] 收到播放通道： {} 时未找到国标通道", commonGbChannel.getCommonGbId());
            callback.call(commonGbChannel, ErrorCode.ERROR500.getCode(), ErrorCode.ERROR500.getMsg(), null);
            return;
        }
        Device device = deviceMapper.getDeviceByDeviceId(channel.getDeviceId());
        if (device == null) {
            logger.warn("[资源类-国标28181] 收到播放通道： {} 时未找到通道 {} 所属的国标设备",
                    commonGbChannel.getCommonGbId(), channel.getDeviceId());
            callback.call(commonGbChannel, ErrorCode.ERROR500.getCode(), ErrorCode.ERROR500.getMsg(), null);
            return;
        }
        MediaServerItem mediaServerItem = playService.getNewMediaServerItem(device);
        playService.play(mediaServerItem, channel.getDeviceId(), channel.getChannelId(), null, (code, msg, data) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                if (data != null) {
                    StreamInfo streamInfo = (StreamInfo)data;
                    callback.call(commonGbChannel, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
                }
            }else {
                callback.call(commonGbChannel, code, msg, null);
            }
        });
    }

    @Override
    public void stopPlay(CommonGbChannel commonGbChannel, IResourcePlayCallback callback) {
        if (!GB28181ResourceServiceImpl.resourceType.equals(commonGbChannel.getType())) {
            logger.warn("[资源类-国标28181] 收到停止播放通道： {} 时发现类型不为28181", commonGbChannel.getCommonGbId());
            if (callback != null) {
                callback.call(commonGbChannel, ErrorCode.ERROR500.getCode(), ErrorCode.ERROR500.getMsg(), null);
            }
            return;
        }
        DeviceChannel channel = deviceChannelMapper.getChannelByCommonChannelId(commonGbChannel.getCommonGbId());
        if (channel == null) {
            logger.warn("[资源类-国标28181] 收到停止播放通道： {} 时未找到国标通道", commonGbChannel.getCommonGbId());
            if (callback != null) {
                callback.call(commonGbChannel, ErrorCode.ERROR500.getCode(), ErrorCode.ERROR500.getMsg(), null);
            }
            return;
        }
        try {
            playService.stop(channel.getDeviceId(), channel.getChannelId());
        } catch (ControllerException exception) {
            if (callback != null) {
                callback.call(commonGbChannel, exception.getCode(), exception.getMsg(), null);
            }
        }
    }

    @Override
    public boolean ptzControl(CommonGbChannel commonGbChannel,
                              String command, Integer horizonSpeed,
                              Integer verticalSpeed, Integer zoomSpeed) {
        return false;
    }

}
