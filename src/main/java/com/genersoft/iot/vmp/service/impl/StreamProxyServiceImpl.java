package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.ParentPlatformMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private StreamProxyMapper streamProxyMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private GbStreamMapper gbStreamMapper;

    @Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private ParentPlatformMapper parentPlatformMapper;

    @Autowired
    private IGbStreamService gbStreamService;

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
    public void save(StreamProxyItem param, GeneralCallback<StreamInfo> callback) {
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
            String ffmpegCmd = mediaServerConfig.getString(param.getFfmpegCmdKey());
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
            dstUrl = String.format("rtmp://%s:%s/%s/%s", "127.0.0.1", mediaInfo.getRtmpPort(), param.getApp(),
                    param.getStream());
        }
        param.setDstUrl(dstUrl);
        logger.info("[拉流代理] 输出地址为：{}", dstUrl);
        param.setMediaServerId(mediaInfo.getId());
        boolean saveResult;
        // 更新
        if (videoManagerStorager.queryStreamProxy(param.getApp(), param.getStream()) != null) {
            saveResult = updateStreamProxy(param);
        }else { // 新增
            saveResult = addStreamProxy(param);
        }
        if (!saveResult) {
            callback.run(ErrorCode.ERROR100.getCode(), "保存失败", null);
            return;
        }

        if (param.isEnable()) {
            String talkKey = UUID.randomUUID().toString();
            dynamicTask.startCron(talkKey, ()->{
                StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(param.getApp(), param.getStream(), mediaInfo.getId(), false);
                if (streamInfo != null) {
                    callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
                }
            }, 1000);
            String delayTalkKey = UUID.randomUUID().toString();
            dynamicTask.startDelay(delayTalkKey, ()->{
                StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(param.getApp(), param.getStream(), mediaInfo.getId(), false);
                if (streamInfo != null) {
                    callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
                }else {
                    dynamicTask.stop(talkKey);
                    callback.run(ErrorCode.ERROR100.getCode(), "超时", null);
                }
            }, 5000);
            JSONObject jsonObject = addStreamProxyToZlm(param);
            if (jsonObject != null && jsonObject.getInteger("code") == 0) {
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
     * 新增代理流
     * @param streamProxyItem
     * @return
     */
    private boolean addStreamProxy(StreamProxyItem streamProxyItem) {
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        boolean result = false;
        streamProxyItem.setStreamType("proxy");
        streamProxyItem.setStatus(true);
        String now = DateUtil.getNow();
        streamProxyItem.setCreateTime(now);
        try {
            if (streamProxyMapper.add(streamProxyItem) > 0) {
                if (!ObjectUtils.isEmpty(streamProxyItem.getGbId())) {
                    if (gbStreamMapper.add(streamProxyItem) < 0) {
                        //事务回滚
                        dataSourceTransactionManager.rollback(transactionStatus);
                        return false;
                    }
                }
            }else {
                //事务回滚
                dataSourceTransactionManager.rollback(transactionStatus);
                return false;
            }
            result = true;
            dataSourceTransactionManager.commit(transactionStatus);     //手动提交
        }catch (Exception e) {
            logger.error("向数据库添加流代理失败：", e);
            dataSourceTransactionManager.rollback(transactionStatus);
        }


        return result;
    }

    /**
     * 更新代理流
     * @param streamProxyItem
     * @return
     */
    @Override
    public boolean updateStreamProxy(StreamProxyItem streamProxyItem) {
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        boolean result = false;
        streamProxyItem.setStreamType("proxy");
        try {
            if (streamProxyMapper.update(streamProxyItem) > 0) {
                if (!ObjectUtils.isEmpty(streamProxyItem.getGbId())) {
                    if (gbStreamMapper.updateByAppAndStream(streamProxyItem) == 0) {
                        //事务回滚
                        dataSourceTransactionManager.rollback(transactionStatus);
                        return false;
                    }
                }
            } else {
                //事务回滚
                dataSourceTransactionManager.rollback(transactionStatus);
                return false;
            }

            dataSourceTransactionManager.commit(transactionStatus);     //手动提交
            result = true;
        }catch (Exception e) {
            logger.error("未处理的异常 ", e);
            dataSourceTransactionManager.rollback(transactionStatus);
        }
        return result;
    }

    @Override
    public JSONObject addStreamProxyToZlm(StreamProxyItem param) {
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
        if ("default".equals(param.getType())){
            result = zlmresTfulUtils.addStreamProxy(mediaServerItem, param.getApp(), param.getStream(), param.getUrl().trim(),
                    param.isEnableAudio(), param.isEnableMp4(), param.getRtpType());
        }else if ("ffmpeg".equals(param.getType())) {
            result = zlmresTfulUtils.addFFmpegSource(mediaServerItem, param.getSrcUrl().trim(), param.getDstUrl(),
                    param.getTimeoutMs() + "", param.isEnableAudio(), param.isEnableMp4(),
                    param.getFfmpegCmdKey());
        }
        return result;
    }

    @Override
    public JSONObject removeStreamProxyFromZlm(StreamProxyItem param) {
        if (param ==null) {
            return null;
        }
        MediaServerItem mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
        JSONObject result = zlmresTfulUtils.closeStreams(mediaServerItem, param.getApp(), param.getStream());
        return result;
    }

    @Override
    public PageInfo<StreamProxyItem> getAll(Integer page, Integer count) {
        return videoManagerStorager.queryStreamProxyList(page, count);
    }

    @Override
    public void del(String app, String stream) {
        StreamProxyItem streamProxyItem = videoManagerStorager.queryStreamProxy(app, stream);
        if (streamProxyItem != null) {
            gbStreamService.sendCatalogMsg(streamProxyItem, CatalogEvent.DEL);

            JSONObject jsonObject = removeStreamProxyFromZlm(streamProxyItem);
            if (jsonObject != null && jsonObject.getInteger("code") == 0) {
                // 如果关联了国标那么移除关联
                int i = platformGbStreamMapper.delByAppAndStream(app, stream);
                gbStreamMapper.del(app, stream);
                System.out.println();
                // TODO 如果关联的推流， 那么状态设置为离线
            }
            videoManagerStorager.deleteStreamProxy(app, stream);
            redisCatchStorage.removeStream(streamProxyItem.getMediaServerId(), "PULL", app, stream);
        }


    }

    @Override
    public boolean start(String app, String stream) {
        boolean result = false;
        StreamProxyItem streamProxy = videoManagerStorager.queryStreamProxy(app, stream);
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
        }
        return result;
    }

    @Override
    public boolean stop(String app, String stream) {
        boolean result = false;
        StreamProxyItem streamProxyDto = videoManagerStorager.queryStreamProxy(app, stream);
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
    public StreamProxyItem getStreamProxyByAppAndStream(String app, String streamId) {
        return videoManagerStorager.getStreamProxyByAppAndStream(app, streamId);
    }

    @Override
    public void zlmServerOnline(String mediaServerId) {
        // 移除开启了无人观看自动移除的流
        List<StreamProxyItem> streamProxyItemList = streamProxyMapper.selectAutoRemoveItemByMediaServerId(mediaServerId);
        if (streamProxyItemList.size() > 0) {
            gbStreamMapper.batchDel(streamProxyItemList);
        }
        streamProxyMapper.deleteAutoRemoveItemByMediaServerId(mediaServerId);

        // 移除拉流代理生成的流信息
//        syncPullStream(mediaServerId);

        // 恢复流代理, 只查找这个这个流媒体
        List<StreamProxyItem> streamProxyListForEnable = storager.getStreamProxyListForEnableInMediaServer(
                mediaServerId, true);
        for (StreamProxyItem streamProxyDto : streamProxyListForEnable) {
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
        List<StreamProxyItem> streamProxyItemList = streamProxyMapper.selectAutoRemoveItemByMediaServerId(mediaServerId);
        if (streamProxyItemList.size() > 0) {
            gbStreamMapper.batchDel(streamProxyItemList);
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
}
