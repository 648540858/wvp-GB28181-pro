package com.genersoft.iot.vmp.vmanager.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyDto;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.storager.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.vmanager.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.streamProxy.StreamProxyController;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 视频代理业务
 */
@Service
public class StreamProxyServiceImpl implements IStreamProxyService {

    @Autowired
    private IVideoManagerStorager videoManagerStorager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private StreamProxyMapper streamProxyMapper;


    @Override
    public void save(StreamProxyDto param) {
        MediaServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
        String dstUrl = String.format("rtmp://%s:%s/%s/%s", "127.0.0.1", mediaInfo.getRtmpPort(), param.getApp(),
                param.getStream() );
        param.setDst_url(dstUrl);
        // 更新
        if (videoManagerStorager.queryStreamProxy(param.getApp(), param.getStream()) != null) {
            int result = videoManagerStorager.updateStreamProxy(param);
            if (result > 0 && param.isEnable()) {
                addStreamProxyToZlm(param);
            }
        }else { // 新增
            int result = videoManagerStorager.addStreamProxy(param);
            if (result > 0 && param.isEnable()) {
                addStreamProxyToZlm(param);
            }
        }
    }

    @Override
    public JSONObject addStreamProxyToZlm(StreamProxyDto param) {
        JSONObject result = null;
        if ("default".equals(param.getType())){
            result = zlmresTfulUtils.addStreamProxy(param.getApp(), param.getStream(), param.getUrl(),
                    param.isEnable_hls(), param.isEnable_mp4(), param.getRtp_type());
        }else if ("ffmpeg".equals(param.getType())) {
            result = zlmresTfulUtils.addFFmpegSource(param.getSrc_url(), param.getDst_url(),
                    param.getTimeout_ms() + "");
        }
        return result;
    }

    @Override
    public JSONObject removeStreamProxyFromZlm(StreamProxyDto param) {
        JSONObject result = zlmresTfulUtils.closeStreams(param.getApp(), param.getStream());
        return result;
    }

    @Override
    public PageInfo<StreamProxyDto> getAll(Integer page, Integer count) {
        return videoManagerStorager.queryStreamProxyList(page, count);
    }

    @Override
    public void del(String app, String stream) {
        StreamProxyDto streamProxyDto = new StreamProxyDto();
        streamProxyDto.setApp(app);
        streamProxyDto.setStream(stream);
        JSONObject jsonObject = removeStreamProxyFromZlm(streamProxyDto);
        if (jsonObject.getInteger("code") == 0) {
            videoManagerStorager.deleteStreamProxy(app, stream);
        }
    }

    @Override
    public boolean start(String app, String stream) {
        boolean result = false;
        StreamProxyDto streamProxyDto = videoManagerStorager.queryStreamProxy(app, stream);
        if (!streamProxyDto.isEnable() &&  streamProxyDto != null) {
            JSONObject jsonObject = addStreamProxyToZlm(streamProxyDto);
            if (jsonObject.getInteger("code") == 0) {
                result = true;
                streamProxyDto.setEnable(true);
                videoManagerStorager.updateStreamProxy(streamProxyDto);
            }
        }
        return result;
    }

    @Override
    public boolean stop(String app, String stream) {
        boolean result = false;
        StreamProxyDto streamProxyDto = videoManagerStorager.queryStreamProxy(app, stream);
        if (streamProxyDto.isEnable() &&  streamProxyDto != null) {
            JSONObject jsonObject = removeStreamProxyFromZlm(streamProxyDto);
            if (jsonObject.getInteger("code") == 0) {
                result = true;
                streamProxyDto.setEnable(false);
                videoManagerStorager.updateStreamProxy(streamProxyDto);
            }
        }
        return result;
    }
}
