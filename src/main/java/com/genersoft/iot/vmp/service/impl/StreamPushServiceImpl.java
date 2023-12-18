package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.genersoft.iot.vmp.common.BatchLimit;
import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OriginType;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.service.bean.CommonGbChannelType;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.Group;
import com.genersoft.iot.vmp.service.bean.StreamPushItemFromRedis;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StreamPushServiceImpl implements IStreamPushService {

    private final static Logger logger = LoggerFactory.getLogger(StreamPushServiceImpl.class);

    @Autowired
    private StreamPushMapper streamPushMapper;

    @Autowired
    private StreamProxyMapper streamProxyMapper;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ICommonGbChannelService commonGbChannelService;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    private TransactionDefinition transactionDefinition;

    @Autowired
    private IGroupService groupService;



    @Override
    public List<StreamPush> handleJSON(String jsonData, MediaServerItem mediaServerItem) {
        if (jsonData == null) {
            return null;
        }

        Map<String, StreamPush> result = new HashMap<>();

        List<OnStreamChangedHookParam> onStreamChangedHookParams = JSON.parseObject(jsonData, new TypeReference<List<OnStreamChangedHookParam>>() {});
        for (OnStreamChangedHookParam item : onStreamChangedHookParams) {

            // 不保存国标推理以及拉流代理的流
            if (item.getOriginType() == OriginType.RTSP_PUSH.ordinal()
                    || item.getOriginType() == OriginType.RTMP_PUSH.ordinal()
                    || item.getOriginType() == OriginType.RTC_PUSH.ordinal() ) {
                String key = item.getApp() + "_" + item.getStream();
                StreamPush streamPushItem = result.get(key);
                if (streamPushItem == null) {
                    streamPushItem = transform(item);
                    result.put(key, streamPushItem);
                }
            }
        }

        return new ArrayList<>(result.values());
    }
    @Override
    public StreamPush transform(OnStreamChangedHookParam item) {
        StreamPush streamPushItem = new StreamPush();
        streamPushItem.setApp(item.getApp());
        streamPushItem.setMediaServerId(item.getMediaServerId());
        streamPushItem.setStream(item.getStream());
        streamPushItem.setAliveSecond(item.getAliveSecond());
        streamPushItem.setTotalReaderCount(item.getTotalReaderCount());
        streamPushItem.setCreateTime(DateUtil.getNow());
        streamPushItem.setAliveSecond(item.getAliveSecond());
        streamPushItem.setVhost(item.getVhost());
        streamPushItem.setServerId(item.getSeverId());
        return streamPushItem;
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
    public StreamPush getPush(String app, String streamId) {
        return streamPushMapper.selectOne(app, streamId);
    }

    @Override
    public boolean stop(String app, String streamId) {
        logger.info("[停止推流 ] {}/{}", app, streamId);

        StreamPush streamPushItem = streamPushMapper.selectOne(app, streamId);
        if (streamPushItem == null) {
            logger.info("[停止推流] 不存在 {}/{} ", app, streamId);
            return false;
        }
        if (streamPushItem.getCommonGbChannelId() == 0) {
            streamPushMapper.del(app, streamId);
        }
        MediaServerItem mediaServerItem = mediaServerService.getOne(streamPushItem.getMediaServerId());
        zlmresTfulUtils.closeStreams(mediaServerItem,app, streamId);
        return true;
    }

    @Override
    public void zlmServerOnline(String mediaServerId) {
        // 同步zlm推流信息
        MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
        if (mediaServerItem == null) {
            return;
        }
        // 数据库记录
        List<StreamPush> pushList = getPushList(mediaServerId);
        Map<String, StreamPush> pushItemMap = new HashMap<>();
        // redis记录
        List<OnStreamChangedHookParam> onStreamChangedHookParams = redisCatchStorage.getStreams(mediaServerId, "PUSH");
        Map<String, OnStreamChangedHookParam> streamInfoPushItemMap = new HashMap<>();
        if (!pushList.isEmpty()) {
            for (StreamPush streamPushItem : pushList) {
                if (streamPushItem.getCommonGbChannelId() > 0) {
                    pushItemMap.put(streamPushItem.getApp() + streamPushItem.getStream(), streamPushItem);
                }
            }
        }
        if (!onStreamChangedHookParams.isEmpty()) {
            for (OnStreamChangedHookParam onStreamChangedHookParam : onStreamChangedHookParams) {
                streamInfoPushItemMap.put(onStreamChangedHookParam.getApp() + onStreamChangedHookParam.getStream(), onStreamChangedHookParam);
            }
        }
        // 获取所有推流鉴权信息，清理过期的
        List<StreamAuthorityInfo> allStreamAuthorityInfo = redisCatchStorage.getAllStreamAuthorityInfo();
        Map<String, StreamAuthorityInfo> streamAuthorityInfoInfoMap = new HashMap<>();
        for (StreamAuthorityInfo streamAuthorityInfo : allStreamAuthorityInfo) {
            streamAuthorityInfoInfoMap.put(streamAuthorityInfo.getApp() + streamAuthorityInfo.getStream(), streamAuthorityInfo);
        }
        zlmresTfulUtils.getMediaList(mediaServerItem, (mediaList ->{
            if (mediaList == null) {
                return;
            }
            String dataStr = mediaList.getString("data");

            Integer code = mediaList.getInteger("code");
            List<StreamPush> streamPushItems = null;
            if (code == 0 ) {
                if (dataStr != null) {
                    streamPushItems = handleJSON(dataStr, mediaServerItem);
                }
            }

            if (streamPushItems != null) {
                for (StreamPush streamPushItem : streamPushItems) {
                    pushItemMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
                    streamInfoPushItemMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
                    streamAuthorityInfoInfoMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
                }
            }
            List<StreamPush> offlinePushItems = new ArrayList<>(pushItemMap.values());
            if (!offlinePushItems.isEmpty()) {
                String type = "PUSH";
                if (offlinePushItems.size() > BatchLimit.count) {
                    for (int i = 0; i < offlinePushItems.size(); i += BatchLimit.count) {
                        int toIndex = i + BatchLimit.count;
                        if (i + BatchLimit.count > offlinePushItems.size()) {
                            toIndex = offlinePushItems.size();
                        }
                        List<StreamPush> streamPushItemsSub = offlinePushItems.subList(i, toIndex);
                        streamPushMapper.delAll(streamPushItemsSub);
                    }
                }else {
                    streamPushMapper.delAll(offlinePushItems);
                }

            }
            Collection<OnStreamChangedHookParam> offlineOnStreamChangedHookParamList = streamInfoPushItemMap.values();
            if (!offlineOnStreamChangedHookParamList.isEmpty()) {
                String type = "PUSH";
                for (OnStreamChangedHookParam offlineOnStreamChangedHookParam : offlineOnStreamChangedHookParamList) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("serverId", userSetting.getServerId());
                    jsonObject.put("app", offlineOnStreamChangedHookParam.getApp());
                    jsonObject.put("stream", offlineOnStreamChangedHookParam.getStream());
                    jsonObject.put("register", false);
                    jsonObject.put("mediaServerId", mediaServerId);
                    redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
                    // 移除redis内流的信息
                    redisCatchStorage.removeStream(mediaServerItem.getId(), "PUSH", offlineOnStreamChangedHookParam.getApp(), offlineOnStreamChangedHookParam.getStream());
                }
            }

            Collection<StreamAuthorityInfo> streamAuthorityInfos = streamAuthorityInfoInfoMap.values();
            if (!streamAuthorityInfos.isEmpty()) {
                for (StreamAuthorityInfo streamAuthorityInfo : streamAuthorityInfos) {
                    // 移除redis内流的信息
                    redisCatchStorage.removeStreamAuthorityInfo(streamAuthorityInfo.getApp(), streamAuthorityInfo.getStream());
                }
            }
        }));
    }

    @Override
    public void zlmServerOffline(String mediaServerId) {
        List<StreamPush> streamPushItems = streamPushMapper.selectAllByMediaServerIdWithOutGbID(mediaServerId);
        // 移除没有GBId的推流
        streamPushMapper.deleteWithoutGBId(mediaServerId);
        // 其他的流设置未启用
        streamPushMapper.updateStatusByMediaServerId(mediaServerId, false);
        streamProxyMapper.updateStatusByMediaServerId(mediaServerId, false);
        // 发送流停止消息
        String type = "PUSH";
        // 发送redis消息
        List<OnStreamChangedHookParam> streamInfoList = redisCatchStorage.getStreams(mediaServerId, type);
        if (!streamInfoList.isEmpty()) {
            for (OnStreamChangedHookParam onStreamChangedHookParam : streamInfoList) {
                // 移除redis内流的信息
                redisCatchStorage.removeStream(mediaServerId, type, onStreamChangedHookParam.getApp(), onStreamChangedHookParam.getStream());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", onStreamChangedHookParam.getApp());
                jsonObject.put("stream", onStreamChangedHookParam.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", mediaServerId);
                redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    @Transactional
    public void batchAdd(List<StreamPush> streamPushItems) {
        // 把存在国标Id的写入同步资源库
        List<CommonGbChannel> commonGbChannelList = new ArrayList<>();
        List<StreamPush> streamPushListForChannel = new ArrayList<>();
        List<StreamPush> streamPushListWithoutChannel = new ArrayList<>();
        // 将含有国标编号的推流数据与没有国标编号的进行拆分，拆分先将通用通道存储，得到每个通道的ID，赋值给推流信息后再将所有推流信息存入
        streamPushItems.stream().forEach(streamPush -> {
            if (!ObjectUtils.isEmpty(streamPush.getGbId())) {
                CommonGbChannel channel = CommonGbChannel.getInstance(streamPush);
                commonGbChannelList.add(channel);
                streamPushListForChannel.add(streamPush);
            }else {
                streamPushListWithoutChannel.add(streamPush);
            }
        });
        if (!commonGbChannelList.isEmpty()) {

            commonGbChannelService.batchAdd(commonGbChannelList);

            for (int i = 0; i < commonGbChannelList.size(); i++) {
                streamPushListForChannel.get(i).setCommonGbChannelId(commonGbChannelList.get(i).getCommonGbId());
            }
            streamPushListWithoutChannel.addAll(streamPushListForChannel);
        }
        if (streamPushListWithoutChannel.size() > BatchLimit.count) {
            for (int i = 0; i < streamPushListWithoutChannel.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > streamPushListWithoutChannel.size()) {
                    toIndex = streamPushListWithoutChannel.size();
                }
                List<StreamPush> streamPushItemsSub = streamPushListWithoutChannel.subList(i, toIndex);
                streamPushMapper.addAll(streamPushItemsSub);
            }
        }else {
            streamPushMapper.addAll(streamPushListWithoutChannel);
        }
    }

    @Override
    public void batchUpdate(List<StreamPush> streamPushItemForUpdate) {

    }

    @Override
    @Transactional
    public void batchAddForUpload(List<StreamPushExcelDto> streamPushExcelDtoList) {
        // 插入国标通用通道得到通道ID
        List<CommonGbChannel> commonGbChannelList = new ArrayList<>();
        List<StreamPush> streamPushListForChannel = new ArrayList<>();
        List<StreamPush> streamPushListWithoutChannel = new ArrayList<>();
        Map<String, Group> groupMap = groupService.getAllGroupMap();
        streamPushExcelDtoList.stream().forEach(streamPushExcelDto -> {
            StreamPush streamPush = StreamPush.getInstance(streamPushExcelDto);

            if (!ObjectUtils.isEmpty(streamPushExcelDto.getGbId().trim())) {
                CommonGbChannel commonGbChannel = CommonGbChannel.getInstance(streamPush);
                if (!ObjectUtils.isEmpty(streamPushExcelDto.getCatalogId())
                        && groupMap.containsKey(streamPushExcelDto.getCatalogId())) {
                    commonGbChannel.setCommonGbBusinessGroupID(streamPushExcelDto.getCatalogId());
                }
                commonGbChannelList.add(commonGbChannel);
                streamPushListForChannel.add(streamPush);
            }else {
                streamPushListWithoutChannel.add(streamPush);

            }
        });
        commonGbChannelService.batchAdd(commonGbChannelList);
        for (int i = 0; i < commonGbChannelList.size(); i++) {
            streamPushListForChannel.get(i).setCommonGbChannelId(commonGbChannelList.get(i).getCommonGbId());
        }
        streamPushListWithoutChannel.addAll(streamPushListForChannel);
        if (streamPushListWithoutChannel.size() > BatchLimit.count) {
            for (int i = 0; i < streamPushListWithoutChannel.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > streamPushListWithoutChannel.size()) {
                    toIndex = streamPushListWithoutChannel.size();
                }
                List<StreamPush> streamPushItemsSub = streamPushListWithoutChannel.subList(i, toIndex);
                streamPushMapper.addAll(streamPushItemsSub);
            }
        }else {
            streamPushMapper.addAll(streamPushListWithoutChannel);
        }
    }

    @Override
    public boolean batchStop(List<GbStream> gbStreams) {
        if (gbStreams == null || gbStreams.size() == 0) {
            return false;
        }
        int delStream = streamPushMapper.delAllForGbStream(gbStreams);
        if (delStream > 0) {
            for (GbStream gbStream : gbStreams) {
                MediaServerItem mediaServerItem = mediaServerService.getOne(gbStream.getMediaServerId());
                zlmresTfulUtils.closeStreams(mediaServerItem, gbStream.getApp(), gbStream.getStream());
            }
        }
        return true;
    }



    @Override
    public void allStreamOffline() {
        List<Integer> onlinePushers = streamPushMapper.getOnlinePusherForGb();
        streamPushMapper.setAllStreamOffline();
        if (!onlinePushers.isEmpty()) {
            commonGbChannelService.offlineForList(onlinePushers);
        }

    }

    @Override
    public void offline(List<StreamPushItemFromRedis> offlineStreams) {

        List<StreamPush> streamPushList = streamPushMapper.getListIn(offlineStreams);
        List<Integer> commonChannelIdList = new ArrayList<>();
        streamPushList.stream().forEach(streamPush -> {
            commonChannelIdList.add(streamPush.getCommonGbChannelId());
        });

        // 更新部分设备离线
        streamPushMapper.offline(streamPushList);
        if (!commonChannelIdList.isEmpty()) {
            commonGbChannelService.offlineForList(commonChannelIdList);
        }

    }

    @Override
    public void online(List<StreamPushItemFromRedis> onlineStreams) {
        List<StreamPush> streamPushList = streamPushMapper.getListIn(onlineStreams);
        List<Integer> commonChannelIdList = new ArrayList<>();
        streamPushList.stream().forEach(streamPush -> {
            commonChannelIdList.add(streamPush.getCommonGbChannelId());
        });

        // 更新部分设备离线
        streamPushMapper.offline(streamPushList);
        if (!commonChannelIdList.isEmpty()) {
            commonGbChannelService.onlineForList(commonChannelIdList);
        }
    }

    @Override
    @Transactional
    public boolean add(StreamPush stream) {
        String now = DateUtil.getNow();
        CommonGbChannel commonGbChannel = null;
        if (!ObjectUtils.isEmpty(stream.getGbId())) {
            commonGbChannel = CommonGbChannel.getInstance(stream);
            commonGbChannelService.add(commonGbChannel);
            if (commonGbChannel.getCommonGbId() > 0) {
                stream.setCommonGbChannelId(commonGbChannel.getCommonGbId());
            }
        }
        stream.setUpdateTime(now);
        stream.setCreateTime(now);
        stream.setServerId(userSetting.getServerId());
        return streamPushMapper.add(stream) > 1;
    }

    @Override
    @Transactional
    public void update(StreamPush streamPush) {
        assert streamPush.getId() > 0;
        StreamPush streamPushIDb = streamPushMapper.query(streamPush.getId());
        assert streamPushIDb != null;
        if (streamPushIDb.getCommonGbChannelId() > 0 && streamPush.getCommonGbChannelId() == 0) {
            commonGbChannelService.deleteById(streamPushIDb.getCommonGbChannelId());
        }
        if (streamPushIDb.getCommonGbChannelId() == 0 && streamPush.getCommonGbChannelId() > 0) {
            CommonGbChannel commonGbChannel = CommonGbChannel.getInstance(streamPush);
            commonGbChannelService.add(commonGbChannel);
        }
        streamPush.setUpdateTime(DateUtil.getNow());
        streamPushMapper.update(streamPush);
    }

    @Override
    public Map<String, StreamPush> getAllAppAndStream() {
        return streamPushMapper.getAllAppAndStream();
    }

    @Override
    public ResourceBaseInfo getOverview() {
        int total = streamPushMapper.getAllCount();
        int online = streamPushMapper.getAllOnline(userSetting.isUsePushingAsStatus());

        return new ResourceBaseInfo(total, online);
    }

    @Override
    public void updateStreamGPS(List<GPSMsgInfo> gpsMsgInfoList) {
        streamPushMapper.updateStreamGPS(gpsMsgInfoList);
    }
}
