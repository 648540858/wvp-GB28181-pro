package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.service.IStreamProxyService;
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
    private PlatformGbStreamMapper platformGbStreamMapper;


    @Override
    public String save(StreamProxyItem param) {
        MediaServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
        String dstUrl = String.format("rtmp://%s:%s/%s/%s", "127.0.0.1", mediaInfo.getRtmpPort(), param.getApp(),
                param.getStream() );
        param.setDst_url(dstUrl);
        StringBuffer result = new StringBuffer();
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
                if (param.isEnable()) {
                    JSONObject jsonObject = addStreamProxyToZlm(param);
                    if (jsonObject == null) {
                        result.append(", 但是启用失败，请检查流地址是否可用");
                        param.setEnable(false);
                        videoManagerStorager.updateStreamProxy(param);
                    }
                }
            }

        }
        return result.toString();
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
            platformGbStreamMapper.delByAppAndStream(app, stream);
            // TODO 如果关联的推流， 那么状态设置为离线
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
}
