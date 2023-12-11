package com.genersoft.iot.vmp.media.zlm;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IResourceService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.storager.dao.StreamPushMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lin
 */
@Component
public class ZLMMediaListManager {

    private Logger logger = LoggerFactory.getLogger("ZLMMediaListManager");

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private Map<String, IResourceService> resourceServiceMap;

    @Autowired
    private StreamPushMapper streamPushMapper;

    @Autowired
    private ZlmHttpHookSubscribe subscribe;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Autowired
    private IMediaServerService mediaServerService;

    private Map<String, ChannelOnlineEvent> channelOnPublishEvents = new ConcurrentHashMap<>();

    public StreamPush addPush(OnStreamChangedHookParam onStreamChangedHookParam) {
        StreamPush transform = streamPushService.transform(onStreamChangedHookParam);
        StreamPush pushInDb = streamPushService.getPush(onStreamChangedHookParam.getApp(), onStreamChangedHookParam.getStream());
        
        if (pushInDb == null) {
            
            streamPushService.add(transform);
        }else {
            pushInDb.setPushIng(onStreamChangedHookParam.isRegist());
            pushInDb.setUpdateTime(DateUtil.getNow());
            pushInDb.setPushTime(DateUtil.getNow());
            pushInDb.setSelf(userSetting.getServerId().equals(onStreamChangedHookParam.getSeverId()));
            streamPushService.update(pushInDb);
        }
        ChannelOnlineEvent channelOnlineEventLister = getChannelOnlineEventLister(transform.getApp(), transform.getStream());
        if ( channelOnlineEventLister != null)  {
            try {
                channelOnlineEventLister.run(transform.getApp(), transform.getStream(), transform.getServerId());;
            } catch (ParseException e) {
                logger.error("addPush: ", e);
            }
            removedChannelOnlineEventLister(transform.getApp(), transform.getStream());
        }
        return transform;
    }

    public void sendStreamEvent(String app, String stream, String mediaServerId) {
        MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
        // 查看推流状态
        Boolean streamReady = zlmServerFactory.isStreamReady(mediaServerItem, app, stream);
        if (streamReady != null && streamReady) {
            ChannelOnlineEvent channelOnlineEventLister = getChannelOnlineEventLister(app, stream);
            if (channelOnlineEventLister != null)  {
                try {
                    channelOnlineEventLister.run(app, stream, mediaServerId);
                } catch (ParseException e) {
                    logger.error("sendStreamEvent: ", e);
                }
                removedChannelOnlineEventLister(app, stream);
            }
        }
    }

    public void streamOffline(String app, String streamId) {
        for (String key : resourceServiceMap.keySet()) {
            resourceServiceMap.get(key).streamOffline(app, streamId);
        }
    }

    public void addChannelOnlineEventLister(String app, String stream, ChannelOnlineEvent callback) {
        this.channelOnPublishEvents.put(app + "_" + stream, callback);
    }

    public void removedChannelOnlineEventLister(String app, String stream) {
        this.channelOnPublishEvents.remove(app + "_" + stream);
    }

    public ChannelOnlineEvent getChannelOnlineEventLister(String app, String stream) {
        return this.channelOnPublishEvents.get(app + "_" + stream);
    }

}
