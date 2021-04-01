package com.genersoft.iot.vmp.vmanager.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.PlarfotmGbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.vmanager.service.IStreamProxyService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    private GbStreamMapper gbStreamMapper;

    @Autowired
    private PlarfotmGbStreamMapper plarfotmGbStreamMapper;


    @Override
    public void save(StreamProxyItem param) {
        MediaServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
        String dstUrl = String.format("rtmp://%s:%s/%s/%s", "127.0.0.1", mediaInfo.getRtmpPort(), param.getApp(),
                param.getStream() );
        param.setDst_url(dstUrl);
        // 更新
        if (videoManagerStorager.queryStreamProxy(param.getApp(), param.getStream()) != null) {
            boolean result = videoManagerStorager.updateStreamProxy(param);
            if (result && param.isEnable()) {
                addStreamProxyToZlm(param);
            }
        }else { // 新增
            boolean result = videoManagerStorager.addStreamProxy(param);
            if (result  && param.isEnable()) {
                addStreamProxyToZlm(param);
            }
        }
    }

    @Override
    public JSONObject addStreamProxyToZlm(StreamProxyItem param) {
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
    public JSONObject removeStreamProxyFromZlm(StreamProxyItem param) {
        JSONObject result = zlmresTfulUtils.closeStreams(param.getApp(), param.getStream());

        return result;
    }

    @Override
    public PageInfo<StreamProxyItem> getAll(Integer page, Integer count) {
        return videoManagerStorager.queryStreamProxyList(page, count);
    }

    @Override
    public void del(String app, String stream) {
        StreamProxyItem streamProxyItem = new StreamProxyItem();
        streamProxyItem.setApp(app);
        streamProxyItem.setStream(stream);
        JSONObject jsonObject = removeStreamProxyFromZlm(streamProxyItem);
        if (jsonObject.getInteger("code") == 0) {
            videoManagerStorager.deleteStreamProxy(app, stream);
            // 如果关联了国标那么移除关联
            gbStreamMapper.del(app, stream);
            plarfotmGbStreamMapper.delByAppAndStream(app, stream);
            // TODO 如果关联的推流， 那么状态设置为离线
        }
    }

    @Override
    public boolean start(String app, String stream) {
        boolean result = false;
        StreamProxyItem streamProxy = videoManagerStorager.queryStreamProxy(app, stream);
        if (!streamProxy.isEnable() &&  streamProxy != null) {
            JSONObject jsonObject = addStreamProxyToZlm(streamProxy);
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
