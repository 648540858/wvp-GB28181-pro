package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxy;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.service.ICommonGbChannelService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.storager.dao.ParentPlatformMapper;
import com.genersoft.iot.vmp.storager.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.*;

/**
 * 视频代理业务
 */
@Service
public class StreamProxyServiceImpl implements IStreamProxyService {

    private final static Logger logger = LoggerFactory.getLogger(StreamProxyServiceImpl.class);

    @Autowired
    private IVideoManagerStorage videoManagerStorager;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Autowired
    private StreamProxyMapper streamProxyMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ICommonGbChannelService commonGbChannelService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ZlmHttpHookSubscribe hookSubscribe;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;


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
            if (streamProxyInDb.getCommonGbChannelId() == 0 && !ObjectUtils.isEmpty(param.getGbId()) ) {
                // 新增通用通道
                CommonGbChannel commonGbChannel = CommonGbChannel.getInstance(param);
                commonGbChannelService.add(commonGbChannel);
                param.setCommonGbChannelId(commonGbChannel.getCommonGbId());
            }
            if (streamProxyInDb.getCommonGbChannelId() > 0 && ObjectUtils.isEmpty(param.getGbId()) ) {
                // 移除通用通道
                commonGbChannelService.deleteById(streamProxyInDb.getCommonGbChannelId());
            }
            param.setUpdateTime(DateUtil.getNow());
            streamProxyMapper.update(param);
        }else { // 新增
            if (!ObjectUtils.isEmpty(param.getGbId())) {
                // 新增通用通道
                CommonGbChannel commonGbChannel = CommonGbChannel.getInstance(param);
                commonGbChannelService.add(commonGbChannel);
                param.setCommonGbChannelId(commonGbChannel.getCommonGbId());
            }
            param.setCreateTime(DateUtil.getNow());
            param.setUpdateTime(DateUtil.getNow());
            streamProxyMapper.add(param);
        }
        HookSubscribeForStreamChange hookSubscribeForStreamChange = HookSubscribeFactory.on_stream_changed(param.getApp(), param.getStream(), true, "rtsp", mediaInfo.getId());
        hookSubscribe.addSubscribe(hookSubscribeForStreamChange, (mediaServerItem, response) -> {
            StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(
                    mediaInfo, param.getApp(), param.getStream(), null, null);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
        });
        if (param.isEnable()) {
            String talkKey = UUID.randomUUID().toString();
            String delayTalkKey = UUID.randomUUID().toString();
            dynamicTask.startDelay(delayTalkKey, ()->{
                StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(param.getApp(), param.getStream(), mediaInfo.getId(), false);
                if (streamInfo != null) {
                    callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
                }else {
                    dynamicTask.stop(talkKey);
                    callback.run(ErrorCode.ERROR100.getCode(), "超时", null);
                }
            }, 7000);
            JSONObject jsonObject = addStreamProxyToZlm(param);
            if (jsonObject != null && jsonObject.getInteger("code") == 0) {
                hookSubscribe.removeSubscribe(hookSubscribeForStreamChange);
                dynamicTask.stop(talkKey);
                StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(
                        mediaInfo, param.getApp(), param.getStream(), null, null);
                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
            }else {
                param.setEnable(false);
                // 直接移除
                if (param.isEnableRemoveNoneReader()) {
                    del(param.getApp(), param.getStream());
                }else {
                    updateStreamProxy(param);
                }
                if (jsonObject == null){
                    callback.run(ErrorCode.ERROR100.getCode(), "记录已保存，启用失败", null);
                }else {
                    callback.run(ErrorCode.ERROR100.getCode(), jsonObject.getString("msg"), null);
                }
            }
        }
        else{
            StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(
                    mediaInfo, param.getApp(), param.getStream(), null, null);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
        }
    }

    @Override
    @Transactional
    public void add(StreamProxy param, GeneralCallback<StreamInfo> callback) {
        MediaServerItem mediaInfo;
        if (ObjectUtils.isEmpty(param.getMediaServerId()) || "auto".equals(param.getMediaServerId())){
            mediaInfo = mediaServerService.getMediaServerForMinimumLoad(null);
        }else {
            mediaInfo = mediaServerService.getOne(param.getMediaServerId());
        }
        if (mediaInfo == null) {
            logger.warn("[添加拉流代理] 未找到在线的ZLM...");
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "保存代理未找到可用的ZLM");
        }
        if ("ffmpeg".equalsIgnoreCase(param.getType())) {
            if (ObjectUtils.isEmpty(param.getDstUrl())) {
                logger.warn("[添加拉流代理] 未设置目标URL地址（DstUrl）");
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "未设置目标URL地址");
            }
            logger.info("[添加拉流代理] ffmpeg，源地址: {}, 目标地址为：{}", param.getUrl(), param.getDstUrl());
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
            logger.info("[添加拉流代理] 直接拉流，源地址: {}, app: {}, stream: {}", param.getUrl(), param.getApp(), param.getStream());
        }
        param.setMediaServerId(mediaInfo.getId());
        StreamProxy streamProxyInDb = streamProxyMapper.selectOne(param.getApp(), param.getStream());
        if (streamProxyInDb != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "app/stream已经存在");
        }
        if (!param.isEnable()) {
            param.setStatus(false);
            saveProxyToDb(param);
            StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(
                    mediaInfo, param.getApp(), param.getStream(), null, null);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
            return;
        }
        String talkKey = UUID.randomUUID().toString();
        String delayTalkKey = UUID.randomUUID().toString();

        HookSubscribeForStreamChange hookSubscribeForStreamChange = HookSubscribeFactory.on_stream_changed(param.getApp(), param.getStream(), true, "rtsp", mediaInfo.getId());
        hookSubscribe.addSubscribe(hookSubscribeForStreamChange, (mediaServerItem, response) -> {
            dynamicTask.stop(talkKey);
            param.setStatus(true);
            saveProxyToDb(param);
            StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(
                    mediaInfo, param.getApp(), param.getStream(), null, null);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
        });

        dynamicTask.startDelay(delayTalkKey, ()->{
            hookSubscribe.removeSubscribe(hookSubscribeForStreamChange);
            dynamicTask.stop(talkKey);
            callback.run(ErrorCode.ERROR100.getCode(), "启用超时，请检查源地址是否可用", null);
            if (param.isEnableRemoveNoneReader()) {
                return;
            }
            param.setProxyError("启用超时");
            param.setStatus(false);
            saveProxyToDb(param);
        }, 10000);
        JSONObject jsonObject = addStreamProxyToZlm(param);
        if (jsonObject != null && jsonObject.getInteger("code") != 0) {
            hookSubscribe.removeSubscribe(hookSubscribeForStreamChange);
            dynamicTask.stop(talkKey);
            callback.run(ErrorCode.ERROR100.getCode(), jsonObject.getString("msg"), null);
            if (param.isEnableRemoveNoneReader()) {
                return;
            }
            param.setProxyError("启用失败: " + jsonObject.getString("msg"));
            param.setStatus(false);
            saveProxyToDb(param);
        }
    }

    @Override
    public void edit(StreamProxy param, GeneralCallback<StreamInfo> callback) {
        MediaServerItem mediaInfo;
        StreamProxy streamProxyInDb = streamProxyMapper.selectOneById(param.getId());
        if (streamProxyInDb != null) {
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
        // 国标编号发生变化，修改通用通道国标变化，通用通道应发送删除再发送添加命令通知上级
        // 类型变化，启用->启用：需要重新拉起视频流， 启用->未启用： 停止旧的视频流， 未启用->启用：发起新的视频流
        // 流地址发生变化。停止旧的，拉起新的
        // ffmpeg类型下，目标流地址变化，停止旧的，拉起新的
        // 节点变化： 停止旧的，拉起新的
        // ffmpeg命令模板变化： 停止旧的，拉起新的
        if (ObjectUtils.isEmpty(streamProxyInDb.getGbId())) {
            if (!ObjectUtils.isEmpty(param.getGbId())) {
                // 之前是空的，现在添加了国标编号

            }
        }else {
            if (ObjectUtils.isEmpty(param.getGbId())) {
                // 移除了国标编号
            }else {
                if (!streamProxyInDb.getGbId().equals(param.getGbId())) {
                    // 修改了国标编号
                }
            }
        }



        if ("ffmpeg".equalsIgnoreCase(param.getType())) {
            if (ObjectUtils.isEmpty(param.getDstUrl())) {
                logger.warn("[添加拉流代理] 未设置目标URL地址（DstUrl）");
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "未设置目标URL地址");
            }
            logger.info("[添加拉流代理] ffmpeg，源地址: {}, 目标地址为：{}", param.getUrl(), param.getDstUrl());
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
            logger.info("[添加拉流代理] 直接拉流，源地址: {}, app: {}, stream: {}", param.getUrl(), param.getApp(), param.getStream());
        }
        param.setMediaServerId(mediaInfo.getId());
        StreamProxy streamProxyInDb = streamProxyMapper.selectOne(param.getApp(), param.getStream());
        if (streamProxyInDb != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "app/stream已经存在");
        }
        if (!param.isEnable()) {
            param.setStatus(false);
            saveProxyToDb(param);
            StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(
                    mediaInfo, param.getApp(), param.getStream(), null, null);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
            return;
        }
        String talkKey = UUID.randomUUID().toString();
        String delayTalkKey = UUID.randomUUID().toString();

        HookSubscribeForStreamChange hookSubscribeForStreamChange = HookSubscribeFactory.on_stream_changed(param.getApp(), param.getStream(), true, "rtsp", mediaInfo.getId());
        hookSubscribe.addSubscribe(hookSubscribeForStreamChange, (mediaServerItem, response) -> {
            dynamicTask.stop(talkKey);
            param.setStatus(true);
            saveProxyToDb(param);
            StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(
                    mediaInfo, param.getApp(), param.getStream(), null, null);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
        });

        dynamicTask.startDelay(delayTalkKey, ()->{
            hookSubscribe.removeSubscribe(hookSubscribeForStreamChange);
            dynamicTask.stop(talkKey);
            callback.run(ErrorCode.ERROR100.getCode(), "启用超时，请检查源地址是否可用", null);
            if (param.isEnableRemoveNoneReader()) {
                return;
            }
            param.setProxyError("启用超时");
            param.setStatus(false);
            saveProxyToDb(param);
        }, 10000);
        JSONObject jsonObject = addStreamProxyToZlm(param);
        if (jsonObject != null && jsonObject.getInteger("code") != 0) {
            hookSubscribe.removeSubscribe(hookSubscribeForStreamChange);
            dynamicTask.stop(talkKey);
            callback.run(ErrorCode.ERROR100.getCode(), jsonObject.getString("msg"), null);
            if (param.isEnableRemoveNoneReader()) {
                return;
            }
            param.setProxyError("启用失败: " + jsonObject.getString("msg"));
            param.setStatus(false);
            saveProxyToDb(param);
        }
    }

    private void saveProxyToDb(StreamProxy param) {
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

    /**
     * 更新代理流
     * @param streamProxyItem
     * @return
     */
    @Override
    public boolean updateStreamProxy(StreamProxy streamProxyItem) {
        streamProxyItem.setCreateTime(DateUtil.getNow());
        return streamProxyMapper.update(streamProxyItem) > 0;
    }

    @Override
    public JSONObject addStreamProxyToZlm(StreamProxy param) {
        JSONObject result = null;
        MediaServerItem mediaServerItem = null;
        if (param.getMediaServerId() == null) {
            logger.warn("添加代理时MediaServerId 为null");
            return null;
        }else {
            mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
        }
        if (mediaServerItem == null) {
            return null;
        }
        if (zlmServerFactory.isStreamReady(mediaServerItem, param.getApp(), param.getStream())) {
            zlmresTfulUtils.closeStreams(mediaServerItem, param.getApp(), param.getStream());
        }
        if ("ffmpeg".equalsIgnoreCase(param.getType())){
            result = zlmresTfulUtils.addFFmpegSource(mediaServerItem, param.getSrcUrl().trim(), param.getDstUrl(),
                    param.getTimeoutMs() + "", param.isEnableAudio(), param.isEnableMp4(),
                    param.getFfmpegCmdKey());
        }else {
            result = zlmresTfulUtils.addStreamProxy(mediaServerItem, param.getApp(), param.getStream(), param.getUrl().trim(),
                    param.isEnableAudio(), param.isEnableMp4(), param.getRtpType());
        }
        if (result != null && result.getInteger("code") == 0) {
            JSONObject data = result.getJSONObject("data");
            if (data == null) {
                logger.warn("[获取拉流代理的结果数据Data] 失败： {}", result );
                return result;
            }
            String key = data.getString("key");
            if (key == null) {
                logger.warn("[获取拉流代理的结果数据Data中的KEY] 失败： {}", result );
                return result;
            }
            param.setStreamKey(key);
            streamProxyMapper.update(param);
        }
        return result;
    }

    @Override
    public JSONObject removeStreamProxyFromZlm(StreamProxy param) {
        if (param ==null) {
            return null;
        }
        MediaServerItem mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
        JSONObject result = null;
        if ("ffmpeg".equalsIgnoreCase(param.getType())){
            result = zlmresTfulUtils.delFFmpegSource(mediaServerItem, param.getStreamKey());
        }else {
            result = zlmresTfulUtils.delStreamProxy(mediaServerItem, param.getStreamKey());
        }
        return result;
    }

    @Override
    public PageInfo<StreamProxy> getAll(Integer page, Integer count) {
        PageHelper.startPage(page, count);
        List<StreamProxy> all = streamProxyMapper.selectAll();
        return new PageInfo<>(all);
    }

    @Override
    public void del(String app, String stream) {
        StreamProxy streamProxyItem = streamProxyMapper.selectOne(app, stream);
        if (streamProxyItem == null) {
            return;
        }
        if (streamProxyItem.getCommonGbChannelId() > 0) {
            commonGbChannelService.deleteById(streamProxyItem.getCommonGbChannelId());
        }
        streamProxyMapper.delById(streamProxyItem.getId());
        redisCatchStorage.removeStream(streamProxyItem.getMediaServerId(), "PULL", app, stream);

        JSONObject jsonObject = removeStreamProxyFromZlm(streamProxyItem);
        if (jsonObject != null && jsonObject.getInteger("code") == 0) {
            logger.info("[移除代理]： 代理： {}/{}, 从zlm移除成功", app, stream);
        }else {
            logger.info("[移除代理]： 代理： {}/{}, 从zlm移除失败", app, stream);
        }
    }

    @Override
    public void delById(int id) {
        StreamProxy streamProxyItem = streamProxyMapper.selectOneById(id);
        if (streamProxyItem == null) {
            return;
        }
        if (streamProxyItem.getCommonGbChannelId() > 0) {
            commonGbChannelService.deleteById(streamProxyItem.getCommonGbChannelId());
        }
        streamProxyMapper.delById(id);
        redisCatchStorage.removeStream(streamProxyItem.getMediaServerId(), "PULL", streamProxyItem.getApp(), streamProxyItem.getStream());

        JSONObject jsonObject = removeStreamProxyFromZlm(streamProxyItem);
        if (jsonObject != null && jsonObject.getInteger("code") == 0) {
            logger.info("[移除代理]： 代理： {}/{}, 从zlm移除成功", streamProxyItem.getApp(), streamProxyItem.getStream());
        }else {
            logger.info("[移除代理]： 代理： {}/{}, 从zlm移除失败", streamProxyItem.getApp(), streamProxyItem.getStream());
        }
    }

    @Override
    public boolean start(String app, String stream) {
        boolean result = false;
        StreamProxy streamProxy = streamProxyMapper.selectOne(app, stream);
        if (streamProxy != null && !streamProxy.isEnable() ) {
            JSONObject jsonObject = addStreamProxyToZlm(streamProxy);
            if (jsonObject == null) {
                return false;
            }
            if (jsonObject.getInteger("code") == 0) {
                result = true;
                streamProxy.setEnable(true);
                updateStreamProxy(streamProxy);
            }else {
                logger.info("启用代理失败： {}/{}->{}({})", app, stream, jsonObject.getString("msg"),
                        streamProxy.getSrcUrl() == null? streamProxy.getUrl():streamProxy.getSrcUrl());
            }
        } else if (streamProxy != null && streamProxy.isEnable()) {
           return true ;
        }
        return result;
    }

    @Override
    public boolean stop(String app, String stream) {
        boolean result = false;
        StreamProxy streamProxyDto = streamProxyMapper.selectOne(app, stream);
        if (streamProxyDto != null && streamProxyDto.isEnable()) {
            JSONObject jsonObject = removeStreamProxyFromZlm(streamProxyDto);
            if (jsonObject != null && jsonObject.getInteger("code") == 0) {
                streamProxyDto.setEnable(false);
                result = updateStreamProxy(streamProxyDto);
            }
        }
        return result;
    }

    @Override
    public JSONObject getFFmpegCMDs(MediaServerItem mediaServerItem) {
        JSONObject result = new JSONObject();
        JSONObject mediaServerConfigResuly = zlmresTfulUtils.getMediaServerConfig(mediaServerItem);
        if (mediaServerConfigResuly != null && mediaServerConfigResuly.getInteger("code") == 0
                && mediaServerConfigResuly.getJSONArray("data").size() > 0){
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

        // 恢复流代理, 只查找这个这个流媒体
        List<StreamProxy> streamProxyListForEnable = streamProxyMapper.selectForEnableInMediaServer(
                mediaServerId, true);
        for (StreamProxy streamProxyDto : streamProxyListForEnable) {
            logger.info("恢复流代理，" + streamProxyDto.getApp() + "/" + streamProxyDto.getStream());
            JSONObject jsonObject = addStreamProxyToZlm(streamProxyDto);
            if (jsonObject == null) {
                // 设置为离线
                logger.info("恢复流代理失败" + streamProxyDto.getApp() + "/" + streamProxyDto.getStream());
                updateStatus(false, streamProxyDto.getApp(), streamProxyDto.getStream());
            }else {
                updateStatus(true, streamProxyDto.getApp(), streamProxyDto.getStream());
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
        streamProxyMapper.updateStatusByMediaServerId(mediaServerId, false);
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
    public void clean() {

    }

    @Override
    public int updateStatus(boolean status, String app, String stream) {
        return streamProxyMapper.updateStatus(app, stream, status);
    }

    private void syncPullStream(String mediaServerId){
        MediaServerItem mediaServer = mediaServerService.getOne(mediaServerId);
        if (mediaServer != null) {
            List<OnStreamChangedHookParam> allPullStream = redisCatchStorage.getStreams(mediaServerId, "PULL");
            if (allPullStream.size() > 0) {
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
                    if (stringStreamInfoMap.size() == 0) {
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
        int online = streamProxyMapper.getOnline();

        return new ResourceBaseInfo(total, online);
    }


    /**
     * 检查拉流代理状态
     */
    @Scheduled(cron = "* 0/10 * * * ?")
    public void asyncCheckStreamProxyStatus() {

        List<MediaServerItem> all = mediaServerService.getAllOnline();

        if (CollectionUtils.isEmpty(all)){
            return;
        }

        Map<String, MediaServerItem> serverItemMap = all.stream().collect(Collectors.toMap(MediaServerItem::getId, Function.identity(), (m1, m2) -> m1));

        List<StreamProxy> list = getAllForEnable();

        if (CollectionUtils.isEmpty(list)){
            return;
        }

        for (StreamProxy streamProxyItem : list) {

            MediaServerItem mediaServerItem = serverItemMap.get(streamProxyItem.getMediaServerId());

            // TODO 支持其他 schema
            JSONObject mediaInfo = zlmresTfulUtils.isMediaOnline(mediaServerItem, streamProxyItem.getApp(), streamProxyItem.getStream(), "rtsp");

            if (mediaInfo == null){
                streamProxyItem.setStatus(false);
            } else {
                if (mediaInfo.getInteger("code") == 0 && mediaInfo.getBoolean("online")) {
                    streamProxyItem.setStatus(true);
                } else {
                    streamProxyItem.setStatus(false);
                }
            }

            updateStreamProxy(streamProxyItem);
        }
    }

    @Override
    public void updateStreamGPS(List<GPSMsgInfo> gpsMsgInfoList) {
        streamProxyMapper.updateStreamGPS(gpsMsgInfoList);
    }

    @Override
    public List<StreamProxy> getAllForEnable() {
        return streamProxyMapper.selectForEnable(true);
    }
}
