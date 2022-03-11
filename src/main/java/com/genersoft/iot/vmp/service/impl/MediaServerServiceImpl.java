package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.session.SsrcConfig;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZLMServerConfig;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.storager.dao.MediaServerMapper;
import com.genersoft.iot.vmp.utils.redis.JedisUtil;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 媒体服务器节点管理
 */
@Service
public class MediaServerServiceImpl implements IMediaServerService {

    private final static Logger logger = LoggerFactory.getLogger(MediaServerServiceImpl.class);

    @Autowired
    private SipConfig sipConfig;

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${server.port}")
    private Integer serverPort;

    @Autowired
    private UserSetup userSetup;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private MediaServerMapper mediaServerMapper;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private ZLMRTPServerFactory zlmrtpServerFactory;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private IStreamProxyService streamProxyService;

    @Autowired
    private EventPublisher publisher;

    @Autowired
    JedisUtil jedisUtil;

    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 初始化
     */
    @Override
    public void updateVmServer(List<MediaServerItem>  mediaServerItemList) {
        logger.info("[缓存初始化] Media Server ");
        for (MediaServerItem mediaServerItem : mediaServerItemList) {
            if (StringUtils.isEmpty(mediaServerItem.getId())) {
                continue;
            }
            // 更新
            if (mediaServerItem.getSsrcConfig() == null) {
                SsrcConfig ssrcConfig = new SsrcConfig(mediaServerItem.getId(), null, sipConfig.getDomain());
                mediaServerItem.setSsrcConfig(ssrcConfig);
                redisUtil.set(VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetup.getServerId() + "_" + mediaServerItem.getId(), mediaServerItem);
            }
            // 查询redis是否存在此mediaServer
            String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetup.getServerId() + "_" + mediaServerItem.getId();
            if (!redisUtil.hasKey(key)) {
                redisUtil.set(key, mediaServerItem);
            }

        }
    }

    @Override
    public SSRCInfo openRTPServer(MediaServerItem mediaServerItem, String streamId) {
        return openRTPServer(mediaServerItem, streamId, false);
    }

    @Override
    public SSRCInfo openRTPServer(MediaServerItem mediaServerItem, String streamId, boolean isPlayback) {
        if (mediaServerItem == null || mediaServerItem.getId() == null) {
            return null;
        }
        // 获取mediaServer可用的ssrc
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetup.getServerId() + "_" + mediaServerItem.getId();

        SsrcConfig ssrcConfig = mediaServerItem.getSsrcConfig();
        if (ssrcConfig == null) {
            logger.info("media server [ {} ] ssrcConfig is null", mediaServerItem.getId());
            return null;
        }else {
            String ssrc = null;
            if (isPlayback) {
                ssrc = ssrcConfig.getPlayBackSsrc();
            }else {
                ssrc = ssrcConfig.getPlaySsrc();
            }

            if (streamId == null) {
                streamId = String.format("%08x", Integer.parseInt(ssrc)).toUpperCase();
            }
            int rtpServerPort = mediaServerItem.getRtpProxyPort();
            if (mediaServerItem.isRtpEnable()) {
                rtpServerPort = zlmrtpServerFactory.createRTPServer(mediaServerItem, streamId);
            }
            redisUtil.set(key, mediaServerItem);
            return new SSRCInfo(rtpServerPort, ssrc, streamId);
        }
    }

    @Override
    public void closeRTPServer(String deviceId, String channelId, String stream) {
        String mediaServerId = streamSession.getMediaServerId(deviceId, channelId, stream);
        String ssrc = streamSession.getSSRC(deviceId, channelId, stream);
        MediaServerItem mediaServerItem = this.getOne(mediaServerId);
        if (mediaServerItem != null) {
            String streamId = String.format("%s_%s", deviceId, channelId);
            zlmrtpServerFactory.closeRTPServer(mediaServerItem, streamId);
            releaseSsrc(mediaServerItem.getId(), ssrc);
        }
        streamSession.remove(deviceId, channelId, stream);
    }

