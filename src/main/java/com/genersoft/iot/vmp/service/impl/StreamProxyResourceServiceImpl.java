package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.DragZoomRequest;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.command.PTZCommand;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxy;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IResourcePlayCallback;
import com.genersoft.iot.vmp.service.IResourceService;
import com.genersoft.iot.vmp.service.IStreamProxyPlayService;
import com.genersoft.iot.vmp.service.bean.CommonGbChannelType;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service(CommonGbChannelType.PROXY)
public class StreamProxyResourceServiceImpl implements IResourceService {

    private final Logger logger = LoggerFactory.getLogger(StreamProxyResourceServiceImpl.class);

    @Autowired
    private StreamProxyMapper streamProxyMapper;

    @Autowired
    private IStreamProxyPlayService streamProxyPlayService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Override
    public boolean deleteChannel(CommonGbChannel commonGbChannel) {
        return false;
    }

    @Override
    public void startPlay(CommonGbChannel commonGbChannel, IResourcePlayCallback callback) {
        assert callback != null;
        if (!CommonGbChannelType.PROXY.equals(commonGbChannel.getType())) {
            logger.warn("[资源类-拉流代理] 收到播放通道： {} 时发现类型不为proxy", commonGbChannel.getCommonGbId());
            callback.call(commonGbChannel, null, ErrorCode.ERROR100.getCode(), "数据关系错误", null);
            return;
        }
        StreamProxy streamProxy = streamProxyMapper.selectOneByByCommonGbChannelId(commonGbChannel.getCommonGbId());
        if (streamProxy == null) {
            logger.warn("[资源类-拉流代理] 收到播放通道： {} 时未找到国标通道", commonGbChannel.getCommonGbId());
            callback.call(commonGbChannel, null, ErrorCode.ERROR100.getCode(), "未找到通道", null);
            return;
        }
        String mediaServerId = streamProxy.getMediaServerId();
        MediaServerItem mediaServerItem;
        if (ObjectUtils.isEmpty(mediaServerId) || mediaServerId.equals("auto")) {
            mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(null);
        }else {
            mediaServerItem = mediaServerService.getOne(mediaServerId);
        }
        if (mediaServerItem == null) {
            callback.call(commonGbChannel, null, ErrorCode.ERROR100.getCode(), "未找到可用的节点", null);
            return;
        }
        streamProxyPlayService.startProxy(streamProxy, mediaServerItem, (code, msg, data) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                callback.call(commonGbChannel, mediaServerItem, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
            }else {
                callback.call(commonGbChannel, null, code, msg, null);
            }
        });
    }

    @Override
    public void stopPlay(CommonGbChannel commonGbChannel, IResourcePlayCallback callback) {

    }

    @Override
    public boolean ptzControl(CommonGbChannel commonGbChannel, PTZCommand ptzCommand) {
        return false;
    }

    @Override
    public void streamOffline(String app, String streamId) {

    }

    @Override
    public void startPlayback(CommonGbChannel channel, Long startTime, Long stopTime, IResourcePlayCallback callback) {

    }

    @Override
    public void startDownload(CommonGbChannel channel, Long startTime, Long stopTime, Integer downloadSpeed, IResourcePlayCallback playCallback) {

    }

    @Override
    public void resetAlarm(CommonGbChannel commonGbChannel, Integer alarmMethod, Integer alarmType) {

    }

    @Override
    public void setGuard(CommonGbChannel commonGbChannel, boolean setGuard) {

    }

    @Override
    public void setRecord(CommonGbChannel commonGbChannel, Boolean isRecord) {

    }

    @Override
    public void setIFame(CommonGbChannel commonGbChannel) {

    }

    @Override
    public void setTeleBoot(CommonGbChannel commonGbChannel) {

    }

    @Override
    public void dragZoom(CommonGbChannel commonGbChannel, DragZoomRequest.DragZoom dragZoom, boolean isIn) {

    }

    @Override
    public void setHomePosition(CommonGbChannel commonGbChannel, boolean enabled, Integer resetTime, Integer presetIndex) {

    }

    @Override
    public void queryrecord(CommonGbChannel commonGbChannel, int sn, int secrecy, String type, String startTime, String endTime, CommonCallback<RecordInfo> callback) {

    }
}
