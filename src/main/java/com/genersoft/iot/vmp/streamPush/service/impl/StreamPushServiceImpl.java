package com.genersoft.iot.vmp.streamPush.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOfflineEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOnlineEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OriginType;
import com.genersoft.iot.vmp.service.bean.StreamPushItemFromRedis;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.streamPush.dao.StreamPushMapper;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
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
import org.springframework.util.ObjectUtils;

import java.util.*;

@Service
@Slf4j
@DS("master")
public class StreamPushServiceImpl implements IStreamPushService {

    @Autowired
    private StreamPushMapper streamPushMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    private IGbChannelService gbChannelService;

    /**
     * 流到来的处理
     */
    @Async("taskExecutor")
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaArrivalEvent event) {
        MediaInfo mediaInfo = event.getMediaInfo();
        if (mediaInfo == null) {
            return;
        }
        if (mediaInfo.getOriginType() != OriginType.RTMP_PUSH.ordinal()
                && mediaInfo.getOriginType() != OriginType.RTSP_PUSH.ordinal()
                && mediaInfo.getOriginType() != OriginType.RTC_PUSH.ordinal()) {
            return;
        }

        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(event.getApp(), event.getStream());
        if (streamAuthorityInfo == null) {
            streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(event);
        } else {
            streamAuthorityInfo.setOriginType(mediaInfo.getOriginType());
        }
        redisCatchStorage.updateStreamAuthorityInfo(event.getApp(), event.getStream(), streamAuthorityInfo);

        StreamPush streamPushInDb = getPush(event.getApp(), event.getStream());
        if (streamPushInDb == null) {
            StreamPush streamPush = StreamPush.getInstance(event, userSetting.getServerId());
            streamPush.setPushIng(true);
            streamPush.setUpdateTime(DateUtil.getNow());
            streamPush.setPushTime(DateUtil.getNow());
            streamPush.setSelf(true);
            add(streamPush);
        }else {
            updatePushStatus(streamPushInDb.getId(), true);
        }
        // 冗余数据，自己系统中自用
        if (!"broadcast".equals(event.getApp()) && !"talk".equals(event.getApp())) {
            StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(
                    event.getMediaServer(), event.getApp(), event.getStream(), event.getMediaInfo(), event.getCallId());
            event.getHookParam().setStreamInfo(new StreamContent(streamInfo));
            redisCatchStorage.addPushListItem(event.getApp(), event.getStream(), event);
        }

        // 发送流变化redis消息
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("serverId", userSetting.getServerId());
        jsonObject.put("app", event.getApp());
        jsonObject.put("stream", event.getStream());
        jsonObject.put("register", true);
        jsonObject.put("mediaServerId", event.getMediaServer().getId());
        redisCatchStorage.sendStreamChangeMsg(OriginType.values()[event.getMediaInfo().getOriginType()].getType(), jsonObject);
    }

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaDepartureEvent event) {

        // 兼容流注销时类型从redis记录获取
        MediaInfo mediaInfo = redisCatchStorage.getStreamInfo(
                event.getApp(), event.getStream(), event.getMediaServer().getId());
        if (mediaInfo != null) {
            String type = OriginType.values()[mediaInfo.getOriginType()].getType();
            redisCatchStorage.removeStream(event.getMediaServer().getId(), type, event.getApp(), event.getStream());
            if ("PUSH".equalsIgnoreCase(type)) {
                // 冗余数据，自己系统中自用
                redisCatchStorage.removePushListItem(event.getApp(), event.getStream(), event.getMediaServer().getId());
            }
            if (type != null) {
                // 发送流变化redis消息
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", event.getApp());
                jsonObject.put("stream", event.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", event.getMediaServer().getId());
                redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
            }
        }
        StreamPush push = getPush(event.getApp(), event.getStream());
        if (push == null) {
            return;
        }
        push.setPushIng(false);
        if (push.getGbDeviceId() != null) {
            if (userSetting.isUsePushingAsStatus()) {
                push.setGbStatus("OFF");
                updateStatus(push);
//                streamPushMapper.updatePushStatus(event.getApp(), event.getStream(), false);
//                eventPublisher.catalogEventPublishForStream(null, gbStream, CatalogEvent.OFF);
            }
        }else {
            deleteByAppAndStream(event.getApp(), event.getStream());
        }
    }

    /**
     * 流媒体节点上线
     */
    @Async("taskExecutor")
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaServerOnlineEvent event) {
        zlmServerOnline(event.getMediaServerId());
    }

    /**
     * 流媒体节点离线
     */
    @Async("taskExecutor")
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaServerOfflineEvent event) {
        zlmServerOffline(event.getMediaServerId());
    }

    @Override
    public PageInfo<StreamPush> getPushList(Integer page, Integer count, String query, Boolean pushing, String mediaServerId) {
        PageHelper.startPage(page, count);
        List<StreamPush> all = streamPushMapper.selectAllForList(query, pushing, mediaServerId);
        return new PageInfo<>(all);
    }

    @Override
    public List<StreamPush> getPushList(String mediaServerId) {
        return streamPushMapper.selectAllByMediaServerIdWithOutGbID(mediaServerId);
    }


    @Override
    public StreamPush getPush(String app, String stream) {
        return streamPushMapper.selectByAppAndStream(app, stream);
    }

    @Override
    @Transactional
    public boolean add(StreamPush stream) {
        log.info("[添加推流] app: {}, stream: {}, 国标编号: {}", stream.getApp(), stream.getStream(), stream.getGbDeviceId());
        StreamPush streamPushInDb = streamPushMapper.selectByAppAndStream(stream.getApp(), stream.getStream());
        if (streamPushInDb != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "应用名+流ID已存在");
        }
        stream.setUpdateTime(DateUtil.getNow());
        stream.setCreateTime(DateUtil.getNow());
        int addResult = streamPushMapper.add(stream);
        if (addResult <= 0) {
            return false;
        }
        if (ObjectUtils.isEmpty(stream.getGbDeviceId())) {
            return true;
        }
        CommonGBChannel channel = gbChannelService.queryByDeviceId(stream.getGbDeviceId());
        if (channel != null) {
            log.info("[添加推流]失败，国标编号已存在: {} app: {}, stream: {}, ", stream.getGbDeviceId(), stream.getApp(), stream.getStream());
        }
        int addChannelResult = gbChannelService.add(stream.buildCommonGBChannel());
        return addChannelResult > 0;
    }

    @Override
    @Transactional
    public void deleteByAppAndStream(String app, String stream) {
        log.info("[删除推流] app: {}, stream: {}, ", app, stream);
        StreamPush streamPush = streamPushMapper.selectByAppAndStream(app, stream);
        if (streamPush == null) {
            log.info("[删除推流]失败， 不存在 app: {}, stream: {}, ", app, stream);
            return;
        }
        if (streamPush.isPushIng()) {
            stop(streamPush);
        }
        if (streamPush.getGbId() > 0) {
            gbChannelService.delete(streamPush.getGbId());
        }
    }
    @Override
    @Transactional
    public boolean update(StreamPush streamPush) {
        log.info("[更新推流]：id: {}, app: {}, stream: {}, ", streamPush.getId(), streamPush.getApp(), streamPush.getStream());
        assert streamPush.getId() != null;
        StreamPush streamPushInDb = streamPushMapper.select(streamPush.getId());
        if (!streamPushInDb.getApp().equals(streamPush.getApp()) || !streamPushInDb.getStream().equals(streamPush.getStream())) {
            // app或者stream变化
            StreamPush streamPushInDbForAppAndStream = streamPushMapper.selectByAppAndStream(streamPush.getApp(), streamPush.getStream());
            if (streamPushInDbForAppAndStream != null && !streamPushInDbForAppAndStream.getId().equals(streamPush.getId())) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "应用名+流ID已存在");
            }
        }
        streamPush.setUpdateTime(DateUtil.getNow());
        streamPushMapper.update(streamPush);
        if (streamPush.getGbId() > 0) {
            gbChannelService.update(streamPush.buildCommonGBChannel());
        }
        return true;
    }


    @Override
    @Transactional
    public boolean stop(StreamPush streamPush) {
        log.info("[主动停止推流] id: {}, app: {}, stream: {}, ", streamPush.getId(), streamPush.getApp(), streamPush.getStream());
        MediaServer mediaServer = null;
        if (streamPush.getMediaServerId() == null) {
            log.info("[主动停止推流]未找到使用MediaServer，开始自动检索 id: {}, app: {}, stream: {}, ", streamPush.getId(), streamPush.getApp(), streamPush.getStream());
            mediaServer = mediaServerService.getMediaServerByAppAndStream(streamPush.getApp(), streamPush.getStream());
            if (mediaServer != null) {
                log.info("[主动停止推流] 检索到MediaServer为{}， id: {}, app: {}, stream: {}, ", mediaServer.getId(), streamPush.getId(), streamPush.getApp(), streamPush.getStream());
            }else {
                log.info("[主动停止推流]未找到使用MediaServer id: {}, app: {}, stream: {}, ", streamPush.getId(), streamPush.getApp(), streamPush.getStream());
            }
        }else {
            mediaServer = mediaServerService.getOne(streamPush.getMediaServerId());
            if (mediaServer == null) {
                log.info("[主动停止推流]未找到使用的MediaServer： {}，开始自动检索 id: {}, app: {}, stream: {}, ",streamPush.getMediaServerId(),  streamPush.getId(), streamPush.getApp(), streamPush.getStream());
                mediaServer = mediaServerService.getMediaServerByAppAndStream(streamPush.getApp(), streamPush.getStream());
                if (mediaServer != null) {
                    log.info("[主动停止推流] 检索到MediaServer为{}， id: {}, app: {}, stream: {}, ", mediaServer.getId(), streamPush.getId(), streamPush.getApp(), streamPush.getStream());
                }else {
                    log.info("[主动停止推流]未找到使用MediaServer id: {}, app: {}, stream: {}, ", streamPush.getId(), streamPush.getApp(), streamPush.getStream());
                }
            }
        }
        if (mediaServer != null) {
            mediaServerService.closeStreams(mediaServer, streamPush.getApp(), streamPush.getStream());
        }
        streamPush.setPushIng(false);
        if (userSetting.isUsePushingAsStatus()) {
            gbChannelService.offline(streamPush.buildCommonGBChannel());
        }
        redisCatchStorage.deleteSendRTPServer(null, streamPush.getGbDeviceId(), null, streamPush.getStream());
        mediaServerService.stopSendRtp(mediaServer, streamPush.getApp(), streamPush.getStream(), null);
        streamPush.setUpdateTime(DateUtil.getNow());
        streamPushMapper.update(streamPush);
        return true;
    }

    @Override
    @Transactional
    public boolean stopByAppAndStream(String app, String stream) {
        log.info("[主动停止推流] ： app: {}, stream: {}, ", app, stream);
        StreamPush streamPushItem = streamPushMapper.selectByAppAndStream(app, stream);
        if (streamPushItem != null) {
            stop(streamPushItem);
        }
        return true;
    }

    @Override
    @Transactional
    public void zlmServerOnline(String mediaServerId) {
        // 同步zlm推流信息
        MediaServer mediaServerItem = mediaServerService.getOne(mediaServerId);
        if (mediaServerItem == null) {
            return;
        }
        // 数据库记录
        List<StreamPush> pushList = getPushList(mediaServerId);
        Map<String, StreamPush> pushItemMap = new HashMap<>();
        // redis记录
        List<MediaInfo> mediaInfoList = redisCatchStorage.getStreams(mediaServerId, "PUSH");
        Map<String, MediaInfo> streamInfoPushItemMap = new HashMap<>();
        if (!pushList.isEmpty()) {
            for (StreamPush streamPushItem : pushList) {
                if (ObjectUtils.isEmpty(streamPushItem.getGbId())) {
                    pushItemMap.put(streamPushItem.getApp() + streamPushItem.getStream(), streamPushItem);
                }
            }
        }
        if (!mediaInfoList.isEmpty()) {
            for (MediaInfo mediaInfo : mediaInfoList) {
                streamInfoPushItemMap.put(mediaInfo.getApp() + mediaInfo.getStream(), mediaInfo);
            }
        }
        // 获取所有推流鉴权信息，清理过期的
        List<StreamAuthorityInfo> allStreamAuthorityInfo = redisCatchStorage.getAllStreamAuthorityInfo();
        Map<String, StreamAuthorityInfo> streamAuthorityInfoInfoMap = new HashMap<>();
        for (StreamAuthorityInfo streamAuthorityInfo : allStreamAuthorityInfo) {
            streamAuthorityInfoInfoMap.put(streamAuthorityInfo.getApp() + streamAuthorityInfo.getStream(), streamAuthorityInfo);
        }
        List<StreamInfo> mediaList = mediaServerService.getMediaList(mediaServerItem, null, null, null);
        if (mediaList == null) {
            return;
        }
        List<StreamPush> streamPushItems = handleJSON(mediaList);
        if (streamPushItems != null) {
            for (StreamPush streamPushItem : streamPushItems) {
                pushItemMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
                streamInfoPushItemMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
                streamAuthorityInfoInfoMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
            }
        }
        List<StreamPush> changedStreamPushList = new ArrayList<>(pushItemMap.values());
        if (!changedStreamPushList.isEmpty()) {
            for (StreamPush streamPush : changedStreamPushList) {
                stop(streamPush);
            }
        }


//        if (!changedStreamPushList.isEmpty()) {
//            String type = "PUSH";
//            int runLimit = 300;
//            if (changedStreamPushList.size() > runLimit) {
//                for (int i = 0; i < changedStreamPushList.size(); i += runLimit) {
//                    int toIndex = i + runLimit;
//                    if (i + runLimit > changedStreamPushList.size()) {
//                        toIndex = changedStreamPushList.size();
//                    }
//                    List<StreamPush> streamPushItemsSub = changedStreamPushList.subList(i, toIndex);
//                    streamPushMapper.delAll(streamPushItemsSub);
//                }
//            }else {
//                streamPushMapper.delAll(changedStreamPushList);
//            }
//
//        }
        Collection<MediaInfo> mediaInfos = streamInfoPushItemMap.values();
        if (!mediaInfos.isEmpty()) {
            String type = "PUSH";
            for (MediaInfo mediaInfo : mediaInfos) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", mediaInfo.getApp());
                jsonObject.put("stream", mediaInfo.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", mediaServerId);
                redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
                // 移除redis内流的信息
                redisCatchStorage.removeStream(mediaServerItem.getId(), "PUSH", mediaInfo.getApp(), mediaInfo.getStream());
                // 冗余数据，自己系统中自用
                redisCatchStorage.removePushListItem(mediaInfo.getApp(), mediaInfo.getStream(), mediaServerItem.getId());
            }
        }

        Collection<StreamAuthorityInfo> streamAuthorityInfos = streamAuthorityInfoInfoMap.values();
        if (!streamAuthorityInfos.isEmpty()) {
            for (StreamAuthorityInfo streamAuthorityInfo : streamAuthorityInfos) {
                // 移除redis内流的信息
                redisCatchStorage.removeStreamAuthorityInfo(streamAuthorityInfo.getApp(), streamAuthorityInfo.getStream());
            }
        }
    }

    @Override
    @Transactional
    public void zlmServerOffline(String mediaServerId) {
        List<StreamPush> streamPushItems = streamPushMapper.selectAllByMediaServerId(mediaServerId);
        if (!streamPushItems.isEmpty()) {
            for (StreamPush streamPushItem : streamPushItems) {
                stop(streamPushItem);
            }
        }
//        // 移除没有GBId的推流
//        streamPushMapper.deleteWithoutGBId(mediaServerId);
//        // 其他的流设置未启用
//        streamPushMapper.updateStatusByMediaServerId(mediaServerId, false);
//        streamProxyMapper.updateStatusByMediaServerId(mediaServerId, false);
        // 发送流停止消息
        String type = "PUSH";
        // 发送redis消息
        List<MediaInfo> mediaInfoList = redisCatchStorage.getStreams(mediaServerId, type);
        if (!mediaInfoList.isEmpty()) {
            for (MediaInfo mediaInfo : mediaInfoList) {
                // 移除redis内流的信息
                redisCatchStorage.removeStream(mediaServerId, type, mediaInfo.getApp(), mediaInfo.getStream());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", mediaInfo.getApp());
                jsonObject.put("stream", mediaInfo.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", mediaServerId);
                redisCatchStorage.sendStreamChangeMsg(type, jsonObject);

                // 冗余数据，自己系统中自用
                redisCatchStorage.removePushListItem(mediaInfo.getApp(), mediaInfo.getStream(), mediaServerId);
            }
        }
    }

    @Override
    @Transactional
    public void batchAdd(List<StreamPush> streamPushItems) {
        streamPushMapper.addAll(streamPushItems);
        List<CommonGBChannel> commonGBChannels = new ArrayList<>();
        for (StreamPush streamPush : streamPushItems) {
            if (!ObjectUtils.isEmpty(streamPush.getGbDeviceId())) {
                commonGBChannels.add(streamPush.buildCommonGBChannel());
            }
        }
        gbChannelService.batchAdd(commonGBChannels);
    }

    @Override
    public void allOffline() {
        List<StreamPush> streamPushList = streamPushMapper.selectAllForList(null, null, null);
        if (streamPushList.isEmpty()) {
            return;
        }
        List<CommonGBChannel> commonGBChannelList = new ArrayList<>();
        for (StreamPush streamPush : streamPushList) {
            commonGBChannelList.add(streamPush.buildCommonGBChannel());
        }
        gbChannelService.offline(commonGBChannelList);
    }

    @Override
    public void offline(List<StreamPushItemFromRedis> offlineStreams) {
        // 更新部分设备离线
        List<StreamPush> streamPushList = streamPushMapper.getListFromRedis(offlineStreams);
        List<CommonGBChannel> commonGBChannelList = new ArrayList<>();
        for (StreamPush onlinePusher : streamPushList) {
            commonGBChannelList.add(onlinePusher.buildCommonGBChannel());
        }
        gbChannelService.offline(commonGBChannelList);
    }

    @Override
    public void online(List<StreamPushItemFromRedis> onlineStreams) {
        // 更新部分设备上线streamPushService
        List<StreamPush> streamPushList = streamPushMapper.getListFromRedis(onlineStreams);
        List<CommonGBChannel> commonGBChannelList = new ArrayList<>();
        for (StreamPush onlinePusher : streamPushList) {
            commonGBChannelList.add(onlinePusher.buildCommonGBChannel());
        }
        gbChannelService.online(commonGBChannelList);
    }

    @Override
    public List<String> getAllAppAndStream() {

        return streamPushMapper.getAllAppAndStream();
    }

    @Override
    public ResourceBaseInfo getOverview() {
        int total = streamPushMapper.getAllCount();
        int online = streamPushMapper.getAllPushing(userSetting.isUsePushingAsStatus());

        return new ResourceBaseInfo(total, online);
    }

    @Override
    public Map<String, StreamPush> getAllAppAndStreamMap() {
        return streamPushMapper.getAllAppAndStreamMap();
    }

    @Override
    public Map<String, StreamPush> getAllGBId() {
        return streamPushMapper.getAllGBId();
    }

    @Override
    public void updateStatus(StreamPush push) {
        if (push.getGbDeviceId() != null) {
            return;
        }
        if ("ON".equalsIgnoreCase(push.getGbStatus())) {
            gbChannelService.online(push.buildCommonGBChannel());
        }else {
            gbChannelService.offline(push.buildCommonGBChannel());
        }
    }



    @Override
    public void updatePushStatus(Integer streamPushId, boolean pushIng) {
        StreamPush streamPushInDb = streamPushMapper.select(streamPushId);
        streamPushInDb.setPushIng(pushIng);
        if (userSetting.isUsePushingAsStatus()) {
            streamPushInDb.setGbStatus(pushIng?"ON":"OFF");
        }
        streamPushInDb.setPushTime(DateUtil.getNow());
        updateStatus(streamPushInDb);
    }

    private List<StreamPush> handleJSON(List<StreamInfo> streamInfoList) {
        if (streamInfoList == null || streamInfoList.isEmpty()) {
            return null;
        }
        Map<String, StreamPush> result = new HashMap<>();
        for (StreamInfo streamInfo : streamInfoList) {
            // 不保存国标推理以及拉流代理的流
            if (streamInfo.getOriginType() == OriginType.RTSP_PUSH.ordinal()
                    || streamInfo.getOriginType() == OriginType.RTMP_PUSH.ordinal()
                    || streamInfo.getOriginType() == OriginType.RTC_PUSH.ordinal() ) {
                String key = streamInfo.getApp() + "_" + streamInfo.getStream();
                StreamPush streamPushItem = result.get(key);
                if (streamPushItem == null) {
                    streamPushItem = streamPushItem.getInstance(streamInfo);
                    result.put(key, streamPushItem);
                }
            }
        }
        return new ArrayList<>(result.values());
    }

    @Override
    public void batchUpdate(List<StreamPush> streamPushItemForUpdate) {

    }

    @Override
    @Transactional
    public int delete(int id) {
        StreamPush streamPush = streamPushMapper.select(id);
        if (streamPush == null) {
            return 0;
        }
        if(streamPush.isPushIng()) {
            MediaServer mediaServer = mediaServerService.getOne(streamPush.getMediaServerId());
            mediaServerService.closeStreams(mediaServer, streamPush.getApp(), streamPush.getStream());
        }
        if (streamPush.getGbDeviceId() != null) {
            gbChannelService.delete(streamPush.getGbId());
        }
        return streamPushMapper.del(id);
    }

    @Override
    @Transactional
    public void batchRemove(Set<Integer> ids) {
        List<StreamPush> streamPushList = streamPushMapper.selectInSet(ids);
        if (streamPushList.isEmpty()) {
            return;
        }
        List<CommonGBChannel> commonGBChannelList = new ArrayList<>();
        streamPushList.stream().forEach(streamPush -> {
            if (streamPush.getGbDeviceId() != null) {
                commonGBChannelList.add(streamPush.buildCommonGBChannel());
            }
        });
        streamPushMapper.batchDel(streamPushList);
        gbChannelService.delete(commonGBChannelList);
    }
}
