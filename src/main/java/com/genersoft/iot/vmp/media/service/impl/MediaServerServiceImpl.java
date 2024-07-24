package com.genersoft.iot.vmp.media.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerChangeEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerDeleteEvent;
import com.genersoft.iot.vmp.media.service.IMediaNodeServerService;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.SendRtpPortManager;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OriginType;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.service.bean.MediaServerLoad;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.MediaServerMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.JsonUtil;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
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

    @Autowired
    private MediaConfig mediaConfig;

    @Autowired
    private SendRtpPortManager sendRtpPortManager;



    /**
     * 流到来的处理
     */
    @Async("taskExecutor")
    @org.springframework.context.event.EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {
        if ("rtsp".equals(event.getSchema())) {
            logger.info("流变化：注册 app->{}, stream->{}", event.getApp(), event.getStream());
            addCount(event.getMediaServer().getId());
            String type = OriginType.values()[event.getMediaInfo().getOriginType()].getType();
            redisCatchStorage.addStream(event.getMediaServer(), type, event.getApp(), event.getStream(), event.getMediaInfo());
        }
    }

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        if ("rtsp".equals(event.getSchema())) {
            logger.info("流变化：注销, app->{}, stream->{}", event.getApp(), event.getStream());
            removeCount(event.getMediaServer().getId());
            MediaInfo mediaInfo = redisCatchStorage.getStreamInfo(
                    event.getApp(), event.getStream(), event.getMediaServer().getId());
            if (mediaInfo == null) {
                return;
            }
            String type = OriginType.values()[mediaInfo.getOriginType()].getType();
            redisCatchStorage.removeStream(mediaInfo.getMediaServer().getId(), type, event.getApp(), event.getStream());
        }

    }


    /**
     * 初始化
     */
    @Override
    public void updateVmServer(List<MediaServer> mediaServerList) {
        logger.info("[媒体服务节点] 缓存初始化 ");
        for (MediaServer mediaServer : mediaServerList) {
            if (ObjectUtils.isEmpty(mediaServer.getId())) {
                continue;
            }
            // 更新
            if (!ssrcFactory.hasMediaServerSSRC(mediaServer.getId())) {
                ssrcFactory.initMediaServerSSRC(mediaServer.getId(), null);
            }
            // 查询redis是否存在此mediaServer
            String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId() + ":" + mediaServer.getId();
            Boolean hasKey = redisTemplate.hasKey(key);
            if (hasKey != null && ! hasKey) {
                redisTemplate.opsForValue().set(key, mediaServer);
            }
        }
    }


    @Override
    public SSRCInfo openRTPServer(MediaServer mediaServer, String streamId, String presetSsrc, boolean ssrcCheck,
                                  boolean isPlayback, Integer port, Boolean onlyAuto, Boolean disableAudio, Boolean reUsePort, Integer tcpMode) {
        if (mediaServer == null || mediaServer.getId() == null) {
            logger.info("[openRTPServer] 失败, mediaServer == null || mediaServer.getId() == null");
            return null;
        }
        // 获取mediaServer可用的ssrc
        String ssrc;
        if (presetSsrc != null) {
            ssrc = presetSsrc;
        }else {
            if (isPlayback) {
                ssrc = ssrcFactory.getPlayBackSsrc(mediaServer.getId());
            }else {
                ssrc = ssrcFactory.getPlaySsrc(mediaServer.getId());
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
        if (mediaServer.isRtpEnable()) {
            IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
            if (mediaNodeServerService == null) {
                logger.info("[openRTPServer] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
                return null;
            }
            rtpServerPort = mediaNodeServerService.createRTPServer(mediaServer, streamId, ssrcCheck ? Long.parseLong(ssrc) : 0, port, onlyAuto, disableAudio, reUsePort, tcpMode);
        } else {
            rtpServerPort = mediaServer.getRtpProxyPort();
        }
        return new SSRCInfo(rtpServerPort, ssrc, streamId);
    }

    @Override
    public void closeRTPServer(MediaServer mediaServer, String streamId) {
        if (mediaServer == null) {
            return;
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[closeRTPServer] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return;
        }
        mediaNodeServerService.closeRtpServer(mediaServer, streamId);
    }

    @Override
    public void closeRTPServer(MediaServer mediaServer, String streamId, CommonCallback<Boolean> callback) {
        if (mediaServer == null) {
            callback.run(false);
            return;
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[closeRTPServer] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return;
        }
        mediaNodeServerService.closeRtpServer(mediaServer, streamId, callback);
    }

    @Override
    public void closeRTPServer(String mediaServerId, String streamId) {
        MediaServer mediaServer = this.getOne(mediaServerId);
        if (mediaServer == null) {
            return;
        }
        if (mediaServer.isRtpEnable()) {
            closeRTPServer(mediaServer, streamId);
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[closeRTPServer] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return;
        }
        mediaNodeServerService.closeStreams(mediaServer, "rtp", streamId);
    }

    @Override
    public Boolean updateRtpServerSSRC(MediaServer mediaServer, String streamId, String ssrc) {
        if (mediaServer == null) {
            return false;
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[updateRtpServerSSRC] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.updateRtpServerSSRC(mediaServer, streamId, ssrc);
    }

    @Override
    public void releaseSsrc(String mediaServerId, String ssrc) {
        MediaServer mediaServer = getOne(mediaServerId);
        if (mediaServer == null || ssrc == null) {
            return;
        }
        ssrcFactory.releaseSsrc(mediaServerId, ssrc);
    }

    /**
     * 媒体服务节点 重启后重置他的推流信息， TODO 给正在使用的设备发送停止命令
     */
    @Override
    public void clearRTPServer(MediaServer mediaServer) {
        ssrcFactory.reset(mediaServer.getId());
    }


    @Override
    public void update(MediaServer mediaSerItem) {
        mediaServerMapper.update(mediaSerItem);
        MediaServer mediaServerInRedis = getOne(mediaSerItem.getId());
        MediaServer mediaServerInDataBase = mediaServerMapper.queryOne(mediaSerItem.getId());
        if (mediaServerInDataBase == null) {
            return;
        }
        mediaServerInDataBase.setStatus(mediaSerItem.isStatus());
        if (mediaServerInRedis == null || !ssrcFactory.hasMediaServerSSRC(mediaServerInDataBase.getId())) {
            ssrcFactory.initMediaServerSSRC(mediaServerInDataBase.getId(),null);
        }
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId() + ":" + mediaServerInDataBase.getId();
        redisTemplate.opsForValue().set(key, mediaServerInDataBase);
        if (mediaServerInDataBase.isStatus()) {
            resetOnlineServerItem(mediaServerInDataBase);
        }else {
            // 发送事件
            MediaServerChangeEvent event = new MediaServerChangeEvent(this);
            event.setMediaServerItemList(mediaServerInDataBase);
            applicationEventPublisher.publishEvent(event);
        }
    }


    @Override
    public List<MediaServer> getAllOnlineList() {
        List<MediaServer> result = new ArrayList<>();
        List<Object> mediaServerKeys = RedisUtil.scan(redisTemplate, String.format("%S*", VideoManagerConstants.MEDIA_SERVER_PREFIX+ userSetting.getServerId() + ":" ));
        String onlineKey = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
        for (Object mediaServerKey : mediaServerKeys) {
            String key = (String) mediaServerKey;
            MediaServer mediaServer = JsonUtil.redisJsonToObject(redisTemplate, key, MediaServer.class);
            if (Objects.isNull(mediaServer)) {
                continue;
            }
            // 检查状态
            Double aDouble = redisTemplate.opsForZSet().score(onlineKey, mediaServer.getId());
            if (aDouble != null) {
                mediaServer.setStatus(true);
            }
            result.add(mediaServer);
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
        for (MediaServer mediaServer : mediaServerList) {
            MediaServer mediaServerInRedis = getOne(mediaServer.getId());
            if (mediaServerInRedis != null) {
                mediaServer.setStatus(mediaServerInRedis.isStatus());
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
        String key = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
        Set<Object> mediaServerIdSet = redisTemplate.opsForZSet().reverseRange(key, 0, -1);

        List<MediaServer> result = new ArrayList<>();
        if (mediaServerIdSet != null && mediaServerIdSet.size() > 0) {
            for (Object mediaServerId : mediaServerIdSet) {
                String mediaServerIdStr = (String) mediaServerId;
                String serverKey = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId() + ":" + mediaServerIdStr;
                result.add((MediaServer) redisTemplate.opsForValue().get(serverKey));
            }
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * 获取单个媒体服务节点服务器
     * @param mediaServerId 服务id
     * @return mediaServer
     */
    @Override
    public MediaServer getOne(String mediaServerId) {
        if (mediaServerId == null) {
            return null;
        }
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId() + ":" + mediaServerId;
        return JsonUtil.redisJsonToObject(redisTemplate, key, MediaServer.class);
    }


    @Override
    public MediaServer getDefaultMediaServer() {
        return mediaServerMapper.queryDefault();
    }

    @Override
    public void clearMediaServerForOnline() {
        String key = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
        redisTemplate.delete(key);
    }

    @Override
    public void add(MediaServer mediaServer) {
        mediaServer.setCreateTime(DateUtil.getNow());
        mediaServer.setUpdateTime(DateUtil.getNow());
        if (mediaServer.getHookAliveInterval() == null || mediaServer.getHookAliveInterval() == 0F) {
            mediaServer.setHookAliveInterval(10F);
        }
        if (mediaServer.getType() == null) {
            logger.info("[添加媒体节点] 失败, mediaServer的类型：为空");
            return;
        }
        if (mediaServerMapper.queryOne(mediaServer.getId()) != null) {
            logger.info("[添加媒体节点] 失败, 媒体服务ID已存在，请修改媒体服务器配置, {}", mediaServer.getId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"保存失败，媒体服务ID [ " + mediaServer.getId() + " ] 已存在，请修改媒体服务器配置");
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[添加媒体节点] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return;
        }

        mediaServerMapper.add(mediaServer);
        if (mediaServer.isStatus()) {
            mediaNodeServerService.online(mediaServer);
        }else {
            // 发送事件
            MediaServerChangeEvent event = new MediaServerChangeEvent(this);
            event.setMediaServerItemList(mediaServer);
            applicationEventPublisher.publishEvent(event);
        }
    }

    @Override
    public void resetOnlineServerItem(MediaServer serverItem) {
        // 更新缓存
        String key = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
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
        String key = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
        redisTemplate.opsForZSet().incrementScore(key, mediaServerId, 1);

    }

    @Override
    public void removeCount(String mediaServerId) {
        String key = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
        redisTemplate.opsForZSet().incrementScore(key, mediaServerId, - 1);
    }

    /**
     * 获取负载最低的节点
     * @return mediaServer
     */
    @Override
    public MediaServer getMediaServerForMinimumLoad(Boolean hasAssist) {
        String key = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
        Long size = redisTemplate.opsForZSet().zCard(key);
        if (size  == null || size == 0) {
            logger.info("获取负载最低的节点时无在线节点");
            return null;
        }

        // 获取分数最低的，及并发最低的
        Set<Object> objects = redisTemplate.opsForZSet().range(key, 0, -1);
        ArrayList<Object> mediaServerObjectS = new ArrayList<>(objects);
        MediaServer mediaServer = null;
        if (hasAssist == null) {
            String mediaServerId = (String)mediaServerObjectS.get(0);
            mediaServer = getOne(mediaServerId);
        }else if (hasAssist) {
            for (Object mediaServerObject : mediaServerObjectS) {
                String mediaServerId = (String)mediaServerObject;
                MediaServer serverItem = getOne(mediaServerId);
                if (serverItem.getRecordAssistPort() > 0) {
                    mediaServer = serverItem;
                    break;
                }
            }
        }else if (!hasAssist) {
            for (Object mediaServerObject : mediaServerObjectS) {
                String mediaServerId = (String)mediaServerObject;
                MediaServer serverItem = getOne(mediaServerId);
                if (serverItem.getRecordAssistPort() == 0) {
                    mediaServer = serverItem;
                    break;
                }
            }
        }

        return mediaServer;
    }

    @Override
    public MediaServer checkMediaServer(String ip, int port, String secret, String type) {
        if (mediaServerMapper.queryOneByHostAndPort(ip, port) != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "此连接已存在");
        }

        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(type);
        if (mediaNodeServerService == null) {
            logger.info("[closeRTPServer] 失败, mediaServer的类型： {}，未找到对应的实现类", type);
            return null;
        }
        MediaServer mediaServer = mediaNodeServerService.checkMediaServer(ip, port, secret);
        if (mediaServer != null) {
            if (mediaServerMapper.queryOne(mediaServer.getId()) != null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "媒体服务ID [" + mediaServer.getId() + " ] 已存在，请修改媒体服务器配置");
            }
        }
        return mediaServer;
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
        redisTemplate.opsForZSet().remove(VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId(), id);
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId() + ":" + id;
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
        Map<String, MediaServer> mediaServerMap = new HashMap<>();

        for (MediaServer mediaServer : allInDatabase) {
            mediaServerMap.put(mediaServer.getId(), mediaServer);
        }
        for (MediaServer mediaServer : allInCatch) {
            // 清除数据中不存在但redis缓存数据
            if (!mediaServerMap.containsKey(mediaServer.getId())) {
                delete(mediaServer.getId());
            }
        }
    }

    @Override
    public MediaServerLoad getLoad(MediaServer mediaServer) {
        MediaServerLoad result = new MediaServerLoad();
        result.setId(mediaServer.getId());
        result.setPush(redisCatchStorage.getPushStreamCount(mediaServer.getId()));
        result.setProxy(redisCatchStorage.getProxyStreamCount(mediaServer.getId()));

        result.setGbReceive(inviteStreamService.getStreamInfoCount(mediaServer.getId()));
        result.setGbSend(redisCatchStorage.getGbSendCount(mediaServer.getId()));
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
            logger.info("[stopSendRtp] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaInfo.getType());
            return false;
        }
        return mediaNodeServerService.stopSendRtp(mediaInfo, app, stream, ssrc);
    }

    @Override
    public boolean initStopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaInfo.getType());
        if (mediaNodeServerService == null) {
            logger.info("[stopSendRtp] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaInfo.getType());
            return false;
        }
        return mediaNodeServerService.initStopSendRtp(mediaInfo, app, stream, ssrc);
    }

    @Override
    public boolean deleteRecordDirectory(MediaServer mediaServer, String app, String stream, String date, String fileName) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[stopSendRtp] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.deleteRecordDirectory(mediaServer, app, stream, date, fileName);
    }

    @Override
    public List<StreamInfo> getMediaList(MediaServer mediaServer, String app, String stream, String callId) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[getMediaList] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return new ArrayList<>();
        }
        return mediaNodeServerService.getMediaList(mediaServer, app, stream, callId);
    }

    @Override
    public Boolean connectRtpServer(MediaServer mediaServer, String address, int port, String stream) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[connectRtpServer] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.connectRtpServer(mediaServer, address, port, stream);
    }

    @Override
    public void getSnap(MediaServer mediaServer, String streamUrl, int timeoutSec, int expireSec, String path, String fileName) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[getSnap] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return;
        }
        mediaNodeServerService.getSnap(mediaServer, streamUrl, timeoutSec, expireSec, path, fileName);
    }

    @Override
    public MediaInfo getMediaInfo(MediaServer mediaServer, String app, String stream) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[getMediaInfo] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return null;
        }
        return mediaNodeServerService.getMediaInfo(mediaServer, app, stream);
    }

    @Override
    public Boolean pauseRtpCheck(MediaServer mediaServer, String streamKey) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[pauseRtpCheck] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.pauseRtpCheck(mediaServer, streamKey);
    }

    @Override
    public boolean resumeRtpCheck(MediaServer mediaServer, String streamKey) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[pauseRtpCheck] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.resumeRtpCheck(mediaServer, streamKey);
    }

    @Override
    public String getFfmpegCmd(MediaServer mediaServer, String cmdKey) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[getFfmpegCmd] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return null;
        }
        return mediaNodeServerService.getFfmpegCmd(mediaServer, cmdKey);
    }

    @Override
    public void closeStreams(MediaServer mediaServer, String app, String stream) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[closeStreams] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return;
        }
        mediaNodeServerService.closeStreams(mediaServer, app, stream);
    }

    @Override
    public WVPResult<String> addFFmpegSource(MediaServer mediaServer, String srcUrl, String dstUrl, int timeoutMs, boolean enableAudio, boolean enableMp4, String ffmpegCmdKey) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[addFFmpegSource] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return WVPResult.fail(ErrorCode.ERROR400);
        }
        return mediaNodeServerService.addFFmpegSource(mediaServer, srcUrl, dstUrl, timeoutMs, enableAudio, enableMp4, ffmpegCmdKey);
    }

    @Override
    public WVPResult<String> addStreamProxy(MediaServer mediaServer, String app, String stream, String url, boolean enableAudio, boolean enableMp4, String rtpType) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[addStreamProxy] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return WVPResult.fail(ErrorCode.ERROR400);
        }
        return mediaNodeServerService.addStreamProxy(mediaServer, app, stream, url, enableAudio, enableMp4, rtpType);
    }

    @Override
    public Boolean delFFmpegSource(MediaServer mediaServer, String streamKey) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[delFFmpegSource] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.delFFmpegSource(mediaServer, streamKey);
    }

    @Override
    public Boolean delStreamProxy(MediaServer mediaServerItem, String streamKey) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            logger.info("[delStreamProxy] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServerItem.getType());
            return false;
        }
        return mediaNodeServerService.delStreamProxy(mediaServerItem, streamKey);
    }

    @Override
    public Map<String, String> getFFmpegCMDs(MediaServer mediaServer) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[getFFmpegCMDs] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return new HashMap<>();
        }
        return mediaNodeServerService.getFFmpegCMDs(mediaServer);
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStream(MediaServer mediaServerItem, String app, String stream, MediaInfo mediaInfo, String callId) {
        return getStreamInfoByAppAndStream(mediaServerItem, app, stream, mediaInfo, null, callId, true);
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream, String mediaServerId, String addr, boolean authority) {
        StreamInfo streamInfo = null;
        if (mediaServerId == null) {
            mediaServerId = mediaConfig.getId();
        }
        MediaServer mediaInfo = getOne(mediaServerId);
        if (mediaInfo == null) {
            return null;
        }
        String calld = null;
        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
        if (streamAuthorityInfo != null) {
            calld = streamAuthorityInfo.getCallId();
        }
        List<StreamInfo> streamInfoList = getMediaList(mediaInfo, app, stream, calld);
        if (streamInfoList == null || streamInfoList.isEmpty()) {
            return null;
        }else {
            return streamInfoList.get(0);
        }
    }



    @Override
    public StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream, String mediaServerId, boolean authority) {
        return getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, null, authority);
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStream(MediaServer mediaServer, String app, String stream, MediaInfo mediaInfo, String addr, String callId, boolean isPlay) {
        StreamInfo streamInfoResult = new StreamInfo();
        streamInfoResult.setStream(stream);
        streamInfoResult.setApp(app);
        if (addr == null) {
            addr = mediaServer.getStreamIp();
        }

        streamInfoResult.setIp(addr);
        streamInfoResult.setMediaServerId(mediaServer.getId());
        String callIdParam = ObjectUtils.isEmpty(callId)?"":"?callId=" + callId;
        streamInfoResult.setRtmp(addr, mediaServer.getRtmpPort(),mediaServer.getRtmpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setRtsp(addr, mediaServer.getRtspPort(),mediaServer.getRtspSSLPort(), app,  stream, callIdParam);


        if ("abl".equals(mediaServer.getType())) {
            String flvFile = String.format("%s/%s.flv%s", app, stream, callIdParam);
            streamInfoResult.setFlv(addr, mediaServer.getFlvPort(),mediaServer.getFlvSSLPort(), flvFile);
            streamInfoResult.setWsFlv(addr, mediaServer.getWsFlvPort(),mediaServer.getWsFlvSSLPort(), flvFile);
        }else {
            String flvFile = String.format("%s/%s.live.flv%s", app, stream, callIdParam);
            streamInfoResult.setFlv(addr, mediaServer.getFlvPort(),mediaServer.getFlvSSLPort(), flvFile);
            streamInfoResult.setWsFlv(addr, mediaServer.getWsFlvPort(),mediaServer.getWsFlvSSLPort(), flvFile);
        }

        streamInfoResult.setFmp4(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setHls(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setTs(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setRtc(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam, isPlay);

        streamInfoResult.setMediaInfo(mediaInfo);

        if (!"broadcast".equalsIgnoreCase(app) && !ObjectUtils.isEmpty(mediaServer.getTranscodeSuffix()) && !"null".equalsIgnoreCase(mediaServer.getTranscodeSuffix())) {
            String newStream = stream + "_" + mediaServer.getTranscodeSuffix();
            mediaServer.setTranscodeSuffix(null);
            StreamInfo transcodeStreamInfo = getStreamInfoByAppAndStream(mediaServer, app, newStream, null, addr, callId, isPlay);
            streamInfoResult.setTranscodeStream(transcodeStreamInfo);
        }
        return streamInfoResult;
    }

    @Override
    public Boolean isStreamReady(MediaServer mediaServer, String rtp, String streamId) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[isStreamReady] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            return false;
        }
        MediaInfo mediaInfo = mediaNodeServerService.getMediaInfo(mediaServer, rtp, streamId);
        return mediaInfo != null;
    }

    @Override
    public void startSendRtpPassive(MediaServer mediaServer, SendRtpItem sendRtpItem, Integer timeout) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[startSendRtpPassive] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到mediaServer对应的实现类");
        }
        mediaNodeServerService.startSendRtpPassive(mediaServer, sendRtpItem, timeout);
    }

    @Override
    public void startSendRtp(MediaServer mediaServer, SendRtpItem sendRtpItem) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[startSendRtpStream] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到mediaServer对应的实现类");
        }
        logger.info("[开始推流] rtp/{}, 目标={}:{}，SSRC={}, RTCP={}", sendRtpItem.getStream(),
                sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isRtcp());
        mediaNodeServerService.startSendRtpStream(mediaServer, sendRtpItem);
    }

    @Override
    public SendRtpItem createSendRtpItem(MediaServer mediaServer, String ip, int port, String ssrc, String requesterId, String deviceId, String channelId, boolean isTcp, boolean rtcp) {
        int localPort = sendRtpPortManager.getNextPort(mediaServer);
        if (localPort == 0) {
            return null;
        }
        SendRtpItem sendRtpItem = new SendRtpItem();
        sendRtpItem.setIp(ip);
        sendRtpItem.setPort(port);
        sendRtpItem.setSsrc(ssrc);
        sendRtpItem.setPlatformId(deviceId);
        sendRtpItem.setDeviceId(deviceId);
        sendRtpItem.setChannelId(channelId);
        sendRtpItem.setTcp(isTcp);
        sendRtpItem.setRtcp(rtcp);
        sendRtpItem.setApp("rtp");
        sendRtpItem.setLocalPort(localPort);
        sendRtpItem.setServerId(userSetting.getServerId());
        sendRtpItem.setMediaServerId(mediaServer.getId());
        return sendRtpItem;
    }

    @Override
    public SendRtpItem createSendRtpItem(MediaServer serverItem, String ip, int port, String ssrc, String platformId,
                                         String app, String stream, String channelId, boolean tcp, boolean rtcp){

        int localPort = sendRtpPortManager.getNextPort(serverItem);
        if (localPort == 0) {
            return null;
        }
        SendRtpItem sendRtpItem = new SendRtpItem();
        sendRtpItem.setIp(ip);
        sendRtpItem.setPort(port);
        sendRtpItem.setSsrc(ssrc);
        sendRtpItem.setApp(app);
        sendRtpItem.setStream(stream);
        sendRtpItem.setPlatformId(platformId);
        sendRtpItem.setChannelId(channelId);
        sendRtpItem.setTcp(tcp);
        sendRtpItem.setLocalPort(localPort);
        sendRtpItem.setServerId(userSetting.getServerId());
        sendRtpItem.setMediaServerId(serverItem.getId());
        sendRtpItem.setRtcp(rtcp);
        return sendRtpItem;
    }

    @Override
    public MediaServer getMediaServerByAppAndStream(String app, String stream) {
        List<MediaServer> mediaServerList = getAll();
        for (MediaServer mediaServer : mediaServerList) {
            MediaInfo mediaInfo = getMediaInfo(mediaServer, app, stream);
            if (mediaInfo != null) {
                return mediaServer;
            }
        }
        return null;
    }

    @Override
    public Long updateDownloadProcess(MediaServer mediaServer, String app, String stream) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            logger.info("[updateDownloadProcess] 失败, mediaServer的类型： {}，未找到对应的实现类", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到mediaServer对应的实现类");
        }
        return mediaNodeServerService.updateDownloadProcess(mediaServer, app, stream);
    }
}
