package com.genersoft.iot.vmp.media.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.event.MediaServerChangeEvent;
import com.genersoft.iot.vmp.media.event.MediaServerDeleteEvent;
import com.genersoft.iot.vmp.media.service.IMediaNodeServerService;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServer;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.service.bean.MediaServerLoad;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.MediaServerMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.JsonUtil;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 媒体服务器节点管理
 */
@Service
@DS("master")
public class MediaServerServiceImpl implements IMediaServerService {

    private final static Logger logger = LoggerFactory.getLogger(MediaServerServiceImpl.class);

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private MediaServerMapper mediaServerMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private Map<String, IMediaNodeServerService> nodeServerServiceMap;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;


    /**
     * 初始化
     */
    @Override
    public void updateVmServer(List<MediaServer> mediaServerItemList) {
        logger.info("[媒体服务节点] 缓存初始化 ");
        for (MediaServer mediaServerItem : mediaServerItemList) {
            if (ObjectUtils.isEmpty(mediaServerItem.getId())) {
                continue;
            }
            // 更新
            if (!ssrcFactory.hasMediaServerSSRC(mediaServerItem.getId())) {
                ssrcFactory.initMediaServerSSRC(mediaServerItem.getId(), null);
            }
            // 查询redis是否存在此mediaServer
            String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId() + "_" + mediaServerItem.getId();
            Boolean hasKey = redisTemplate.hasKey(key);
            if (hasKey != null && ! hasKey) {
                redisTemplate.opsForValue().set(key, mediaServerItem);
            }
        }
    }


