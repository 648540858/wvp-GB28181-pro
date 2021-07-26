package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@SuppressWarnings("unchecked")
@Component
public class ZLMMediaListManager {

    private Logger logger = LoggerFactory.getLogger("ZLMMediaListManager");

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private GbStreamMapper gbStreamMapper;

    @Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private ZLMHttpHookSubscribe subscribe;


    public void updateMediaList(MediaServerItem mediaServerItem) {
        storager.clearMediaList();

        // 使用异步的当时更新媒体流列表
        zlmresTfulUtils.getMediaList(mediaServerItem, (mediaList ->{
            if (mediaList == null) return;
            String dataStr = mediaList.getString("data");

            Integer code = mediaList.getInteger("code");
            Map<String, StreamPushItem> result = new HashMap<>();
            List<StreamPushItem> streamPushItems = null;
            // 获取所有的国标关联
//            List<GbStream> gbStreams = gbStreamMapper.selectAllByMediaServerId(mediaServerItem.getId());
            if (code == 0 ) {
                if (dataStr != null) {
                    streamPushItems = streamPushService.handleJSON(dataStr, mediaServerItem);
                }
            }else {
                logger.warn("更新视频流失败，错误code： " + code);
            }

            if (streamPushItems != null) {
                storager.updateMediaList(streamPushItems);
                for (StreamPushItem streamPushItem : streamPushItems) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("app", streamPushItem.getApp());
                    jsonObject.put("stream", streamPushItem.getStream());
                    jsonObject.put("mediaServerId", mediaServerItem.getId());
                    subscribe.addSubscribe(ZLMHttpHookSubscribe.HookType.on_play,jsonObject,
                            (MediaServerItem mediaServerItemInuse, JSONObject response)->{
                                updateMedia(mediaServerItem, response.getString("app"), response.getString("stream"));
                            }
                    );
                }
            }
        }));

    }

    public void addMedia(MediaServerItem mediaServerItem, String app, String streamId) {
        //使用异步更新推流
        updateMedia(mediaServerItem, app, streamId);
    }


    public void updateMedia(MediaServerItem mediaServerItem, String app, String streamId) {
        //使用异步更新推流
        zlmresTfulUtils.getMediaList(mediaServerItem, app, streamId, "rtmp", json->{

            if (json == null) return;
            String dataStr = json.getString("data");

            Integer code = json.getInteger("code");
            Map<String, StreamPushItem> result = new HashMap<>();
            List<StreamPushItem> streamPushItems = null;
            if (code == 0 ) {
                if (dataStr != null) {
                    streamPushItems = streamPushService.handleJSON(dataStr, mediaServerItem);
                }
            }else {
                logger.warn("更新视频流失败，错误code： " + code);
            }

            if (streamPushItems != null && streamPushItems.size() == 1) {
                storager.updateMedia(streamPushItems.get(0));
            }
        });
    }


    public void removeMedia(String app, String streamId) {
        // 查找是否关联了国标， 关联了不删除， 置为离线
        StreamProxyItem streamProxyItem = gbStreamMapper.selectOne(app, streamId);
        if (streamProxyItem == null) {
            storager.removeMedia(app, streamId);
        }else {
            storager.mediaOutline(app, streamId);
        }
    }

//    public void clearAllSessions() {
//        logger.info("清空所有国标相关的session");
//        JSONObject allSessionJSON = zlmresTfulUtils.getAllSession();
//        ZLMServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
//        HashSet<String> allLocalPorts = new HashSet();
//        if (allSessionJSON.getInteger("code") == 0) {
//            JSONArray data = allSessionJSON.getJSONArray("data");
//            if (data.size() > 0) {
//                for (int i = 0; i < data.size(); i++) {
//                    JSONObject sessionJOSN = data.getJSONObject(i);
//                    Integer local_port = sessionJOSN.getInteger("local_port");
//                    if (!local_port.equals(Integer.valueOf(mediaInfo.getHttpPort())) &&
//                        !local_port.equals(Integer.valueOf(mediaInfo.getHttpSSLport())) &&
//                        !local_port.equals(Integer.valueOf(mediaInfo.getRtmpPort())) &&
//                        !local_port.equals(Integer.valueOf(mediaInfo.getRtspPort())) &&
//                        !local_port.equals(Integer.valueOf(mediaInfo.getRtspSSlport())) &&
//                        !local_port.equals(Integer.valueOf(mediaInfo.getHookOnFlowReport()))){
//                        allLocalPorts.add(sessionJOSN.getInteger("local_port") + "");
//                     }
//                }
//            }
//        }
//        if (allLocalPorts.size() > 0) {
//            List<String> result = new ArrayList<>(allLocalPorts);
//            String localPortSStr = String.join(",", result);
//            zlmresTfulUtils.kickSessions(localPortSStr);
//        }
//    }
}
