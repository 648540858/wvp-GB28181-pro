package com.genersoft.iot.vmp.streamProxy.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.media.MediaNotFoundEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOfflineEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOnlineEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OriginType;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxyParam;
import com.genersoft.iot.vmp.streamProxy.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyPlayService;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频代理业务
 */
@Slf4j
@Service
public class StreamProxyServiceImpl implements IStreamProxyService {

    @Autowired
    private StreamProxyMapper streamProxyMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IStreamProxyPlayService playService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IGbChannelService gbChannelService;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    /**
     * 流到来的处理
     */
    @Async("taskExecutor")
    @Transactional
    @org.springframework.context.event.EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {
        if ("rtsp".equals(event.getSchema())) {
            streamChangeHandler(event.getApp(), event.getStream(), event.getMediaServer().getId(), true);
        }
    }

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaDepartureEvent event) {
        if ("rtsp".equals(event.getSchema())) {
            streamChangeHandler(event.getApp(), event.getStream(), event.getMediaServer().getId(), false);
        }
    }

    /**
     * 流未找到的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaNotFoundEvent event) {
        if ("rtp".equals(event.getApp())) {
            return;
        }
        // 拉流代理
        StreamProxy streamProxyByAppAndStream = getStreamProxyByAppAndStream(event.getApp(), event.getStream());
        if (streamProxyByAppAndStream != null && streamProxyByAppAndStream.isEnableDisableNoneReader()) {
            startByAppAndStream(event.getApp(), event.getStream());
        }
    }

    /**
     * 流媒体节点上线
     */
    @Async("taskExecutor")
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaServerOnlineEvent event) {
        zlmServerOnline(event.getMediaServer());
    }

    /**
     * 流媒体节点离线
     */
    @Async("taskExecutor")
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaServerOfflineEvent event) {
        zlmServerOffline(event.getMediaServer());
    }


    @Override
    @Transactional
    public StreamInfo save(StreamProxyParam param) {
        // 兼容旧接口
        StreamProxy streamProxyInDb = getStreamProxyByAppAndStream(param.getApp(), param.getStream());
        if (streamProxyInDb != null && streamProxyInDb.getPulling() != null && streamProxyInDb.getPulling()) {
            playService.stopProxy(streamProxyInDb);
        }
        if (param.getMediaServerId().equals("auto")) {
            param.setMediaServerId(null);
        }
        StreamProxy streamProxy = param.buildStreamProxy(userSetting.getServerId());

        if (streamProxyInDb == null) {
            add(streamProxy);
        } else {
            try {
                playService.stopProxy(streamProxyInDb);
            } catch (ControllerException ignored) {
            }
            streamProxyMapper.delete(streamProxyInDb.getId());
            add(streamProxy);
        }

        if (param.isEnable()) {
            return playService.startProxy(streamProxy);
        } else {
            return null;
        }
    }

    @Override
    @Transactional
    public void add(StreamProxy streamProxy) {
        StreamProxy streamProxyInDb = streamProxyMapper.selectOneByAppAndStream(streamProxy.getApp(), streamProxy.getStream());
        if (streamProxyInDb != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "APP+STREAM已经存在");
        }
        if (streamProxy.getGbDeviceId() != null) {
            gbChannelService.add(streamProxy.buildCommonGBChannel());
        }
        streamProxy.setCreateTime(DateUtil.getNow());
        streamProxy.setUpdateTime(DateUtil.getNow());
        streamProxyMapper.add(streamProxy);
        streamProxy.setDataType(ChannelDataType.STREAM_PROXY.value);
        streamProxy.setDataDeviceId(streamProxy.getId());
    }

    @Override
    public void delete(int id) {
        StreamProxy streamProxy = getStreamProxy(id);
        if (streamProxy == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "代理不存在");
        }
        delete(streamProxy);
    }

    private void delete(StreamProxy streamProxy) {
        Assert.notNull(streamProxy, "代理不可为NULL");
        if (streamProxy.getPulling() != null && streamProxy.getPulling()) {
            playService.stopProxy(streamProxy);
        }
        if (streamProxy.getGbId() > 0) {
            gbChannelService.delete(streamProxy.getGbId());
        }
        streamProxyMapper.delete(streamProxy.getId());
    }

    @Override
    @Transactional
    public void delteByAppAndStream(String app, String stream) {
        StreamProxy streamProxy = streamProxyMapper.selectOneByAppAndStream(app, stream);
        if (streamProxy == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "代理不存在");
        }
        delete(streamProxy);
    }

    /**
     * 更新代理流
     */
    @Override
    public boolean update(StreamProxy streamProxy) {
        streamProxy.setUpdateTime(DateUtil.getNow());
        StreamProxy streamProxyInDb = streamProxyMapper.select(streamProxy.getId());
        if (streamProxyInDb == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "代理不存在");
        }
        int updateResult = streamProxyMapper.update(streamProxy);
        if (updateResult > 0 && !ObjectUtils.isEmpty(streamProxy.getGbDeviceId())) {
            if (streamProxy.getGbId() > 0) {
                gbChannelService.update(streamProxy.buildCommonGBChannel());
            } else {
                gbChannelService.add(streamProxy.buildCommonGBChannel());
            }
        }
        return true;
    }

    @Override
    public PageInfo<StreamProxy> getAll(Integer page, Integer count, String query, Boolean pulling, String mediaServerId) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<StreamProxy> all = streamProxyMapper.selectAll(query, pulling, mediaServerId);
        return new PageInfo<>(all);
    }


    @Override
    public boolean startByAppAndStream(String app, String stream) {
        StreamProxy streamProxy = streamProxyMapper.selectOneByAppAndStream(app, stream);
        if (streamProxy == null) {
            throw new ControllerException(ErrorCode.ERROR404.getCode(), "代理信息未找到");
        }
        StreamInfo streamInfo = playService.startProxy(streamProxy);
        return streamInfo != null;
    }

    @Override
    public void stopByAppAndStream(String app, String stream) {
        StreamProxy streamProxy = streamProxyMapper.selectOneByAppAndStream(app, stream);
        if (streamProxy == null) {
            throw new ControllerException(ErrorCode.ERROR404.getCode(), "代理信息未找到");
        }
        playService.stopProxy(streamProxy);
    }


    @Override
    public Map<String, String> getFFmpegCMDs(MediaServer mediaServer) {
        return mediaServerService.getFFmpegCMDs(mediaServer);
    }


    @Override
    public StreamProxy getStreamProxyByAppAndStream(String app, String stream) {
        return streamProxyMapper.selectOneByAppAndStream(app, stream);
    }

    @Override
    @Transactional
    public void zlmServerOnline(MediaServer mediaServer) {
        if (mediaServer == null) {
            return;
        }
        // 这里主要是控制数据库/redis缓存/以及zlm中存在的代理流 三者状态一致。以数据库中数据为根本
        redisCatchStorage.removeStream(mediaServer.getId(), "PULL");

        List<StreamProxy> streamProxies = streamProxyMapper.selectForPushingInMediaServer(mediaServer.getId(), true);
        if (streamProxies.isEmpty()) {
            return;
        }
        Map<String, StreamProxy> streamProxyMapForDb = new HashMap<>();
        for (StreamProxy streamProxy : streamProxies) {
            streamProxyMapForDb.put(streamProxy.getApp() + "_" + streamProxy.getStream(), streamProxy);
        }

        List<StreamInfo> streamInfoList = mediaServerService.getMediaList(mediaServer, null, null, null);

        List<CommonGBChannel> channelListForOnline = new ArrayList<>();
        for (StreamInfo streamInfo : streamInfoList) {
            String key = streamInfo.getApp() + streamInfo.getStream();
            StreamProxy streamProxy = streamProxyMapForDb.get(key);
            if (streamProxy == null) {
                // 流媒体存在，数据库中不存在
                continue;
            }
            if (streamInfo.getOriginType() == OriginType.PULL.ordinal()
                    || streamInfo.getOriginType() == OriginType.FFMPEG_PULL.ordinal()) {
                if (streamProxyMapForDb.get(key) != null) {
                    redisCatchStorage.addStream(mediaServer, "pull", streamInfo.getApp(), streamInfo.getStream(), streamInfo.getMediaInfo());
                    if ("OFF".equalsIgnoreCase(streamProxy.getGbStatus()) && streamProxy.getGbId() > 0) {
                        streamProxy.setGbStatus("ON");
                        channelListForOnline.add(streamProxy.buildCommonGBChannel());
                    }
                    streamProxyMapForDb.remove(key);
                }
            }
        }

        if (!channelListForOnline.isEmpty()) {
            gbChannelService.online(channelListForOnline);
        }
        List<CommonGBChannel> channelListForOffline = new ArrayList<>();
        List<StreamProxy> streamProxiesForRemove = new ArrayList<>();
        if (!streamProxyMapForDb.isEmpty()) {
            for (StreamProxy streamProxy : streamProxyMapForDb.values()) {
                if ("ON".equalsIgnoreCase(streamProxy.getGbStatus()) && streamProxy.getGbId() > 0) {
                    streamProxy.setGbStatus("OFF");
                    channelListForOffline.add(streamProxy.buildCommonGBChannel());
                }
                // 移除开启了无人观看自动移除的流
                if (streamProxy.getGbDeviceId() == null && streamProxy.isEnableRemoveNoneReader()) {
                    streamProxiesForRemove.add(streamProxy);
                    streamProxyMapForDb.remove(streamProxy.getApp() + streamProxy.getStream());
                }
            }
        }
        if (!channelListForOffline.isEmpty()) {
            gbChannelService.offline(channelListForOffline);
        }
        if (!streamProxiesForRemove.isEmpty()) {
            streamProxyMapper.deleteByList(streamProxiesForRemove);
        }

        if (!streamProxyMapForDb.isEmpty()) {
            for (StreamProxy streamProxy : streamProxyMapForDb.values()) {
                streamProxyMapper.offline(streamProxy.getId());
            }
        }
    }

    @Override
    public void zlmServerOffline(MediaServer mediaServer) {
        List<StreamProxy> streamProxies = streamProxyMapper.selectForPushingInMediaServer(mediaServer.getId(), true);

        // 清理redis相关的缓存
        redisCatchStorage.removeStream(mediaServer.getId(), "PULL");

        if (streamProxies.isEmpty()) {
            return;
        }
        List<StreamProxy> streamProxiesForRemove = new ArrayList<>();
        List<StreamProxy> streamProxiesForSendMessage = new ArrayList<>();
        List<CommonGBChannel> channelListForOffline = new ArrayList<>();

        for (StreamProxy streamProxy : streamProxies) {
            if (streamProxy.getGbId() > 0 && "ON".equalsIgnoreCase(streamProxy.getGbStatus())) {
                channelListForOffline.add(streamProxy.buildCommonGBChannel());
            }
            if (streamProxy.getGbId() == 0 && streamProxy.isEnableRemoveNoneReader()) {
                streamProxiesForRemove.add(streamProxy);
            }
            if ("ON".equalsIgnoreCase(streamProxy.getGbStatus())) {
                streamProxiesForSendMessage.add(streamProxy);
            }
        }
        if (!streamProxiesForRemove.isEmpty()) {
            // 移除开启了无人观看自动移除的流
            streamProxyMapper.deleteByList(streamProxiesForRemove);
        }
        if (!streamProxiesForRemove.isEmpty()) {
            // 修改国标关联的国标通道的状态
            gbChannelService.offline(channelListForOffline);
        }
        if (!streamProxiesForSendMessage.isEmpty()) {
            for (StreamProxy streamProxy : streamProxiesForSendMessage) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", streamProxy.getApp());
                jsonObject.put("stream", streamProxy.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", mediaServer);
                redisCatchStorage.sendStreamChangeMsg("pull", jsonObject);
            }
        }
    }

    @Transactional
    public void streamChangeHandler(String app, String stream, String mediaServerId, boolean status) {
        // 状态变化时推送到国标上级
        StreamProxy streamProxy = streamProxyMapper.selectOneByAppAndStream(app, stream);
        if (streamProxy == null) {
            return;
        }
        streamProxy.setPulling(status);
        streamProxy.setMediaServerId(mediaServerId);
        streamProxy.setUpdateTime(DateUtil.getNow());
        streamProxyMapper.addStream(streamProxy);

        streamProxy.setGbStatus(status ? "ON" : "OFF");
        if (streamProxy.getGbId() > 0) {
            if (status) {
                gbChannelService.online(streamProxy.buildCommonGBChannel());
            } else {
                gbChannelService.offline(streamProxy.buildCommonGBChannel());
            }
        }
    }

    @Override
    public ResourceBaseInfo getOverview() {

        int total = streamProxyMapper.getAllCount();
        int online = streamProxyMapper.getOnline();

        return new ResourceBaseInfo(total, online);
    }

    @Override
    public StreamProxy getStreamProxy(int id) {
        return streamProxyMapper.select(id);
    }

}