    @Override
    public SSRCInfo openRTPServer(MediaServer mediaServerItem, String streamId, String presetSsrc, boolean ssrcCheck,
                                  boolean isPlayback, Integer port, Boolean onlyAuto, Boolean reUsePort, Integer tcpMode) {
        if (mediaServerItem == null || mediaServerItem.getId() == null) {
            logger.info("[openRTPServer] 失败, mediaServerItem == null || mediaServerItem.getId() == null");
            return null;
        }
        // 获取mediaServer可用的ssrc
        String ssrc;
        if (presetSsrc != null) {
            ssrc = presetSsrc;
        }else {
            if (isPlayback) {
                ssrc = ssrcFactory.getPlayBackSsrc(mediaServerItem.getId());
            }else {
                ssrc = ssrcFactory.getPlaySsrc(mediaServerItem.getId());
            }
        }

        if (streamId == null) {
            streamId = String.format("%08x", Long.parseLong(ssrc)).toUpperCase();
        }
        if (ssrcCheck && tcpMode > 0) {
            // 目前zlm不支持 tcp模式更新ssrc，暂时关闭ssrc校验
            logger.warn("[openRTPServer] 平台对接时下级可能自定义ssrc，但是tcp模式zlm收流目前无法更新ssrc，可能收流超时，此时请使用udp收流或者关闭ssrc校验");
        }
        int rtpServerPort;
        if (mediaServerItem.isRtpEnable()) {
            IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
            if (mediaNodeServerService == null) {
                logger.info("[openRTPServer] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaServerItem.getType());
                return null;
            }
            rtpServerPort = mediaNodeServerService.createRTPServer(mediaServerItem, streamId, ssrcCheck ? Long.parseLong(ssrc) : 0, port, onlyAuto, reUsePort, tcpMode);
        } else {
            rtpServerPort = mediaServerItem.getRtpProxyPort();
        }
        return new SSRCInfo(rtpServerPort, ssrc, streamId);
    }

    @Override
    public SSRCInfo openRTPServer(MediaServer mediaServerItem, String streamId, String ssrc, boolean ssrcCheck, boolean isPlayback, Integer port, Boolean onlyAuto) {
        return openRTPServer(mediaServerItem, streamId, ssrc, ssrcCheck, isPlayback, port, onlyAuto, null, 0);
    }


    @Override
    public void closeRTPServer(MediaServer mediaServerItem, String streamId) {
        if (mediaServerItem == null) {
            return;
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            logger.info("[closeRTPServer] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaServerItem.getType());
            return;
        }
        mediaNodeServerService.closeRtpServer(mediaServerItem, streamId);
    }

    @Override
    public void closeRTPServer(MediaServer mediaServerItem, String streamId, CommonCallback<Boolean> callback) {
        if (mediaServerItem == null) {
            callback.run(false);
            return;
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            logger.info("[closeRTPServer] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaServerItem.getType());
            return;
        }
        mediaNodeServerService.closeRtpServer(mediaServerItem, streamId, callback);
    }

    @Override
    public void closeRTPServer(String mediaServerId, String streamId) {
        MediaServer mediaServerItem = this.getOne(mediaServerId);
        if (mediaServerItem == null) {
            return;
        }
        if (mediaServerItem.isRtpEnable()) {
            closeRTPServer(mediaServerItem, streamId);
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            logger.info("[closeRTPServer] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaServerItem.getType());
            return;
        }
        mediaNodeServerService.closeStreams(mediaServerItem, "rtp", streamId);
    }

    @Override
    public Boolean updateRtpServerSSRC(MediaServer mediaServerItem, String streamId, String ssrc) {
        if (mediaServerItem == null) {
            return false;
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            logger.info("[updateRtpServerSSRC] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaServerItem.getType());
            return false;
        }
        return mediaNodeServerService.updateRtpServerSSRC(mediaServerItem, streamId, ssrc);
    }

    @Override
    public void releaseSsrc(String mediaServerItemId, String ssrc) {
        MediaServer mediaServerItem = getOne(mediaServerItemId);
        if (mediaServerItem == null || ssrc == null) {
            return;
        }
        ssrcFactory.releaseSsrc(mediaServerItemId, ssrc);
    }

    /**
     * 媒体服务节点 重启后重置他的推流信息， TODO 给正在使用的设备发送停止命令
     */
    @Override
    public void clearRTPServer(MediaServer mediaServerItem) {
        ssrcFactory.reset(mediaServerItem.getId());
    }


    @Override
    public void update(MediaServer mediaSerItem) {
        mediaServerMapper.update(mediaSerItem);
        MediaServer mediaServerInRedis = getOne(mediaSerItem.getId());
        MediaServer mediaServerItemInDataBase = mediaServerMapper.queryOne(mediaSerItem.getId());
        if (mediaServerItemInDataBase == null) {
            return;
        }
        mediaServerItemInDataBase.setStatus(mediaSerItem.isStatus());
        if (mediaServerInRedis == null || !ssrcFactory.hasMediaServerSSRC(mediaServerItemInDataBase.getId())) {
            ssrcFactory.initMediaServerSSRC(mediaServerItemInDataBase.getId(),null);
        }
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId() + "_" + mediaServerItemInDataBase.getId();
        redisTemplate.opsForValue().set(key, mediaServerItemInDataBase);
        if (mediaServerItemInDataBase.isStatus()) {
            resetOnlineServerItem(mediaServerItemInDataBase);
        }else {
            // 发送事件
            MediaServerChangeEvent event = new MediaServerChangeEvent(this);
            event.setMediaServerItemList(mediaServerItemInDataBase);
            applicationEventPublisher.publishEvent(event);
        }
    }


    @Override
    public List<MediaServer> getAllOnlineList() {
        List<MediaServer> result = new ArrayList<>();
        List<Object> mediaServerKeys = RedisUtil.scan(redisTemplate, String.format("%S*", VideoManagerConstants.MEDIA_SERVER_PREFIX+ userSetting.getServerId() + "_" ));
        String onlineKey = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetting.getServerId();
        for (Object mediaServerKey : mediaServerKeys) {
            String key = (String) mediaServerKey;
            MediaServer mediaServerItem = JsonUtil.redisJsonToObject(redisTemplate, key, MediaServer.class);
            if (Objects.isNull(mediaServerItem)) {
                continue;
            }
            // 检查状态
            Double aDouble = redisTemplate.opsForZSet().score(onlineKey, mediaServerItem.getId());
            if (aDouble != null) {
                mediaServerItem.setStatus(true);
            }
            result.add(mediaServerItem);
        }
        result.sort((serverItem1, serverItem2)->{
            int sortResult = 0;
            LocalDateTime localDateTime1 = LocalDateTime.parse(serverItem1.getCreateTime(), DateUtil.formatter);
            LocalDateTime localDateTime2 = LocalDateTime.parse(serverItem2.getCreateTime(), DateUtil.formatter);

            sortResult = localDateTime1.compareTo(localDateTime2);
            return  sortResult;
        });
        return result;
    }

    @Override
    public List<MediaServer> getAll() {
        List<MediaServer> mediaServerList = mediaServerMapper.queryAll();
        if (mediaServerList.isEmpty()) {
            return new ArrayList<>();
        }
        for (MediaServer mediaServerItem : mediaServerList) {
            MediaServer mediaServerItemInRedis = getOne(mediaServerItem.getId());
            if (mediaServerItemInRedis != null) {
                mediaServerItem.setStatus(mediaServerItemInRedis.isStatus());
            }
        }
        return mediaServerList;
    }


    @Override
    public List<MediaServer> getAllFromDatabase() {
        return mediaServerMapper.queryAll();
    }

    @Override
    public List<MediaServer> getAllOnline() {
        String key = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetting.getServerId();
        Set<Object> mediaServerIdSet = redisTemplate.opsForZSet().reverseRange(key, 0, -1);

        List<MediaServer> result = new ArrayList<>();
        if (mediaServerIdSet != null && mediaServerIdSet.size() > 0) {
            for (Object mediaServerId : mediaServerIdSet) {
                String mediaServerIdStr = (String) mediaServerId;
                String serverKey = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId() + "_" + mediaServerIdStr;
                result.add((MediaServer) redisTemplate.opsForValue().get(serverKey));
            }
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * 获取单个媒体服务节点服务器
     * @param mediaServerId 服务id
     * @return MediaServerItem
     */
    @Override
    public MediaServer getOne(String mediaServerId) {
        if (mediaServerId == null) {
            return null;
        }
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId() + "_" + mediaServerId;
        return JsonUtil.redisJsonToObject(redisTemplate, key, MediaServer.class);
    }


    @Override
    public MediaServer getDefaultMediaServer() {
        return mediaServerMapper.queryDefault();
    }

    @Override
    public void clearMediaServerForOnline() {
        String key = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetting.getServerId();
        redisTemplate.delete(key);
    }

    @Override
    public void add(MediaServer mediaServerItem) {
        mediaServerItem.setCreateTime(DateUtil.getNow());
        mediaServerItem.setUpdateTime(DateUtil.getNow());
        if (mediaServerItem.getHookAliveInterval() == null || mediaServerItem.getHookAliveInterval() == 0F) {
            mediaServerItem.setHookAliveInterval(10F);
        }
        if (mediaServerItem.getType() == null) {
            logger.info("[添加媒体节点] 失败, mediaServerItem的类型：为空");
            return;
        }
        if (mediaServerMapper.queryOne(mediaServerItem.getId()) != null) {
            logger.info("[添加媒体节点] 失败, 媒体服务ID已存在，请修改媒体服务器配置, {}", mediaServerItem.getId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"保存失败，媒体服务ID [ " + mediaServerItem.getId() + " ] 已存在，请修改媒体服务器配置");
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            logger.info("[添加媒体节点] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaServerItem.getType());
            return;
        }
        mediaServerMapper.add(mediaServerItem);
        if (mediaServerItem.isStatus()) {
            mediaNodeServerService.online(mediaServerItem);
        }else {
            // 发送事件
            MediaServerChangeEvent event = new MediaServerChangeEvent(this);
            event.setMediaServerItemList(mediaServerItem);
            applicationEventPublisher.publishEvent(event);
        }
    }

    @Override
    public void resetOnlineServerItem(MediaServer serverItem) {
        // 更新缓存
        String key = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetting.getServerId();
        // 使用zset的分数作为当前并发量， 默认值设置为0
        if (redisTemplate.opsForZSet().score(key, serverItem.getId()) == null) {  // 不存在则设置默认值 已存在则重置
            redisTemplate.opsForZSet().add(key, serverItem.getId(), 0L);
            // 查询服务流数量
            int count = getMediaList(serverItem);
            redisTemplate.opsForZSet().add(key, serverItem.getId(), count);
        }else {
            clearRTPServer(serverItem);
        }
    }

    private int getMediaList(MediaServer serverItem) {

        return 0;
    }


    @Override
    public void addCount(String mediaServerId) {
        if (mediaServerId == null) {
            return;
        }
        String key = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetting.getServerId();
        redisTemplate.opsForZSet().incrementScore(key, mediaServerId, 1);

    }

    @Override
    public void removeCount(String mediaServerId) {
        String key = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetting.getServerId();
        redisTemplate.opsForZSet().incrementScore(key, mediaServerId, - 1);
    }

    /**
     * 获取负载最低的节点
     * @return MediaServerItem
     */
    @Override
    public MediaServer getMediaServerForMinimumLoad(Boolean hasAssist) {
        String key = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetting.getServerId();
        Long size = redisTemplate.opsForZSet().zCard(key);
        if (size  == null || size == 0) {
            logger.info("获取负载最低的节点时无在线节点");
            return null;
        }

        // 获取分数最低的，及并发最低的
        Set<Object> objects = redisTemplate.opsForZSet().range(key, 0, -1);
        ArrayList<Object> mediaServerObjectS = new ArrayList<>(objects);
        MediaServer mediaServerItem = null;
        if (hasAssist == null) {
            String mediaServerId = (String)mediaServerObjectS.get(0);
            mediaServerItem = getOne(mediaServerId);
        }else if (hasAssist) {
            for (Object mediaServerObject : mediaServerObjectS) {
                String mediaServerId = (String)mediaServerObject;
                MediaServer serverItem = getOne(mediaServerId);
                if (serverItem.getRecordAssistPort() > 0) {
                    mediaServerItem = serverItem;
                    break;
                }
            }
        }else if (!hasAssist) {
            for (Object mediaServerObject : mediaServerObjectS) {
                String mediaServerId = (String)mediaServerObject;
                MediaServer serverItem = getOne(mediaServerId);
                if (serverItem.getRecordAssistPort() == 0) {
                    mediaServerItem = serverItem;
                    break;
                }
            }
        }

        return mediaServerItem;
    }

    @Override
    public MediaServer checkMediaServer(String ip, int port, String secret, String type) {
        if (mediaServerMapper.queryOneByHostAndPort(ip, port) != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "此连接已存在");
        }

        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(type);
        if (mediaNodeServerService == null) {
            logger.info("[closeRTPServer] 失败, mediaServerItem的类型： {}，未找到对应的实现类", type);
            return null;
        }
        MediaServer mediaServerItem = mediaNodeServerService.checkMediaServer(ip, port, secret);
        if (mediaServerItem != null) {
            if (mediaServerMapper.queryOne(mediaServerItem.getId()) != null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "媒体服务ID [" + mediaServerItem.getId() + " ] 已存在，请修改媒体服务器配置");
            }
        }
        return mediaServerItem;
    }

    @Override
    public boolean checkMediaRecordServer(String ip, int port) {
        boolean result = false;
        OkHttpClient client = new OkHttpClient();
        String url = String.format("http://%s:%s/index/api/record",  ip, port);
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response != null) {
                result = true;
            }
        } catch (Exception e) {}

        return result;
    }

    @Override
    public void delete(String id) {
        mediaServerMapper.delOne(id);
        redisTemplate.opsForZSet().remove(VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetting.getServerId(), id);
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId() + "_" + id;
        redisTemplate.delete(key);
        // 发送节点移除通知
        MediaServerDeleteEvent event = new MediaServerDeleteEvent(this);
        event.setMediaServerId(id);
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public MediaServer getOneFromDatabase(String mediaServerId) {
        return mediaServerMapper.queryOne(mediaServerId);
    }

    @Override
    public void syncCatchFromDatabase() {
        List<MediaServer> allInCatch = getAllOnlineList();
        List<MediaServer> allInDatabase = mediaServerMapper.queryAll();
        Map<String, MediaServer> mediaServerItemMap = new HashMap<>();

        for (MediaServer mediaServerItem : allInDatabase) {
            mediaServerItemMap.put(mediaServerItem.getId(), mediaServerItem);
        }
        for (MediaServer mediaServerItem : allInCatch) {
            // 清除数据中不存在但redis缓存数据
            if (!mediaServerItemMap.containsKey(mediaServerItem.getId())) {
                delete(mediaServerItem.getId());
            }
        }
    }

    @Override
    public MediaServerLoad getLoad(MediaServer mediaServerItem) {
        MediaServerLoad result = new MediaServerLoad();
        result.setId(mediaServerItem.getId());
        result.setPush(redisCatchStorage.getPushStreamCount(mediaServerItem.getId()));
        result.setProxy(redisCatchStorage.getProxyStreamCount(mediaServerItem.getId()));

        result.setGbReceive(inviteStreamService.getStreamInfoCount(mediaServerItem.getId()));
        result.setGbSend(redisCatchStorage.getGbSendCount(mediaServerItem.getId()));
        return result;
    }

    @Override
    public List<MediaServer> getAllWithAssistPort() {
        return mediaServerMapper.queryAllWithAssistPort();
    }


    @Override
    public boolean stopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaInfo.getType());
        if (mediaNodeServerService == null) {
            logger.info("[stopSendRtp] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaInfo.getType());
            return false;
        }
        return mediaNodeServerService.stopSendRtp(mediaInfo, app, stream, ssrc);
    }

    @Override
    public boolean deleteRecordDirectory(MediaServer mediaServerItem, String app, String stream, String date, String fileName) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            logger.info("[stopSendRtp] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaServerItem.getType());
            return false;
        }
        return mediaNodeServerService.deleteRecordDirectory(mediaServerItem, app, stream, date, fileName);
    }

    @Override
    public List<StreamInfo> getMediaList(MediaServer mediaServerItem, String app, String stream, String callId) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            logger.info("[getMediaList] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaServerItem.getType());
            return new ArrayList<>();
        }
        return mediaNodeServerService.getMediaList(mediaServerItem, app, stream, callId);
    }

    @Override
    public Boolean connectRtpServer(MediaServer mediaServerItem, String address, int port, String stream) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            logger.info("[connectRtpServer] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaServerItem.getType());
            return false;
        }
        return mediaNodeServerService.connectRtpServer(mediaServerItem, address, port, stream);
    }

    @Override
    public void getSnap(MediaServer mediaServerItem, String streamUrl, int timeoutSec, int expireSec, String path, String fileName) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            logger.info("[getSnap] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaServerItem.getType());
            return;
        }
        mediaNodeServerService.getSnap(mediaServerItem, streamUrl, timeoutSec, expireSec, path, fileName);
    }

    @Override
    public MediaInfo getMediaInfo(MediaServer mediaServerItem, String app, String stream) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            logger.info("[getMediaInfo] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaServerItem.getType());
            return null;
        }
        return mediaNodeServerService.getMediaInfo(mediaServerItem, app, stream);
    }

    @Override
    public Boolean pauseRtpCheck(MediaServer mediaServerItem, String streamKey) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            logger.info("[pauseRtpCheck] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaServerItem.getType());
            return false;
        }
        return mediaNodeServerService.pauseRtpCheck(mediaServerItem, streamKey);
    }

    @Override
    public boolean resumeRtpCheck(MediaServer mediaServerItem, String streamKey) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            logger.info("[pauseRtpCheck] 失败, mediaServerItem的类型： {}，未找到对应的实现类", mediaServerItem.getType());
            return false;
        }
        return mediaNodeServerService.resumeRtpCheck(mediaServerItem, streamKey);
    }
}