    @Override
    public void releaseSsrc(String mediaServerItemId, String ssrc) {
        MediaServerItem mediaServerItem = getOne(mediaServerItemId);
        if (mediaServerItem == null || ssrc == null) {
            return;
        }
        SsrcConfig ssrcConfig = mediaServerItem.getSsrcConfig();
        ssrcConfig.releaseSsrc(ssrc);
        mediaServerItem.setSsrcConfig(ssrcConfig);
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetup.getServerId() + "_" + mediaServerItem.getId();
        redisUtil.set(key, mediaServerItem);
    }

    /**
     * zlm 重启后重置他的推流信息， TODO 给正在使用的设备发送停止命令
     */
    @Override
    public void clearRTPServer(MediaServerItem mediaServerItem) {
        mediaServerItem.setSsrcConfig(new SsrcConfig(mediaServerItem.getId(), null, sipConfig.getDomain()));
        redisUtil.zAdd(VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetup.getServerId(), mediaServerItem.getId(), 0);
    }


    @Override
    public void update(MediaServerItem mediaSerItem) {
        mediaServerMapper.update(mediaSerItem);
        MediaServerItem mediaServerItemInRedis = getOne(mediaSerItem.getId());
        MediaServerItem mediaServerItemInDataBase = mediaServerMapper.queryOne(mediaSerItem.getId());
        if (mediaServerItemInRedis != null && mediaServerItemInRedis.getSsrcConfig() != null) {
            mediaServerItemInDataBase.setSsrcConfig(mediaServerItemInRedis.getSsrcConfig());
        }else {
            mediaServerItemInDataBase.setSsrcConfig(
                    new SsrcConfig(
                            mediaServerItemInDataBase.getId(),
                            null,
                            sipConfig.getDomain()
                    )
            );
        }
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetup.getServerId() + "_" + mediaServerItemInDataBase.getId();
        redisUtil.set(key, mediaServerItemInDataBase);
    }

