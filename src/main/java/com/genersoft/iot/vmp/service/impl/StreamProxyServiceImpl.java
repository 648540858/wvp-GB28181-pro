package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.Page;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频代理业务
 */
@Service
public class StreamProxyServiceImpl implements IStreamProxyService {

    private final static Logger logger = LoggerFactory.getLogger(StreamProxyServiceImpl.class);

    @Autowired
    private IVideoManagerStorager videoManagerStorager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;;

    @Autowired
    private StreamProxyMapper streamProxyMapper;

    @Autowired
    private GbStreamMapper gbStreamMapper;

    @Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

    @Autowired
    private IGbStreamService gbStreamService;

    @Autowired
    private IMediaServerService mediaServerService;


    @Override
    public String save(StreamProxyItem param) {
        MediaServerItem mediaInfo;
        if ("auto".equals(param.getMediaServerId())){
            mediaInfo = mediaServerService.getMediaServerForMinimumLoad();
        }else {
            mediaInfo = mediaServerService.getOne(param.getMediaServerId());
        }
        if (mediaInfo == null) {
            logger.warn("保存代理未找到在线的ZLM...");
            return "保存失败";
        }
        String dstUrl = String.format("rtmp://%s:%s/%s/%s", "127.0.0.1", mediaInfo.getRtmpPort(), param.getApp(),
                param.getStream() );
        param.setDst_url(dstUrl);
        StringBuffer result = new StringBuffer();
        boolean streamLive = false;
        param.setMediaServerId(mediaInfo.getId());
        // 更新
        if (videoManagerStorager.queryStreamProxy(param.getApp(), param.getStream()) != null) {
            if (videoManagerStorager.updateStreamProxy(param)) {
                result.append("保存成功");
                if (param.isEnable()){
                    JSONObject jsonObject = addStreamProxyToZlm(param);
                    if (jsonObject == null) {
                        result.append(", 但是启用失败，请检查流地址是否可用");
                        param.setEnable(false);
                        videoManagerStorager.updateStreamProxy(param);
                    }
                }
            }
        }else { // 新增
            if (videoManagerStorager.addStreamProxy(param)){
                result.append("保存成功");
                streamLive = true;
                if (param.isEnable()) {
                    JSONObject jsonObject = addStreamProxyToZlm(param);
                    if (jsonObject == null) {
                        streamLive = false;
                        result.append(", 但是启用失败，请检查流地址是否可用");
                        param.setEnable(false);
                        videoManagerStorager.updateStreamProxy(param);
                    }
                }
            }else {
                result.append("保存失败");
            }

        }
        if (param.getPlatformGbId() != null && streamLive) {
            List<GbStream> gbStreams = new ArrayList<>();
            gbStreams.add(param);
            if (gbStreamService.addPlatformInfo(gbStreams, param.getPlatformGbId())){
                result.append(",  关联国标平台[ " + param.getPlatformGbId() + " ]成功");
            }else {
                result.append(",  关联国标平台[ " + param.getPlatformGbId() + " ]失败");
            }
        }
        return result.toString();
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
        if (param ==null) return null;
        MediaServerItem mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
        JSONObject result = zlmresTfulUtils.closeStreams(mediaServerItem, param.getApp(), param.getStream());
        return result;
    }

    @Override
    public Page<StreamProxyItem> getAll(Integer page, Integer count, String query, Boolean enable) {
        return videoManagerStorager.queryStreamProxyList(page, count, query, enable);
    }

    @Override
    public void del(String app, String stream) {
        StreamProxyItem streamProxyItem = videoManagerStorager.queryStreamProxy(app, stream);
        if (streamProxyItem != null) {
            videoManagerStorager.deleteStreamProxy(app, stream);
            JSONObject jsonObject = removeStreamProxyFromZlm(streamProxyItem);
            if (jsonObject != null && jsonObject.getInteger("code") == 0) {
                // 如果关联了国标那么移除关联
                gbStreamMapper.del(app, stream);
                platformGbStreamMapper.delByAppAndStream(app, stream);
                // TODO 如果关联的推流， 那么状态设置为离线
            }
        }

    }

    @Override
    public boolean start(String app, String stream) {
        boolean result = false;
        StreamProxyItem streamProxy = videoManagerStorager.queryStreamProxy(app, stream);
        if (!streamProxy.isEnable() &&  streamProxy != null) {
            JSONObject jsonObject = addStreamProxyToZlm(streamProxy);
            if (jsonObject == null) return false;
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
}
