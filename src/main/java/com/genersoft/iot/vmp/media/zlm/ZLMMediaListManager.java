package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.bean.ThirdPartyGB;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.StreamPushMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.checkerframework.checker.units.qual.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private UserSetting userSetting;

    private Map<String, ChannelOnlineEvent> channelOnlineEvents = new ConcurrentHashMap<>();

    public StreamPushItem addPush(MediaItem mediaItem) {
        // 查找此直播流是否存在redis预设gbId
        StreamPushItem transform = streamPushService.transform(mediaItem);
        StreamPushItem pushInDb = streamPushService.getPush(mediaItem.getApp(), mediaItem.getStream());
        transform.setPushIng(mediaItem.isRegist());
        transform.setUpdateTime(DateUtil.getNow());
        transform.setPushTime(DateUtil.getNow());
        transform.setSelf(userSetting.getServerId().equals(mediaItem.getSeverId()));
        if (pushInDb == null) {
            transform.setCreateTime(DateUtil.getNow());
            streamPushMapper.add(transform);
        }else {
            streamPushMapper.update(transform);
        }
        return transform;
    }

    public int removeMedia(String app, String streamId) {
        // 查找是否关联了国标， 关联了不删除， 置为离线
        GbStream gbStream = gbStreamMapper.selectOne(app, streamId);
        int result;
        if (gbStream == null) {
            result = storager.removeMedia(app, streamId);
        }else {
            // TODO 暂不设置为离线
            result =storager.mediaOffline(app, streamId);
        }
        return result;
    }

    public void addChannelOnlineEventLister(String key, ChannelOnlineEvent callback) {
        this.channelOnlineEvents.put(key,callback);
    }

    public void removedChannelOnlineEventLister(String key) {
        this.channelOnlineEvents.remove(key);
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
