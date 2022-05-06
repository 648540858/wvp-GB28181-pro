package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.ParentPlatformMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private ZLMRESTfulUtils zlmresTfulUtils;;

    @Autowired
    private StreamProxyMapper streamProxyMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SipConfig sipConfig;

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


    @Override
    public WVPResult<StreamInfo> save(StreamProxyItem param) {
        MediaServerItem mediaInfo;
        WVPResult<StreamInfo> wvpResult = new WVPResult<>();
        wvpResult.setCode(0);
        if (param.getMediaServerId() == null || "auto".equals(param.getMediaServerId())){
            mediaInfo = mediaServerService.getMediaServerForMinimumLoad();
        }else {
            mediaInfo = mediaServerService.getOne(param.getMediaServerId());
        }
        if (mediaInfo == null) {
            logger.warn("保存代理未找到在线的ZLM...");
            wvpResult.setMsg("保存失败");
            return wvpResult;
        }
        String dstUrl = String.format("rtmp://%s:%s/%s/%s", "127.0.0.1", mediaInfo.getRtmpPort(), param.getApp(),
                param.getStream() );
        param.setDst_url(dstUrl);
        StringBuffer result = new StringBuffer();
        boolean streamLive = false;
        param.setMediaServerId(mediaInfo.getId());
        boolean saveResult;
        // 更新
        if (videoManagerStorager.queryStreamProxy(param.getApp(), param.getStream()) != null) {
            saveResult = videoManagerStorager.updateStreamProxy(param);
        }else { // 新增
            saveResult = videoManagerStorager.addStreamProxy(param);
        }
        if (saveResult) {
            result.append("保存成功");
            if (param.isEnable()) {
                JSONObject jsonObject = addStreamProxyToZlm(param);
                if (jsonObject == null || jsonObject.getInteger("code") != 0) {
                    streamLive = false;
                    result.append(", 但是启用失败，请检查流地址是否可用");
                    param.setEnable(false);
                    // 直接移除
                    if (param.isEnable_remove_none_reader()) {
                        del(param.getApp(), param.getStream());
                    }else {
                        videoManagerStorager.updateStreamProxy(param);
                    }

                }else {
                    streamLive = true;
                    StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(
                            mediaInfo, param.getApp(), param.getStream(), null, null);
                    wvpResult.setData(streamInfo);

                }
            }
        }else {
            result.append("保存失败");
        }
        if ( !StringUtils.isEmpty(param.getPlatformGbId()) && streamLive) {
            List<GbStream> gbStreams = new ArrayList<>();
            gbStreams.add(param);
            if (gbStreamService.addPlatformInfo(gbStreams, param.getPlatformGbId(), param.getCatalogId())){
                result.append(",  关联国标平台[ " + param.getPlatformGbId() + " ]成功");
            }else {
                result.append(",  关联国标平台[ " + param.getPlatformGbId() + " ]失败");
            }
        }
        if (!StringUtils.isEmpty(param.getGbId())) {
            // 查找开启了全部直播流共享的上级平台
            List<ParentPlatform> parentPlatforms = parentPlatformMapper.selectAllAhareAllLiveStream();
            if (parentPlatforms.size() > 0) {
                for (ParentPlatform parentPlatform : parentPlatforms) {
                    param.setPlatformId(parentPlatform.getServerGBId());
                    param.setCatalogId(parentPlatform.getCatalogId());
                    String stream = param.getStream();
                    StreamProxyItem streamProxyItems = platformGbStreamMapper.selectOne(param.getApp(), stream, parentPlatform.getServerGBId());
                    if (streamProxyItems == null) {
                        platformGbStreamMapper.add(param);
                        eventPublisher.catalogEventPublishForStream(parentPlatform.getServerGBId(), param, CatalogEvent.ADD);
                    }
                }
            }
        }

        wvpResult.setMsg(result.toString());
        return wvpResult;
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
            result = zlmresTfulUtils.addStreamProxy(mediaServerItem, param.getApp(), param.getStream(), param.getUrl(),
                    param.isEnable_hls(), param.isEnable_mp4(), param.getRtp_type());
        }else if ("ffmpeg".equals(param.getType())) {
            result = zlmresTfulUtils.addFFmpegSource(mediaServerItem, param.getSrc_url(), param.getDst_url(),
                    param.getTimeout_ms() + "", param.isEnable_hls(), param.isEnable_mp4(),
                    param.getFfmpeg_cmd_key());
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
            videoManagerStorager.deleteStreamProxy(app, stream);
            JSONObject jsonObject = removeStreamProxyFromZlm(streamProxyItem);
            if (jsonObject != null && jsonObject.getInteger("code") == 0) {
                // 如果关联了国标那么移除关联
                gbStreamMapper.del(app, stream);
                platformGbStreamMapper.delByAppAndStream(app, stream);
                // TODO 如果关联的推流， 那么状态设置为离线
            }
            redisCatchStorage.removeStream(streamProxyItem.getMediaServerId(), "PULL", app, stream);
        }


    }

    @Override
    public boolean start(String app, String stream) {
        boolean result = false;
        StreamProxyItem streamProxy = videoManagerStorager.queryStreamProxy(app, stream);
        if (!streamProxy.isEnable() &&  streamProxy != null) {
            JSONObject jsonObject = addStreamProxyToZlm(streamProxy);
            if (jsonObject == null) {
                return false;
            }
            if (jsonObject.getInteger("code") == 0) {
                result = true;
                streamProxy.setEnable(true);
                videoManagerStorager.updateStreamProxy(streamProxy);
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
            if (jsonObject.getInteger("code") == 0) {
                streamProxyDto.setEnable(false);
                result = videoManagerStorager.updateStreamProxy(streamProxyDto);
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
        List<StreamProxyItem> streamProxyItemList = streamProxyMapper.selecAutoRemoveItemByMediaServerId(mediaServerId);
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
        List<StreamProxyItem> streamProxyItemList = streamProxyMapper.selecAutoRemoveItemByMediaServerId(mediaServerId);
        if (streamProxyItemList.size() > 0) {
            gbStreamMapper.batchDel(streamProxyItemList);
        }
        streamProxyMapper.deleteAutoRemoveItemByMediaServerId(mediaServerId);
        // 其他的流设置离线
        streamProxyMapper.updateStatusByMediaServerId(false, mediaServerId);
        String type = "PULL";

        // 发送redis消息
        List<MediaItem> mediaItems = redisCatchStorage.getStreams(mediaServerId, type);
        if (mediaItems.size() > 0) {
            for (MediaItem mediaItem : mediaItems) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", mediaItem.getApp());
                jsonObject.put("stream", mediaItem.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", mediaServerId);
                redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
                // 移除redis内流的信息
                redisCatchStorage.removeStream(mediaServerId, type, mediaItem.getApp(), mediaItem.getStream());
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public int updateStatus(boolean status, String app, String stream) {
        return streamProxyMapper.updateStatus(status, app, stream);
    }

    private void syncPullStream(String mediaServerId){
        MediaServerItem mediaServer = mediaServerService.getOne(mediaServerId);
        if (mediaServer != null) {
            List<MediaItem> allPullStream = redisCatchStorage.getStreams(mediaServerId, "PULL");
            if (allPullStream.size() > 0) {
                zlmresTfulUtils.getMediaList(mediaServer, jsonObject->{
                    Map<String, StreamInfo> stringStreamInfoMap = new HashMap<>();
                    if (jsonObject.getInteger("code") == 0) {
                        JSONArray data = jsonObject.getJSONArray("data");
                        if(data != null && data.size() > 0) {
                            for (int i = 0; i < data.size(); i++) {
                                JSONObject streamJSONObj = data.getJSONObject(i);
                                if ("rtmp".equals(streamJSONObj.getString("schema"))) {
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
}
