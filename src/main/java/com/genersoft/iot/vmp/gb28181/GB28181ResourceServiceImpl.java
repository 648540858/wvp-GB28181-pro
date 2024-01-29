package com.genersoft.iot.vmp.gb28181;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.command.PTZCommand;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.IResourcePlayCallback;
import com.genersoft.iot.vmp.service.IResourceService;
import com.genersoft.iot.vmp.service.bean.CommonGbChannelType;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.aspectj.bridge.ICommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;


/**
 * 国标的资源实现类
 */
@Service(CommonGbChannelType.GB28181)
public class GB28181ResourceServiceImpl implements IResourceService {

    private final Logger logger = LoggerFactory.getLogger(GB28181ResourceServiceImpl.class);

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    @Autowired
    private IPlayService playService;

    @Autowired
    private ISIPCommander commander;

    @Override
    public boolean deleteChannel(CommonGbChannel commonGbChannel) {
        if (!CommonGbChannelType.GB28181.equals(commonGbChannel.getType())) {
            logger.warn("[资源类-国标28181] 收到移除通道： {} 时发现类型不为28181", commonGbChannel.getCommonGbId());
            return false;
        }
        return deviceChannelMapper.removeCommonChannelId(commonGbChannel.getCommonGbId()) > 0;
    }

    @Override
    public void startPlay(CommonGbChannel commonGbChannel, IResourcePlayCallback callback) {
        assert callback != null;
        CheckCommonGbChannelResult checkResult = checkCommonGbChannel(commonGbChannel);

        if (checkResult.errorMsg != null) {
            callback.call(commonGbChannel, null, ErrorCode.SUCCESS.getCode(), checkResult.errorMsg, null);
            return;
        }
        if (checkResult.device == null || checkResult.channel == null) {
            callback.call(commonGbChannel, null, ErrorCode.SUCCESS.getCode(), "设备获取失败", null);
            return;
        }

        MediaServerItem mediaServerItem = playService.getNewMediaServerItem(checkResult.device);
        playService.play(mediaServerItem, checkResult.channel.getDeviceId(), checkResult.channel.getChannelId(), null, (code, msg, data) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                StreamInfo streamInfo = (StreamInfo)data;
                callback.call(commonGbChannel, mediaServerItem, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
            }else {
                callback.call(commonGbChannel, null, code, msg, null);
            }
        });
    }

    @Override
    public void stopPlay(CommonGbChannel commonGbChannel, IResourcePlayCallback callback) {
        CheckCommonGbChannelResult checkResult = checkCommonGbChannel(commonGbChannel);

        if (checkResult.errorMsg != null) {
            callback.call(commonGbChannel, null, ErrorCode.SUCCESS.getCode(), checkResult.errorMsg, null);
            return;
        }
        if (checkResult.device == null || checkResult.channel == null) {
            callback.call(commonGbChannel, null, ErrorCode.SUCCESS.getCode(), "设备获取失败", null);
            return;
        }
        try {
            playService.stop(checkResult.channel.getDeviceId(), checkResult.channel.getChannelId());
        } catch (ControllerException exception) {
            if (callback != null) {
                callback.call(commonGbChannel, null,exception.getCode(), exception.getMsg(), null);
            }
        }
    }

    @Override
    public boolean ptzControl(CommonGbChannel commonGbChannel, PTZCommand ptzCommand) {
        CheckCommonGbChannelResult checkResult = checkCommonGbChannel(commonGbChannel);

        if (checkResult.errorMsg != null) {
            logger.warn("[资源类-国标28181] 云台控制失败： {}", checkResult.errorMsg);
            return false;
        }
        if (checkResult.device == null || checkResult.channel == null) {
            logger.warn("[资源类-国标28181] 云台控制失败： 设备获取失败");
            return false;
        }
        try {
            commander.ptzCmd(checkResult.device, checkResult.channel.getChannelId(), ptzCommand);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败]： ", e);
        }
        return false;
    }

    @Override
    public void streamOffline(String app, String streamId) {
        // TODO
    }

    @Override
    public void startPlayback(CommonGbChannel channel, Long startTime, Long stopTime, IResourcePlayCallback callback) {

    }

    @Override
    public void startDownload(CommonGbChannel channel, Long startTime, Long stopTime, Integer downloadSpeed, IResourcePlayCallback playCallback) {

    }

    static class CheckCommonGbChannelResult {
        Device device;

        DeviceChannel channel;

        String errorMsg;

        public CheckCommonGbChannelResult(String errorMsg) {
            this.errorMsg = errorMsg;
        }

        public CheckCommonGbChannelResult(Device device, DeviceChannel channel) {
            this.device = device;
            this.channel = channel;
        }
    }

    private CheckCommonGbChannelResult checkCommonGbChannel(CommonGbChannel commonGbChannel) {
        if (commonGbChannel == null) {
            logger.warn("[资源类-检验参数] 通道不可为NULL");
            return new CheckCommonGbChannelResult("通道不可为NULL");
        }
        if (!CommonGbChannelType.GB28181.equals(commonGbChannel.getType())) {
            logger.warn("[资源类-国标28181] 收到通道： {} 时发现类型不为28181", commonGbChannel.getCommonGbId());
            return new CheckCommonGbChannelResult("数据关系错误");
        }
        DeviceChannel channel = deviceChannelMapper.getChannelByCommonChannelId(commonGbChannel.getCommonGbId());
        if (channel == null) {
            logger.warn("[资源类-国标28181] 收到通道： {} 时未找到国标通道", commonGbChannel.getCommonGbId());
            return new CheckCommonGbChannelResult("未找到通道");
        }
        Device device = deviceMapper.getDeviceByDeviceId(channel.getDeviceId());
        if (device == null) {
            logger.warn("[资源类-国标28181] 收到通道： {} 时未找到通道 {} 所属的国标设备",
                    commonGbChannel.getCommonGbId(), channel.getDeviceId());
            return new CheckCommonGbChannelResult("未找到设备");
        }
        return new CheckCommonGbChannelResult(device, channel);
    }
}