    @Override
    public List<MediaServerItem> getAll() {
        List<MediaServerItem> result = new ArrayList<>();
        List<Object> mediaServerKeys = redisUtil.scan(String.format("%S*", VideoManagerConstants.MEDIA_SERVER_PREFIX+ userSetup.getServerId() + "_" ));
        String onlineKey = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetup.getServerId();
        for (Object mediaServerKey : mediaServerKeys) {
            String key = (String) mediaServerKey;
            MediaServerItem mediaServerItem = (MediaServerItem) redisUtil.get(key);
            // 检查状态
            Double aDouble = redisUtil.zScore(onlineKey, mediaServerItem.getId());
            if (aDouble != null) {
                mediaServerItem.setStatus(true);
            }
            result.add(mediaServerItem);
        }
        result.sort((serverItem1, serverItem2)->{
            int sortResult = 0;
            try {
                sortResult = format.parse(serverItem1.getCreateTime()).compareTo(format.parse(serverItem2.getCreateTime()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return  sortResult;
        });
        return result;
    }


    @Override
    public List<MediaServerItem> getAllFromDatabase() {
        return mediaServerMapper.queryAll();
    }

    @Override
    public List<MediaServerItem> getAllOnline() {
        String key = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetup.getServerId();
        Set<String> mediaServerIdSet = redisUtil.zRevRange(key, 0, -1);

        List<MediaServerItem> result = new ArrayList<>();
        if (mediaServerIdSet != null && mediaServerIdSet.size() > 0) {
            for (String mediaServerId : mediaServerIdSet) {
                String serverKey = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetup.getServerId() + "_" + mediaServerId;
                result.add((MediaServerItem) redisUtil.get(serverKey));
            }
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * 获取单个zlm服务器
     * @param mediaServerId 服务id
     * @return MediaServerItem
     */
    @Override
    public MediaServerItem getOne(String mediaServerId) {
        if (mediaServerId == null) {
            return null;
        }
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetup.getServerId() + "_" + mediaServerId;
        return (MediaServerItem)redisUtil.get(key);
    }

    @Override
    public MediaServerItem getDefaultMediaServer() {

        return mediaServerMapper.queryDefault();
    }

    @Override
    public void clearMediaServerForOnline() {
        String key = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetup.getServerId();
        redisUtil.del(key);
    }

    @Override
    public WVPResult<String> add(MediaServerItem mediaServerItem) {
        WVPResult<String> result = new WVPResult<>();
        mediaServerItem.setCreateTime(this.format.format(System.currentTimeMillis()));
        mediaServerItem.setUpdateTime(this.format.format(System.currentTimeMillis()));
        mediaServerItem.setHookAliveInterval(120);
        JSONObject responseJSON = zlmresTfulUtils.getMediaServerConfig(mediaServerItem);
        if (responseJSON != null) {
            JSONArray data = responseJSON.getJSONArray("data");
            if (data != null && data.size() > 0) {
                ZLMServerConfig zlmServerConfig= JSON.parseObject(JSON.toJSONString(data.get(0)), ZLMServerConfig.class);
                if (mediaServerMapper.queryOne(zlmServerConfig.getGeneralMediaServerId()) != null) {
                    result.setCode(-1);
                    result.setMsg("保存失败，媒体服务ID [ " + zlmServerConfig.getGeneralMediaServerId() + " ] 已存在，请修改媒体服务器配置");
                    return result;
                }
                mediaServerItem.setId(zlmServerConfig.getGeneralMediaServerId());
                zlmServerConfig.setIp(mediaServerItem.getIp());
                mediaServerMapper.add(mediaServerItem);
                zlmServerOnline(zlmServerConfig);
                result.setCode(0);
                result.setMsg("success");
            }else {
                result.setCode(-1);
                result.setMsg("连接失败");
            }

        }else {
            result.setCode(-1);
            result.setMsg("连接失败");
        }
       return result;
    }

    @Override
    public int addToDatabase(MediaServerItem mediaSerItem) {
        return mediaServerMapper.add(mediaSerItem);
    }

    @Override
    public int updateToDatabase(MediaServerItem mediaSerItem) {
        int result = 0;
        if (mediaSerItem.isDefaultServer()) {
            TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            int delResult = mediaServerMapper.delDefault();
            if (delResult == 0) {
                logger.error("移除数据库默认zlm节点失败");
                //事务回滚
                dataSourceTransactionManager.rollback(transactionStatus);
                return 0;
            }
            result = mediaServerMapper.add(mediaSerItem);
            dataSourceTransactionManager.commit(transactionStatus);     //手动提交
        }else {
            result = mediaServerMapper.update(mediaSerItem);
        }
        return result;
    }

    /**
     * 处理zlm上线
     * @param zlmServerConfig zlm上线携带的参数
     */
    @Override
    public void zlmServerOnline(ZLMServerConfig zlmServerConfig) {
        logger.info("[ ZLM：{} ]-[ {}:{} ]正在连接",
                zlmServerConfig.getGeneralMediaServerId(), zlmServerConfig.getIp(), zlmServerConfig.getHttpPort());

        MediaServerItem serverItem = mediaServerMapper.queryOne(zlmServerConfig.getGeneralMediaServerId());
        if (serverItem == null) {
            logger.warn("[未注册的zlm] 拒接接入：{}来自{}：{}", zlmServerConfig.getGeneralMediaServerId(), zlmServerConfig.getIp(),zlmServerConfig.getHttpPort() );
            logger.warn("请检查ZLM的<general.mediaServerId>配置是否与WVP的<media.id>一致");
            return;
        }
        serverItem.setHookAliveInterval(zlmServerConfig.getHookAliveInterval());
        if (serverItem.getHttpPort() == 0) {
            serverItem.setHttpPort(zlmServerConfig.getHttpPort());
        }
        if (serverItem.getHttpSSlPort() == 0) {
            serverItem.setHttpSSlPort(zlmServerConfig.getHttpSSLport());
        }
        if (serverItem.getRtmpPort() == 0) {
            serverItem.setRtmpPort(zlmServerConfig.getRtmpPort());
        }
        if (serverItem.getRtmpSSlPort() == 0) {
            serverItem.setRtmpSSlPort(zlmServerConfig.getRtmpSslPort());
        }
        if (serverItem.getRtspPort() == 0) {
            serverItem.setRtspPort(zlmServerConfig.getRtspPort());
        }
        if (serverItem.getRtspSSLPort() == 0) {
            serverItem.setRtspSSLPort(zlmServerConfig.getRtspSSlport());
        }
        if (serverItem.getRtpProxyPort() == 0) {
            serverItem.setRtpProxyPort(zlmServerConfig.getRtpProxyPort());
        }
        serverItem.setStatus(true);

        if (StringUtils.isEmpty(serverItem.getId())) {
            logger.warn("[未注册的zlm] serverItem缺少ID， 无法接入：{}：{}", zlmServerConfig.getIp(),zlmServerConfig.getHttpPort() );
            return;
        }
        mediaServerMapper.update(serverItem);
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetup.getServerId() + "_" + zlmServerConfig.getGeneralMediaServerId();
        if (redisUtil.get(key) == null) {
            SsrcConfig ssrcConfig = new SsrcConfig(zlmServerConfig.getGeneralMediaServerId(), null, sipConfig.getDomain());
            serverItem.setSsrcConfig(ssrcConfig);
        }else {
            MediaServerItem mediaServerItemInRedis = (MediaServerItem)redisUtil.get(key);
            serverItem.setSsrcConfig(mediaServerItemInRedis.getSsrcConfig());
        }
        redisUtil.set(key, serverItem);
        resetOnlineServerItem(serverItem);
        updateMediaServerKeepalive(serverItem.getId(), null);
        setZLMConfig(serverItem, "0".equals(zlmServerConfig.getHookEnable()));

        publisher.zlmOnlineEventPublish(serverItem.getId());
        logger.info("[ ZLM：{} ]-[ {}:{} ]连接成功",
                zlmServerConfig.getGeneralMediaServerId(), zlmServerConfig.getIp(), zlmServerConfig.getHttpPort());
    }


    @Override
    public void zlmServerOffline(String mediaServerId) {
        delete(mediaServerId);
    }

    @Override
    public void resetOnlineServerItem(MediaServerItem serverItem) {
        // 更新缓存
        String key = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetup.getServerId();
        // 使用zset的分数作为当前并发量， 默认值设置为0
        if (redisUtil.zScore(key, serverItem.getId()) == null) {  // 不存在则设置默认值 已存在则重置
            redisUtil.zAdd(key, serverItem.getId(), 0L);
            // 查询服务流数量
            zlmresTfulUtils.getMediaList(serverItem, null, null, "rtmp",(mediaList ->{
                Integer code = mediaList.getInteger("code");
                if (code == 0) {
                    JSONArray data = mediaList.getJSONArray("data");
                    if (data != null) {
                        redisUtil.zAdd(key, serverItem.getId(), data.size());
                    }
                }
            }));
        }else {
            clearRTPServer(serverItem);
        }

    }


    @Override
    public void addCount(String mediaServerId) {
        if (mediaServerId == null) {
            return;
        }
        String key = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetup.getServerId();
        redisUtil.zIncrScore(key, mediaServerId, 1);

    }

    @Override
    public void removeCount(String mediaServerId) {
        String key = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetup.getServerId();
        redisUtil.zIncrScore(key, mediaServerId, - 1);
    }

    /**
     * 获取负载最低的节点
     * @return MediaServerItem
     */
    @Override
    public MediaServerItem getMediaServerForMinimumLoad() {
        String key = VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetup.getServerId();

        if (redisUtil.zSize(key)  == null || redisUtil.zSize(key) == 0) {
            logger.info("获取负载最低的节点时无在线节点");
            return null;
        }

        // 获取分数最低的，及并发最低的
        Set<Object> objects = redisUtil.ZRange(key, 0, -1);
        ArrayList<Object> mediaServerObjectS = new ArrayList<>(objects);

        String mediaServerId = (String)mediaServerObjectS.get(0);
        return getOne(mediaServerId);
    }

    /**
     * 对zlm服务器进行基础配置
     * @param mediaServerItem 服务ID
     * @param restart 是否重启zlm
     */
    @Override
    public void setZLMConfig(MediaServerItem mediaServerItem, boolean restart) {
        logger.info("[ ZLM：{} ]-[ {}:{} ]正在设置zlm",
                mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
        String protocol = sslEnabled ? "https" : "http";
        String hookPrex = String.format("%s://%s:%s/index/hook", protocol, mediaServerItem.getHookIp(), serverPort);
        String recordHookPrex = null;
        if (mediaServerItem.getRecordAssistPort() != 0) {
            recordHookPrex = String.format("http://127.0.0.1:%s/api/record", mediaServerItem.getRecordAssistPort());
        }
        Map<String, Object> param = new HashMap<>();
        param.put("api.secret",mediaServerItem.getSecret()); // -profile:v Baseline
        param.put("ffmpeg.cmd","%s -fflags nobuffer -i %s -c:a aac -strict -2 -ar 44100 -ab 48k -c:v libx264  -f flv %s");
        param.put("hook.enable","1");
        param.put("hook.on_flow_report","");
        param.put("hook.on_play",String.format("%s/on_play", hookPrex));
        param.put("hook.on_http_access","");
        param.put("hook.on_publish", String.format("%s/on_publish", hookPrex));
        param.put("hook.on_record_mp4",recordHookPrex != null? String.format("%s/on_record_mp4", recordHookPrex): "");
        param.put("hook.on_record_ts","");
        param.put("hook.on_rtsp_auth","");
        param.put("hook.on_rtsp_realm","");
        param.put("hook.on_server_started",String.format("%s/on_server_started", hookPrex));
        param.put("hook.on_shell_login",String.format("%s/on_shell_login", hookPrex));
        param.put("hook.on_stream_changed",String.format("%s/on_stream_changed", hookPrex));
        param.put("hook.on_stream_none_reader",String.format("%s/on_stream_none_reader", hookPrex));
        param.put("hook.on_stream_not_found",String.format("%s/on_stream_not_found", hookPrex));
        param.put("hook.on_server_keepalive",String.format("%s/on_server_keepalive", hookPrex));
        param.put("hook.timeoutSec","20");
        param.put("general.streamNoneReaderDelayMS",mediaServerItem.getStreamNoneReaderDelayMS()==-1?"3600000":mediaServerItem.getStreamNoneReaderDelayMS() );
        // 推流断开后可以在超时时间内重新连接上继续推流，这样播放器会接着播放。
        // 置0关闭此特性(推流断开会导致立即断开播放器)
        // 此参数不应大于播放器超时时间
        // 优化此消息以更快的收到流注销事件
        param.put("general.continue_push_ms", "3000" );
        // 最多等待未初始化的Track时间，单位毫秒，超时之后会忽略未初始化的Track, 设置此选项优化那些音频错误的不规范流，
        // 等zlm支持给每个rtpServer设置关闭音频的时候可以不设置此选项
        param.put("general.wait_track_ready_ms", "3000" );

        JSONObject responseJSON = zlmresTfulUtils.setServerConfig(mediaServerItem, param);

        if (responseJSON != null && responseJSON.getInteger("code") == 0) {
            if (restart) {
                logger.info("[ ZLM：{} ]-[ {}:{} ]设置zlm成功, 开始重启以保证配置生效",
                        mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                zlmresTfulUtils.restartServer(mediaServerItem);
            }else {
                logger.info("[ ZLM：{} ]-[ {}:{} ]设置zlm成功",
                        mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
            }


        }else {
            logger.info("[ ZLM：{} ]-[ {}:{} ]设置zlm失败",
                    mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
        }


    }


    @Override
    public WVPResult<MediaServerItem> checkMediaServer(String ip, int port, String secret) {
        WVPResult<MediaServerItem> result = new WVPResult<>();
        if (mediaServerMapper.queryOneByHostAndPort(ip, port) != null) {
            result.setCode(-1);
            result.setMsg("此连接已存在");
            return result;
        }
        MediaServerItem mediaServerItem = new MediaServerItem();
        mediaServerItem.setIp(ip);
        mediaServerItem.setHttpPort(port);
        mediaServerItem.setSecret(secret);
        JSONObject responseJSON = zlmresTfulUtils.getMediaServerConfig(mediaServerItem);
        if (responseJSON == null) {
            result.setCode(-1);
            result.setMsg("连接失败");
            return result;
        }
        JSONArray data = responseJSON.getJSONArray("data");
        ZLMServerConfig zlmServerConfig = JSON.parseObject(JSON.toJSONString(data.get(0)), ZLMServerConfig.class);
        if (zlmServerConfig == null) {
            result.setCode(-1);
            result.setMsg("读取配置失败");
            return result;
        }
        if (mediaServerMapper.queryOne(zlmServerConfig.getGeneralMediaServerId()) != null) {
            result.setCode(-1);
            result.setMsg("媒体服务ID [" + zlmServerConfig.getGeneralMediaServerId() + " ] 已存在，请修改媒体服务器配置");
            return result;
        }
        mediaServerItem.setHttpSSlPort(zlmServerConfig.getHttpPort());
        mediaServerItem.setRtmpPort(zlmServerConfig.getRtmpPort());
        mediaServerItem.setRtmpSSlPort(zlmServerConfig.getRtmpSslPort());
        mediaServerItem.setRtspPort(zlmServerConfig.getRtspPort());
        mediaServerItem.setRtspSSLPort(zlmServerConfig.getRtspSSlport());
        mediaServerItem.setRtpProxyPort(zlmServerConfig.getRtpProxyPort());
        mediaServerItem.setStreamIp(ip);
        mediaServerItem.setHookIp(sipConfig.getIp());
        mediaServerItem.setSdpIp(ip);
        mediaServerItem.setStreamNoneReaderDelayMS(zlmServerConfig.getGeneralStreamNoneReaderDelayMS());
        result.setCode(0);
        result.setMsg("成功");
        result.setData(mediaServerItem);
        return result;
    }

    @Override
    public boolean checkMediaRecordServer(String ip, int port) {
        boolean result = false;
        OkHttpClient client = new OkHttpClient();
        String url = String.format("http://%s:%s/index/api/record",  ip, port);

        FormBody.Builder builder = new FormBody.Builder();

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
        redisUtil.zRemove(VideoManagerConstants.MEDIA_SERVERS_ONLINE_PREFIX + userSetup.getServerId(), id);
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetup.getServerId() + "_" + id;
        redisUtil.del(key);
    }
    @Override
    public void deleteDb(String id){
        //同步删除数据库中的数据
        mediaServerMapper.delOne(id);
    }

    @Override
    public void updateMediaServerKeepalive(String mediaServerId, JSONObject data) {
        MediaServerItem mediaServerItem = getOne(mediaServerId);
        if (mediaServerItem == null) {
            // zlm连接重试

            logger.warn("[更新ZLM 保活信息]失败，未找到流媒体信息");
            return;
        }
        String key = VideoManagerConstants.MEDIA_SERVER_KEEPALIVE_PREFIX + userSetup.getServerId() + "_" + mediaServerId;
        int hookAliveInterval = mediaServerItem.getHookAliveInterval() + 2;
        redisUtil.set(key, data, hookAliveInterval);
    }

    @Override
    public void syncCatchFromDatabase() {
        List<MediaServerItem> allInCatch = getAll();
        List<MediaServerItem> allInDatabase = mediaServerMapper.queryAll();
        Map<String, MediaServerItem> mediaServerItemMap = new HashMap<>();

        for (MediaServerItem mediaServerItem : allInDatabase) {
            mediaServerItemMap.put(mediaServerItem.getId(), mediaServerItem);
        }
        for (MediaServerItem mediaServerItem : allInCatch) {
            if (mediaServerItemMap.get(mediaServerItem) == null) {
                delete(mediaServerItem.getId());
            }
        }
    }

}
