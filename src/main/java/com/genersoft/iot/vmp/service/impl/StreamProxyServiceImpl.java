package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxy;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OriginType;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.*;

/**
 * 视频代理业务
 */
@Service
public class StreamProxyServiceImpl implements IStreamProxyService {

    private final static Logger logger = LoggerFactory.getLogger(StreamProxyServiceImpl.class);

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private StreamProxyMapper streamProxyMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ICommonGbChannelService commonGbChannelService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ZlmHttpHookSubscribe hookSubscribe;

    @Autowired
    private IStreamProxyPlayService streamProxyPlayService;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;


    @Override
    @Transactional
    public void save(StreamProxy param, GeneralCallback<StreamInfo> callback) {
        MediaServerItem mediaInfo;
        if (ObjectUtils.isEmpty(param.getMediaServerId()) || "auto".equals(param.getMediaServerId())){
            mediaInfo = mediaServerService.getMediaServerForMinimumLoad(null);
        }else {
            mediaInfo = mediaServerService.getOne(param.getMediaServerId());
        }
        if (mediaInfo == null) {
            logger.warn("保存代理未找到在线的ZLM...");
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "保存代理未找到在线的ZLM");
        }
        String dstUrl;
        if ("ffmpeg".equalsIgnoreCase(param.getType())) {
            JSONObject jsonObject = zlmresTfulUtils.getMediaServerConfig(mediaInfo);
            if (jsonObject.getInteger("code") != 0) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "获取流媒体配置失败");
            }
            JSONArray dataArray = jsonObject.getJSONArray("data");
            JSONObject mediaServerConfig = dataArray.getJSONObject(0);
            if (ObjectUtils.isEmpty(param.getFfmpegCmdKey())) {
                param.setFfmpegCmdKey("ffmpeg.cmd");
            }
            String ffmpegCmd = mediaServerConfig.getString(param.getFfmpegCmdKey());
            if (ffmpegCmd == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "ffmpeg拉流代理无法获取ffmpeg cmd");
            }
            String schema = getSchemaFromFFmpegCmd(ffmpegCmd);
            if (schema == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "ffmpeg拉流代理无法从ffmpeg cmd中获取到输出格式");
            }
            int port;
            String schemaForUri;
            if (schema.equalsIgnoreCase("rtsp")) {
                port = mediaInfo.getRtspPort();
                schemaForUri = schema;
            }else if (schema.equalsIgnoreCase("flv")) {
                port = mediaInfo.getRtmpPort();
                schemaForUri = schema;
            }else {
                port = mediaInfo.getRtmpPort();
                schemaForUri = schema;
            }

            dstUrl = String.format("%s://%s:%s/%s/%s", schemaForUri, "127.0.0.1", port, param.getApp(),
                    param.getStream());
        }else {
            dstUrl = String.format("rtsp://%s:%s/%s/%s", "127.0.0.1", mediaInfo.getRtspPort(), param.getApp(),
                    param.getStream());
        }
        param.setDstUrl(dstUrl);
        logger.info("[拉流代理] 输出地址为：{}", dstUrl);
        param.setMediaServerId(mediaInfo.getId());
        // 更新
        StreamProxy streamProxyInDb = streamProxyMapper.selectOne(param.getApp(), param.getStream());
        if (streamProxyInDb != null) {
            param.setId(streamProxyInDb.getId());
            updateProxyToDb(param);
        }else { // 新增
            addProxyToDb(param);
        }

        HookSubscribeForStreamChange hookSubscribeForStreamChange = HookSubscribeFactory.on_stream_changed(param.getApp(), param.getStream(), true, "rtsp", mediaInfo.getId());
        hookSubscribe.addSubscribe(hookSubscribeForStreamChange, (mediaServerItem, response) -> {
            StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(
                    mediaInfo, param.getApp(), param.getStream(), null, null);
            if (callback != null) {
                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
            }
        });
        streamProxyPlayService.startProxy(param, mediaInfo, (code, msg, data) -> {
            if (code != ErrorCode.SUCCESS.getCode()) {
                if (callback != null) {
                    callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
                }
                param.setPulling(true);
                streamProxyMapper.update(param);
            }else {
                if (callback != null) {
                    callback.run(code, msg, null);
                }
                param.setPulling(false);
                // 直接移除
                if (param.isEnableRemoveNoneReader()) {
                    delProxyFromDb(param);
                }else {
                    updateProxyToDb(param);
                }
            }
        });
    }

    @Override
    @Transactional
    public void add(StreamProxy param, GeneralCallback<StreamInfo> callback) {
        proxyParamHandler(param);
        StreamProxy streamProxyInDb = streamProxyMapper.selectOne(param.getApp(), param.getStream());
        if (streamProxyInDb != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "app/stream已经存在");
        }
        addProxyToDb(param);
        if (callback != null) {
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
        }
    }

    @Override
    @Transactional
    public void edit(StreamProxy param, GeneralCallback<StreamInfo> callback) {
        MediaServerItem mediaInfo;
        StreamProxy streamProxyInDb = streamProxyMapper.selectOneById(param.getId());
        if (streamProxyInDb == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "代理不存在");
        }
        if (ObjectUtils.isEmpty(param.getMediaServerId()) || "auto".equals(param.getMediaServerId())){
            mediaInfo = mediaServerService.getMediaServerForMinimumLoad(null);
        }else {
            mediaInfo = mediaServerService.getOne(param.getMediaServerId());
        }
        if (mediaInfo == null) {
            logger.warn("[编辑拉流代理] 未找到在线的ZLM...");
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "保存代理未找到可用的ZLM");
        }
        proxyParamHandler(param);
        param.setMediaServerId(mediaInfo.getId());
        updateProxyToDb(param);
        if (callback != null) {
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
        }
        taskExecutor.execute(()->{
            // 国标编号发生变化，修改通用通道国标变化，通用通道应发送删除再发送添加命令通知上级
            // 类型变化，启用->启用：需要重新拉起视频流， 启用->未启用： 停止旧的视频流， 未启用->启用：发起新的视频流
            // 流地址发生变化。停止旧的，拉起新的
            // ffmpeg类型下，目标流地址变化，停止旧的，拉起新的
            // 节点变化： 停止旧的，拉起新的
            // ffmpeg命令模板变化： 停止旧的，拉起新的
            boolean stopOldProxy = !streamProxyInDb.getType().equals(param.getType())
                    || !streamProxyInDb.getUrl().equals(param.getUrl())
                    || !streamProxyInDb.getMediaServerId().equals(param.getMediaServerId())
                    || (streamProxyInDb.getType().equals("ffmpeg") && (
                    streamProxyInDb.getDstUrl().equals(param.getDstUrl())
                            || streamProxyInDb.getFfmpegCmdKey().equals(param.getFfmpegCmdKey())
            ));

            // 如果是开启代理这是开启代理结束后的回调
            final GeneralCallback<StreamInfo> startProxyCallback = (code, msg, data) -> {
                if (code == ErrorCode.SUCCESS.getCode()) {
                    param.setPulling(true);
                } else {
                    param.setPulling(false);
                    if (param.isEnableRemoveNoneReader()) {
                        return;
                    }
                    param.setProxyError(msg);
                }
                updateProxyToDb(param);
            };
            if(stopOldProxy) {
                streamProxyPlayService.stopProxy(param, mediaInfo, (code, msg, data) -> {
                    if (param.isPulling()) {
                        streamProxyPlayService.startProxy(param, mediaInfo, callback);
                    }
                });
            }
        });
    }

    public void proxyParamHandler(StreamProxy param) {
        if ("ffmpeg".equalsIgnoreCase(param.getType())) {
            if (ObjectUtils.isEmpty(param.getDstUrl())) {
                logger.warn("[拉流代理参数处理] 未设置目标URL地址（DstUrl）");
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "未设置目标URL地址");
            }
            logger.info("[拉流代理参数处理] ffmpeg，源地址: {}, 目标地址为：{}", param.getUrl(), param.getDstUrl());
            if (ObjectUtils.isEmpty(param.getApp()) || ObjectUtils.isEmpty(param.getStream())) {
                try {
                    URL url = new URL(param.getDstUrl());
                    String path = url.getPath();
                    if (path.indexOf("/", 1) < 0) {
                        throw new ControllerException(ErrorCode.ERROR100.getCode(), "解析DstUrl失败, 至少两层路径");
                    }
                    String app = path.substring(1, path.indexOf("/", 2));
                    String stream = path.substring(path.indexOf("/", 2) + 1);
                    param.setApp(app);
                    param.setStream(stream);
                } catch (MalformedURLException e) {
                    throw new ControllerException(ErrorCode.ERROR100.getCode(), "解析DstUrl失败");
                }
            }
        }else {
            logger.info("[拉流代理参数处理] 方式：直接拉流，源地址: {}, app: {}, stream: {}", param.getUrl(), param.getApp(), param.getStream());
        }
    }

    private void addProxyToDb(StreamProxy param) {
        // 未启用的数据可以直接保存了
        if (!ObjectUtils.isEmpty(param.getGbId())) {
            CommonGbChannel commonGbChannel = CommonGbChannel.getInstance(param);
            if (commonGbChannelService.add(commonGbChannel) > 0) {
                param.setCommonGbChannelId(commonGbChannel.getCommonGbId());
            }else {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "添加通用通道失败，请检查是否国标编码重复");
            }
        }
        param.setUpdateTime(DateUtil.getNow());
        param.setCreateTime(DateUtil.getNow());
        int addStreamProxyResult = streamProxyMapper.add(param);
        if (addStreamProxyResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "添加拉流代理失败");
        }
    }

    @Transactional
    public void updateProxyToDb(StreamProxy param) {
        if (param.getId() <= 0) {
            logger.error("[更新代理存储到数据库] 错误， 缺少ID");
            return;
        }
        StreamProxy streamProxyInDb = streamProxyMapper.selectOneById(param.getId());
        if (streamProxyInDb == null) {
            logger.error("[更新代理存储到数据库] 错误，ID： {} 不在数据库中", param.getId());
            return;
        }
        if (!ObjectUtils.isEmpty(streamProxyInDb.getGbId())
                && ObjectUtils.isEmpty(param.getGbId())) {
            // 国标ID已经移除
            if (streamProxyInDb.getCommonGbChannelId() > 0) {
                commonGbChannelService.deleteById(streamProxyInDb.getCommonGbChannelId());
            }
        }else if (!ObjectUtils.isEmpty(param.getGbId())
                && ObjectUtils.isEmpty(streamProxyInDb.getGbId())) {
            CommonGbChannel commonGbChannel = CommonGbChannel.getInstance(param);
            // 国标ID已经添加
            if (commonGbChannelService.add(commonGbChannel) > 0) {
                param.setCommonGbChannelId(commonGbChannel.getCommonGbId());
            }
        }else {
            CommonGbChannel commonGbChannel = CommonGbChannel.getInstance(param);
            commonGbChannel.setCommonGbId(streamProxyInDb.getCommonGbChannelId());
            // 国标ID已经改变
            commonGbChannelService.update(commonGbChannel);
        }
        param.setUpdateTime(DateUtil.getNow());
        int addStreamProxyResult = streamProxyMapper.update(param);
        if (addStreamProxyResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "保存拉流代理失败");
        }
    }

    private String getSchemaFromFFmpegCmd(String ffmpegCmd) {
        ffmpegCmd = ffmpegCmd.replaceAll(" + ", " ");
        String[] paramArray = ffmpegCmd.split(" ");
        if (paramArray.length == 0) {
            return null;
        }
        for (int i = 0; i < paramArray.length; i++) {
            if (paramArray[i].equalsIgnoreCase("-f")) {
                if (i + 1 < paramArray.length - 1) {
                    return paramArray[i+1];
                }else {
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public PageInfo<StreamProxy> getAll(String query, Boolean online, String mediaServerId, Integer page, Integer count) {
        PageHelper.startPage(page, count);
        List<StreamProxy> all = streamProxyMapper.selectAll(query, online, mediaServerId);
        return new PageInfo<>(all);
    }

    private void delProxyFromDb(StreamProxy streamProxy) {
        StreamProxy streamProxyItem;
        if (streamProxy.getId() <= 0) {
            streamProxyItem = streamProxyMapper.selectOne(streamProxy.getApp(), streamProxy.getStream());
        }else {
            streamProxyItem = streamProxy;
        }
        if (streamProxyItem == null) {
            return;
        }
        if (streamProxyItem.getCommonGbChannelId() > 0) {
            commonGbChannelService.deleteById(streamProxyItem.getCommonGbChannelId());
        }
        streamProxyMapper.delById(streamProxyItem.getId());
        redisCatchStorage.removeStream(streamProxyItem.getMediaServerId(), "PULL",
                streamProxy.getApp(), streamProxy.getStream());
    }

    @Override
    public void removeProxy(int id) {
        StreamProxy streamProxy = streamProxyMapper.selectOneById(id);
        if (streamProxy == null) {
            return;
        }
        if (streamProxy.isPulling()) {
            String mediaServerId = streamProxy.getMediaServerId();
            MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
            if (mediaServerItem != null) {
                boolean ready = mediaService.isReady(mediaServerItem, streamProxy.getApp(), streamProxy.getStream());
                if (ready) {
                    streamProxyPlayService.stopProxy(streamProxy, mediaServerItem, (code, msg, data) -> {
                        if (code == ErrorCode.SUCCESS.getCode()) {
                            logger.info("[移除代理]： 代理： {}/{}, 从zlm移除成功", streamProxy.getApp(), streamProxy.getStream());
                        }else {
                            logger.info("[移除代理]： 代理： {}/{}, 从zlm移除失败", streamProxy.getApp(), streamProxy.getStream());
                        }
                    });
                }
            }
        }
        delProxyFromDb(streamProxy);
    }

    @Override
    @Transactional
    public void start(String app, String stream, GeneralCallback<StreamInfo> callback) {
        StreamProxy streamProxy = streamProxyMapper.selectOne(app, stream);
        if (streamProxy == null ){
            if (callback != null) {
                callback.run(ErrorCode.ERROR100.getCode(), "代理不存在", null);
            }
            return;
        }
        String mediaServerId = streamProxy.getMediaServerId();
        MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
        if (mediaServerItem == null) {
            if (callback != null) {
                callback.run(ErrorCode.ERROR100.getCode(), "使用的媒体节点不存在", null);
            }
            return;
        }
        streamProxyPlayService.startProxy(streamProxy, mediaServerItem, (code, msg, data) -> {
            if (code == ErrorCode.SUCCESS.getCode()) {
                streamProxy.setPulling(true);
            }else {
                streamProxy.setPulling(false);
            }
            streamProxy.setUpdateTime(DateUtil.getNow());
            updateProxyToDb(streamProxy);
            if (callback != null) {
                callback.run(code, msg, data);
            }
        });
    }

    @Override
    @Transactional
    public void stop(String app, String stream, GeneralCallback<StreamInfo> callback) {
        StreamProxy streamProxy = streamProxyMapper.selectOne(app, stream);
        if (streamProxy == null){
            if (callback != null) {
                callback.run(ErrorCode.ERROR100.getCode(), "代理不存在", null);
            }
            return;
        }
        String mediaServerId = streamProxy.getMediaServerId();
        MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
        if (mediaServerItem == null) {
            if (callback != null) {
                callback.run(ErrorCode.ERROR100.getCode(), "使用的媒体节点不存在", null);
            }
            return;
        }
        streamProxyPlayService.stopProxy(streamProxy, mediaServerItem, (code, msg, data) -> {
            streamProxy.setPulling(false);
            streamProxy.setUpdateTime(DateUtil.getNow());
            updateProxyToDb(streamProxy);
            if (callback != null) {
                callback.run(code, msg, data);
            }
        });
    }

    @Override
    public JSONObject getFFmpegCMDs(MediaServerItem mediaServerItem) {
        JSONObject result = new JSONObject();
        JSONObject mediaServerConfigResuly = zlmresTfulUtils.getMediaServerConfig(mediaServerItem);
        if (mediaServerConfigResuly != null && mediaServerConfigResuly.getInteger("code") == 0
                && !mediaServerConfigResuly.getJSONArray("data").isEmpty()){
            JSONObject mediaServerConfig = mediaServerConfigResuly.getJSONArray("data").getJSONObject(0);

            for (String key : mediaServerConfig.keySet()) {
                if (key.startsWith("ffmpeg.cmd")){
                    result.put(key, mediaServerConfig.getString(key));
                }
            }
        }
        return result;
    }


    @Override
    public StreamProxy getStreamProxyByAppAndStream(String app, String streamId) {
        return streamProxyMapper.selectOne(app, streamId);
    }

    @Override
    @Transactional
    public void zlmServerOnline(String mediaServerId) {
        // 移除开启了无人观看自动移除的流
        List<StreamProxy> streamProxyItemList = streamProxyMapper.selectAutoRemoveItemByMediaServerId(mediaServerId);
        List<Integer> commonChannelIdList = new ArrayList<>();
        if (!streamProxyItemList.isEmpty()) {
            streamProxyItemList.stream().forEach(streamProxy -> {
                if (streamProxy.getCommonGbChannelId() > 0) {
                    commonChannelIdList.add(streamProxy.getCommonGbChannelId());
                }
            });
        }
        if (!commonChannelIdList.isEmpty()) {
            commonGbChannelService.deleteByIdList(commonChannelIdList);
        }
        streamProxyMapper.deleteAutoRemoveItemByMediaServerId(mediaServerId);

        // 移除拉流代理生成的流信息
        syncPullStream(mediaServerId);
    }

    @Transactional
    public void updateStatusById(StreamProxy streamProxy, boolean status) {
        streamProxyMapper.updatePullingById(streamProxy.getId(), status);
        if (streamProxy.getCommonGbChannelId() > 0) {
            List<Integer> ids = new ArrayList<>();
            ids.add(streamProxy.getCommonGbChannelId());
            if (status) {
                commonGbChannelService.onlineForList(ids);
            }else {
                commonGbChannelService.offlineForList(ids);
            }
        }
    }

    @Override
    public void zlmServerOffline(String mediaServerId) {
        // 移除开启了无人观看自动移除的流
        List<StreamProxy> streamProxyItemList = streamProxyMapper.selectAutoRemoveItemByMediaServerId(mediaServerId);
        List<Integer> commonChannelIdList = new ArrayList<>();
        if (!streamProxyItemList.isEmpty()) {
            streamProxyItemList.stream().forEach(streamProxy -> {
                if (streamProxy.getCommonGbChannelId() > 0) {
                    commonChannelIdList.add(streamProxy.getCommonGbChannelId());
                }
            });
        }
        if (!commonChannelIdList.isEmpty()) {
            commonGbChannelService.deleteByIdList(commonChannelIdList);
        }
        streamProxyMapper.deleteAutoRemoveItemByMediaServerId(mediaServerId);
        // 其他的流设置离线
        streamProxyMapper.updatePullingStatusByMediaServerId(mediaServerId, false);
        String type = "PULL";

        // 发送redis消息
        List<OnStreamChangedHookParam> onStreamChangedHookParams = redisCatchStorage.getStreams(mediaServerId, type);
        if (onStreamChangedHookParams.size() > 0) {
            for (OnStreamChangedHookParam onStreamChangedHookParam : onStreamChangedHookParams) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", onStreamChangedHookParam.getApp());
                jsonObject.put("stream", onStreamChangedHookParam.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", mediaServerId);
                redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
                // 移除redis内流的信息
                redisCatchStorage.removeStream(mediaServerId, type, onStreamChangedHookParam.getApp(), onStreamChangedHookParam.getStream());
            }
        }
    }

    @Override
    @Transactional
    public void updateStatus(boolean status, String app, String stream) {
        StreamProxy streamProxy = streamProxyMapper.selectOne(app, stream);
        if (streamProxy == null) {
            return;
        }
        updateStatusById(streamProxy, status);
    }

    private void syncPullStream(String mediaServerId){
        MediaServerItem mediaServer = mediaServerService.getOne(mediaServerId);
        if (mediaServer != null) {
            List<OnStreamChangedHookParam> allPullStream = redisCatchStorage.getStreams(mediaServerId, "PULL");
            if (!allPullStream.isEmpty()) {
                zlmresTfulUtils.getMediaList(mediaServer, jsonObject->{
                    Map<String, StreamInfo> stringStreamInfoMap = new HashMap<>();
                    if (jsonObject.getInteger("code") == 0) {
                        JSONArray data = jsonObject.getJSONArray("data");
                        if(data != null && data.size() > 0) {
                            for (int i = 0; i < data.size(); i++) {
                                JSONObject streamJSONObj = data.getJSONObject(i);
                                if ("rtsp".equals(streamJSONObj.getString("schema"))) {
                                    StreamInfo streamInfo = new StreamInfo();
                                    String app = streamJSONObj.getString("app");
                                    String stream = streamJSONObj.getString("stream");
                                    streamInfo.setApp(app);
                                    streamInfo.setStream(stream);
                                    stringStreamInfoMap.put(app+stream, streamInfo);
                                }
                            }
                        }
                    }
                    if (stringStreamInfoMap.isEmpty()) {
                        redisCatchStorage.removeStream(mediaServerId, "PULL");
                    }else {
                        for (String key : stringStreamInfoMap.keySet()) {
                            StreamInfo streamInfo = stringStreamInfoMap.get(key);
                            if (stringStreamInfoMap.get(streamInfo.getApp() + streamInfo.getStream()) == null) {
                                redisCatchStorage.removeStream(mediaServerId, "PULL", streamInfo.getApp(),
                                        streamInfo.getStream());
                            }
                        }
                    }
                });
            }

        }

    }

    @Override
    public ResourceBaseInfo getOverview() {

        int total = streamProxyMapper.getAllCount();
        int online = streamProxyMapper.getPulline();

        return new ResourceBaseInfo(total, online);
    }


    /**
     * 检查拉流代理状态
     */
//    @Scheduled(cron = "* 0/10 * * * ?")
//    @Transactional
//    public void asyncCheckStreamProxyStatus() {
//
//        List<MediaServerItem> all = mediaServerService.getAllOnline();
//        if (CollectionUtils.isEmpty(all)){
//            return;
//        }
//        Map<String, MediaServerItem> serverItemMap = all.stream().collect(
//                Collectors.toMap(MediaServerItem::getId, Function.identity(), (m1, m2) -> m1));
//        List<StreamProxy> list = getAllForEnable();
//
//        if (CollectionUtils.isEmpty(list)){
//            return;
//        }
//        for (StreamProxy streamProxyItem : list) {
//            MediaServerItem mediaServerItem = serverItemMap.get(streamProxyItem.getMediaServerId());
//            JSONObject mediaInfo = zlmresTfulUtils.isMediaOnline(mediaServerItem, streamProxyItem.getApp(),
//                    streamProxyItem.getStream(), "rtsp");
//            if (mediaInfo == null){
//                if (streamProxyItem.isStatus()) {
//                    updateStatusById(streamProxyItem, false);
//                }
//            } else {
//                if (mediaInfo.getInteger("code") == 0 && mediaInfo.getBoolean("online")) {
//                    if (!streamProxyItem.isStatus()) {
//                        updateStatusById(streamProxyItem, true);
//                    }
//                } else {
//                    if (streamProxyItem.isStatus()) {
//                        updateStatusById(streamProxyItem, false);
//                    }
//                }
//            }
//
//        }
//    }

    @Override
    public void updateStreamGPS(List<GPSMsgInfo> gpsMsgInfoList) {
        streamProxyMapper.updateStreamGPS(gpsMsgInfoList);
    }

    @Override
    public void getStreamProxyById(Integer id, GeneralCallback<StreamInfo> callback) {
        assert id != null;
        StreamProxy streamProxy = streamProxyMapper.selectOneById(id);
        assert streamProxy != null;
        StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(
                streamProxy.getApp(), streamProxy.getStream(), streamProxy.getMediaServerId(), false);
        if (streamInfo == null) {
            callback.run(ErrorCode.ERROR100.getCode(), "地址获取失败", null);
        }else {
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
        }
    }

    @Override
    public void play(Integer id, GeneralCallback<StreamInfo> callback) {
        StreamProxy streamProxy = streamProxyMapper.selectOneById(id);
        assert streamProxy != null;
        String mediaServerId = streamProxy.getMediaServerId();
        MediaServerItem mediaServerItem;
        if (ObjectUtils.isEmpty(mediaServerId) || mediaServerId.equals("auto")) {
            mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(null);
        }else {
            mediaServerItem = mediaServerService.getOne(mediaServerId);
        }
        if (mediaServerItem == null && callback != null) {
            callback.run(ErrorCode.ERROR100.getCode(), "未找到可用的节点", null);
            return;
        }
        streamProxyPlayService.startProxy(streamProxy, mediaServerItem, callback);
    }

    @Override
    public StreamProxy getStreamProxyByCommonGbChannelId(int commonGbId) {
        return streamProxyMapper.selectOneByByCommonGbChannelId(commonGbId);
    }
}
