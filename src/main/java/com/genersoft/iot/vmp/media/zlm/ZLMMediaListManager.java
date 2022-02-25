package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.dto.MediaItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.bean.ThirdPartyGB;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.StreamPushMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private IStreamProxyService streamProxyService;

    @Autowired
    private StreamPushMapper streamPushMapper;

    @Autowired
    private ZLMHttpHookSubscribe subscribe;

    @Autowired
    private UserSetup userSetup;


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

    public StreamPushItem addPush(MediaItem mediaItem) {
        // 查找此直播流是否存在redis预设gbId
        StreamPushItem transform = streamPushService.transform(mediaItem);
        // 从streamId取出查询关键值
        Pattern pattern = Pattern.compile(userSetup.getThirdPartyGBIdReg());
        Matcher matcher = pattern.matcher(mediaItem.getStream());// 指定要匹配的字符串
        String queryKey = null;
        if (matcher.find()) { //此处find（）每次被调用后，会偏移到下一个匹配
            queryKey = matcher.group();
        }
        if (queryKey != null) {
            ThirdPartyGB thirdPartyGB = redisCatchStorage.queryMemberNoGBId(queryKey);
            if (thirdPartyGB != null && !StringUtils.isEmpty(thirdPartyGB.getNationalStandardNo())) {
                transform.setGbId(thirdPartyGB.getNationalStandardNo());
                transform.setName(thirdPartyGB.getName());
            }
        }
        if (!StringUtils.isEmpty(transform.getGbId())) {
            // 如果这个国标ID已经给了其他推流且流已离线，则移除其他推流
            List<GbStream> gbStreams = gbStreamMapper.selectByGBId(transform.getGbId());
            if (gbStreams.size() > 0) {
                for (GbStream gbStream : gbStreams) {
                    // 出现使用相同国标Id的视频流时，使用新流替换旧流，
                    gbStreamMapper.del(gbStream.getApp(), gbStream.getStream());
                    if (!gbStream.isStatus()) {
                        streamPushMapper.del(gbStream.getApp(), gbStream.getStream());
                    }
                }
            }
            StreamProxyItem streamProxyItem = gbStreamMapper.selectOne(transform.getApp(), transform.getStream());
            if (streamProxyItem != null) {
                transform.setGbStreamId(streamProxyItem.getGbStreamId());
                gbStreamMapper.update(transform);
            }else {
                transform.setCreateStamp(System.currentTimeMillis());
                gbStreamMapper.add(transform);
            }
        }
        storager.updateMedia(transform);
        return transform;
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


    public int removeMedia(String app, String streamId) {
        // 查找是否关联了国标， 关联了不删除， 置为离线
        StreamProxyItem streamProxyItem = gbStreamMapper.selectOne(app, streamId);
        int result = 0;
        if (streamProxyItem == null) {
            result = storager.removeMedia(app, streamId);
        }else {
            result =storager.mediaOutline(app, streamId);
        }
        return result;
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
