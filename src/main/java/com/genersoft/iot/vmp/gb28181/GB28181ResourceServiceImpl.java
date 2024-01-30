package com.genersoft.iot.vmp.gb28181;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.DeviceControlType;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.DragZoomRequest;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.command.PTZCommand;
import com.genersoft.iot.vmp.gb28181.event.record.RecordEndEventListener;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.IResourcePlayCallback;
import com.genersoft.iot.vmp.service.IResourceService;
import com.genersoft.iot.vmp.service.bean.CommonGbChannelType;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;


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
    private IMediaServerService mediaServerService;

    @Autowired
    private ISIPCommander commander;

    @Autowired
    private RecordEndEventListener recordEndEventListener;

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
    public void resetAlarm(CommonGbChannel commonGbChannel, Integer alarmMethod, Integer alarmType) {
        CheckCommonGbChannelResult checkResult = checkCommonGbChannel(commonGbChannel);

        if (checkResult.errorMsg != null) {
            logger.warn("[资源类-国标28181] 报警处理失败： {}", checkResult.errorMsg);
            return;
        }
        if (checkResult.device == null || checkResult.channel == null) {
            logger.warn("[资源类-国标28181] 报警处理失败： 设备获取失败");
            return;
        }
        try {
            commander.alarmCmd(checkResult.device, alarmMethod, alarmType,null, null);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败]： ", e);
        }
    }

    @Override
    public void setGuard(CommonGbChannel commonGbChannel, boolean setGuard) {
        CheckCommonGbChannelResult checkResult = checkCommonGbChannel(commonGbChannel);

        if (checkResult.errorMsg != null) {
            logger.warn("[资源类-国标28181] 布防/撤防失败： {}", checkResult.errorMsg);
            return;
        }
        if (checkResult.device == null || checkResult.channel == null) {
            logger.warn("[资源类-国标28181] 布防/撤防失败： 设备获取失败");
            return;
        }

        try {
            commander.guardCmd(checkResult.device, setGuard,null, null);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 布防/撤防命令: {}", e.getMessage());
        }
    }

    @Override
    public void setRecord(CommonGbChannel commonGbChannel, Boolean isRecord) {
        CheckCommonGbChannelResult checkResult = checkCommonGbChannel(commonGbChannel);

        if (checkResult.errorMsg != null) {
            logger.warn("[资源类-国标28181] 录像控制失败： {}", checkResult.errorMsg);
            return;
        }
        if (checkResult.device == null || checkResult.channel == null) {
            logger.warn("[资源类-国标28181] 录像控制失败： 设备获取失败");
            return;
        }

        try {
            commander.recordCmd(checkResult.device, checkResult.channel.getChannelId(), isRecord,null, null);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 录像控制命令: {}", e.getMessage());
        }
    }

    @Override
    public void setIFame(CommonGbChannel commonGbChannel) {
        CheckCommonGbChannelResult checkResult = checkCommonGbChannel(commonGbChannel);

        if (checkResult.errorMsg != null) {
            logger.warn("[资源类-国标28181] 强制关键帧失败： {}", checkResult.errorMsg);
            return;
        }
        if (checkResult.device == null || checkResult.channel == null) {
            logger.warn("[资源类-国标28181] 强制关键帧失败： 设备获取失败");
            return;
        }

        try {
            commander.iFrameCmd(checkResult.device, checkResult.channel.getChannelId());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 强制关键帧: {}", e.getMessage());
        }
    }

    @Override
    public void setTeleBoot(CommonGbChannel commonGbChannel) {
        CheckCommonGbChannelResult checkResult = checkCommonGbChannel(commonGbChannel);

        if (checkResult.errorMsg != null) {
            logger.warn("[资源类-国标28181] 重启设备失败： {}", checkResult.errorMsg);
            return;
        }
        if (checkResult.device == null || checkResult.channel == null) {
            logger.warn("[资源类-国标28181] 重启设备失败： 设备获取失败");
            return;
        }

        try {
            commander.teleBootCmd(checkResult.device);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 重启设备: {}", e.getMessage());
        }
    }

    @Override
    public void dragZoom(CommonGbChannel commonGbChannel, DragZoomRequest.DragZoom dragZoom, boolean isIn) {
        CheckCommonGbChannelResult checkResult = checkCommonGbChannel(commonGbChannel);

        if (checkResult.errorMsg != null) {
            logger.warn("[资源类-国标28181] 拉框放大/缩小失败： {}", checkResult.errorMsg);
            return;
        }
        if (checkResult.device == null || checkResult.channel == null) {
            logger.warn("[资源类-国标28181] 拉框放大/缩小失败： 设备获取失败");
            return;
        }
        StringBuffer cmdXml = new StringBuffer(200);
        String type = isIn? DeviceControlType.DRAG_ZOOM_IN.getVal(): DeviceControlType.DRAG_ZOOM_OUT.getVal();
        cmdXml.append("<" + type + ">\r\n");
        cmdXml.append("<Length>" + dragZoom.getLength() + "</Length>\r\n");
        cmdXml.append("<Width>" + dragZoom.getWidth() + "</Width>\r\n");
        cmdXml.append("<MidPointX>" + dragZoom.getMidPointX() + "</MidPointX>\r\n");
        cmdXml.append("<MidPointY>" + dragZoom.getMidPointY() + "</MidPointY>\r\n");
        cmdXml.append("<LengthX>" + dragZoom.getLengthX() + "</LengthX>\r\n");
        cmdXml.append("<LengthY>" + dragZoom.getLengthY() + "</LengthY>\r\n");
        cmdXml.append("</" + type + ">\r\n");
        try {
            commander.dragZoomCmd(checkResult.device, checkResult.channel.getChannelId(), cmdXml.toString());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 拉框放大/缩小: {}", e.getMessage());
        }
    }

    @Override
    public void setHomePosition(CommonGbChannel commonGbChannel, boolean enabled, Integer resetTime, Integer presetIndex) {
        CheckCommonGbChannelResult checkResult = checkCommonGbChannel(commonGbChannel);

        if (checkResult.errorMsg != null) {
            logger.warn("[资源类-国标28181] 看守位控制失败： {}", checkResult.errorMsg);
            return;
        }
        if (checkResult.device == null || checkResult.channel == null) {
            logger.warn("[资源类-国标28181] 看守位控制失败： 设备获取失败");
            return;
        }
        try {
            commander.homePositionCmd(checkResult.device, checkResult.channel.getChannelId(),
                    enabled, resetTime, presetIndex, null, null);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 看守位控制: {}", e.getMessage());
        }

    }

    @Override
    public void queryrecord(CommonGbChannel commonGbChannel, int sn, int secrecy, String type, String startTime, String endTime, CommonCallback<RecordInfo> callback) {
        CheckCommonGbChannelResult checkResult = checkCommonGbChannel(commonGbChannel);

        if (checkResult.errorMsg != null) {
            logger.warn("[资源类-国标28181] 国标录像查询失败： {}", checkResult.errorMsg);
            return;
        }
        if (checkResult.device == null || checkResult.channel == null) {
            logger.warn("[资源类-国标28181] 国标录像查询失败： 设备获取失败");
            return;
        }
        // 接收录像数据
        recordEndEventListener.addEndEventHandler(checkResult.device.getDeviceId(), checkResult.channel.getChannelId(), (recordInfo)->{
            if (recordInfo == null ) {
                logger.info("[资源类-国标28181] 录像查询, 结果为空，设备: {}, 通道：{}",
                        checkResult.device.getDeviceId(), checkResult.channel.getChannelId());
                return;
            }
            if (recordInfo.getRecordList() == null) {
                recordInfo.setRecordList(new ArrayList<>());
            }
            logger.info("[资源类-国标28181] 录像查询收到数据，设备: {}, 通道：{}，共{}条",
                    checkResult.device.getDeviceId(), checkResult.channel.getChannelId(), recordInfo.getRecordList().size());
            if (callback != null) {
                callback.run(recordInfo);
            }
        });
        try {
            commander.recordInfoQuery(checkResult.device, checkResult.channel.getChannelId(), DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(startTime),
                    DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(endTime), sn, secrecy, type, null, null);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            logger.error("[命令发送失败] 录像查询: {}", e.getMessage());
        }
    }

    @Override
    public void streamOffline(String app, String streamId) {
        // TODO
    }

    @Override
    public void startPlayback(CommonGbChannel commonGbChannel, Instant startTime, Instant stopTime, IResourcePlayCallback callback) {
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
        String startTimeStr = DateUtil.formatter.format(startTime);
        String endTimeStr = DateUtil.formatter.format(stopTime);
        String stream = checkResult.device.getDeviceId() + "_" + checkResult.channel.getChannelId() + "_" +
                startTimeStr + "_" + endTimeStr;
        MediaServerItem mediaServerItem = playService.getNewMediaServerItem(checkResult.device);
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, stream, null,
                checkResult.device.isSsrcCheck(),  true, 0, false, checkResult.device.getStreamModeForParam());
        playService.playBack(mediaServerItem, ssrcInfo, checkResult.channel.getDeviceId(), checkResult.channel.getChannelId(),
                startTimeStr, endTimeStr, (code, msg, data) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                StreamInfo streamInfo = (StreamInfo)data;
                callback.call(commonGbChannel, mediaServerItem, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
            }else {
                callback.call(commonGbChannel, null, code, msg, null);
            }
        });
    }

    @Override
    public void startDownload(CommonGbChannel channel, Instant startTime, Instant stopTime, Integer downloadSpeed, IResourcePlayCallback playCallback) {

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
