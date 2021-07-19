package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.ProxyServletConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZLMServerConfig;
import com.genersoft.iot.vmp.media.zlm.dto.IMediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMRunInfo;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.MediaServerMapper;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 媒体服务器节点管理
 */
@Service
public class MediaServerServiceImpl implements IMediaServerService {

    private final static Logger logger = LoggerFactory.getLogger(MediaServerServiceImpl.class);

    private Map<String, IMediaServerItem> zlmServers = new HashMap<>(); // 所有数据库的zlm的缓存
    private Map<String, Integer> zlmServerStatus = new LinkedHashMap<>(); // 所有上线的zlm的缓存以及负载

    @Value("${sip.ip}")
    private String sipIp;

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${server.port}")
    private String serverPort;

    @Autowired
    private MediaConfig mediaConfig;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private MediaServerMapper mediaServerMapper;


    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private ZLMRTPServerFactory zlmrtpServerFactory;

    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 初始化
     */
    @Override
    public void init() {
        zlmServers.clear();
        zlmServerStatus.clear();
        List<MediaServerItem> mediaServerItemList = mediaServerMapper.queryAll();
        for (IMediaServerItem mediaServerItem : mediaServerItemList) {
            zlmServers.put(mediaServerItem.getId(), mediaServerItem);
        }
    }

    @Override
    public void closeRTPServer(Device device, String channelId) {
        StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(device.getDeviceId(), channelId);
        IMediaServerItem mediaServerItem = null;
        if (streamInfo != null) {
            mediaServerItem = this.getOne (streamInfo.getMediaServerId());
        }
        String streamId = String.format("gb_play_%s_%s", device.getDeviceId(), channelId);
        zlmrtpServerFactory.closeRTPServer(mediaServerItem, streamId);
        streamSession.remove(device.getDeviceId(), channelId);
    }

    @Override
    public void update(MediaConfig mediaConfig) {

    }

    @Override
    public List<IMediaServerItem> getAll() {
        if (zlmServers.size() == 0) {
            init();
        }
        List<IMediaServerItem> result = new ArrayList<>();
        for (String id : zlmServers.keySet()) {
            IMediaServerItem mediaServerItem = zlmServers.get(id);
            mediaServerItem.setCount(zlmServerStatus.get(id) == null ? 0 : zlmServerStatus.get(id));
            result.add(mediaServerItem);
        }
        return result;


//        return mediaServerMapper.queryAll();
    }

    /**
     * 获取单个zlm服务器
     * @param mediaServerId 服务id
     * @return MediaServerItem
     */
    @Override
    public IMediaServerItem getOne(String mediaServerId) {
        if (mediaServerId ==null) return null;
        IMediaServerItem mediaServerItem = zlmServers.get(mediaServerId);
        if (mediaServerItem != null) {
            mediaServerItem.setCount(zlmServerStatus.get(mediaServerId) == null ? 0 : zlmServerStatus.get(mediaServerId));
            return mediaServerItem;
        }else {
            IMediaServerItem item = mediaServerMapper.queryOne(mediaServerId);
            if (item != null) {
                zlmServers.put(item.getId(), item);
            }
            return item;
        }
    }

    @Override
    public IMediaServerItem getOneByHostAndPort(String host, int port) {
        return mediaServerMapper.queryOneByHostAndPort(host, port);
    }

    /**
     * 处理zlm上线
     * @param zlmServerConfig zlm上线携带的参数
     */
    @Override
    public void handLeZLMServerConfig(ZLMServerConfig zlmServerConfig) {
        logger.info("[ {} ]-[ {}:{} ]已连接",
                zlmServerConfig.getGeneralMediaServerId(), zlmServerConfig.getIp(), zlmServerConfig.getHttpPort());

        IMediaServerItem serverItem = getOne(zlmServerConfig.getGeneralMediaServerId());
        String now = this.format.format(new Date(System.currentTimeMillis()));
        if (serverItem != null) {
            serverItem.setSecret(zlmServerConfig.getApiSecret());
            serverItem.setIp(zlmServerConfig.getIp());
            // 如果是配置文件中的zlm。 也就是默认zlm。 一切以配置文件内容为准
            // docker部署不会使用zlm配置的端口号;
            // 直接编译部署的使用配置文件的端口号，如果zlm修改配改了配置，wvp自动修改

            if (serverItem.getId().equals(mediaConfig.getId())
                    || (serverItem.getIp().equals(mediaConfig.getIp()) && serverItem.getHttpPort() == mediaConfig.getHttpPort())) {
                // 配置文件的zlm
                mediaConfig.setId(zlmServerConfig.getGeneralMediaServerId());
                mediaConfig.setUpdateTime(now);
                if (mediaConfig.getHttpPort() == 0) mediaConfig.setHttpPort(zlmServerConfig.getHttpPort());
                if (mediaConfig.getHttpSSlPort() == 0) mediaConfig.setHttpSSlPort(zlmServerConfig.getHttpSSLport());
                if (mediaConfig.getRtmpPort() == 0) mediaConfig.setRtmpPort(zlmServerConfig.getRtmpPort());
                if (mediaConfig.getRtmpSSlPort() == 0) mediaConfig.setRtmpSSlPort(zlmServerConfig.getRtmpSslPort());
                if (mediaConfig.getRtspPort() == 0) mediaConfig.setRtspPort(zlmServerConfig.getRtspPort());
                if (mediaConfig.getRtspSSLPort() == 0) mediaConfig.setRtspSSLPort(zlmServerConfig.getRtspSSlport());
                if (mediaConfig.getRtpProxyPort() == 0) mediaConfig.setRtpProxyPort(zlmServerConfig.getRtpProxyPort());
                mediaServerMapper.update(mediaConfig);
                serverItem = mediaConfig.getMediaSerItem();
                setZLMConfig(mediaConfig);
            }else {
                if (!serverItem.isDocker()) {
                    serverItem.setHttpPort(zlmServerConfig.getHttpPort());
                    serverItem.setHttpSSlPort(zlmServerConfig.getHttpSSLport());
                    serverItem.setRtmpPort(zlmServerConfig.getRtmpPort());
                    serverItem.setRtmpSSlPort(zlmServerConfig.getRtmpSslPort());
                    serverItem.setRtpProxyPort(zlmServerConfig.getRtpProxyPort());
                    serverItem.setRtspPort(zlmServerConfig.getRtspPort());

                }
                serverItem.setUpdateTime(now);
                mediaServerMapper.update(serverItem);
                setZLMConfig(serverItem);
            }
        }else {
            if (zlmServerConfig.getGeneralMediaServerId().equals(mediaConfig.getId())
                    || (zlmServerConfig.getIp().equals(mediaConfig.getIp()) && zlmServerConfig.getHttpPort() == mediaConfig.getHttpPort())) {
                mediaConfig.setId(zlmServerConfig.getGeneralMediaServerId());
                mediaConfig.setCreateTime(now);
                mediaConfig.setUpdateTime(now);
                serverItem = mediaConfig.getMediaSerItem();
                mediaServerMapper.add(mediaConfig);
            }else {
                // 一个新的zlm接入wvp
                serverItem = new MediaServerItem(zlmServerConfig, sipIp);
                serverItem.setCreateTime(now);
                serverItem.setUpdateTime(now);
                mediaServerMapper.add(serverItem);
            }
        }
        // 更新缓存
        if (zlmServerStatus.get(serverItem.getId()) == null) {
            zlmServers.put(serverItem.getId(), serverItem);
            zlmServerStatus.put(serverItem.getId(),0);
        }
        // 查询服务流数量
        IMediaServerItem finalServerItem = serverItem;
        zlmresTfulUtils.getMediaList(serverItem, null, null, "rtmp",(mediaList ->{
            Integer code = mediaList.getInteger("code");
            if (code == 0) {
                JSONArray data = mediaList.getJSONArray("data");
                if (data != null) {
                    zlmServerStatus.put(finalServerItem.getId(),data.size());
                }else {
                    zlmServerStatus.put(finalServerItem.getId(),0);
                }

            }
        }));

    }

    /**
     * 更新缓存
     * @param mediaServerItem zlm服务
     * @param count 在线数
     * @param online 在线状态
     */
    @Override
    public void updateServerCatch(IMediaServerItem mediaServerItem, Integer count, Boolean online) {
        if (mediaServerItem != null) {
            zlmServers.put(mediaServerItem.getId(), mediaServerItem);
            Collection<Integer> values = zlmServerStatus.values();
            if (online != null && count != null) {
                zlmServerStatus.put(mediaServerItem.getId(), count);
            }
        }
    }

    @Override
    public void addCount(String mediaServerId) {
        if (zlmServerStatus.get(mediaServerId) != null) {
            zlmServerStatus.put(mediaServerId, zlmServerStatus.get(mediaServerId) + 1);
        }
    }

    @Override
    public void removeCount(String mediaServerId) {
        if (zlmServerStatus.get(mediaServerId) != null) {
            zlmServerStatus.put(mediaServerId, zlmServerStatus.get(mediaServerId) - 1);
        }
    }

    /**
     * 获取负载最低的节点
     * @return MediaServerItem
     */
    @Override
    public IMediaServerItem getMediaServerForMinimumLoad() {
        int mediaCount = -1;
        String key = null;
        System.out.println(JSON.toJSONString(zlmServerStatus));
        if (zlmServerStatus.size() == 1) {
            Map.Entry entry = zlmServerStatus.entrySet().iterator().next();
            key= (String) entry.getKey();
        }else {
            for (String id : zlmServerStatus.keySet()) {
                if (key == null) {
                    key = id;
                    mediaCount = zlmServerStatus.get(id);
                }
                if (zlmServerStatus.get(id) == 0) {
                    key = id;
                    break;
                }else if (mediaCount >= zlmServerStatus.get(id)){
                    mediaCount = zlmServerStatus.get(id);
                    key = id;
                }
            }
        }

        if (key == null) {
            logger.info("获取负载最低的节点时无在线节点");
            return null;
        }else{
            return  zlmServers.get(key);
        }
    }

    /**
     * 对zlm服务器进行基础配置
     * @param mediaServerItem 服务ID
     */
    @Override
    public void setZLMConfig(IMediaServerItem mediaServerItem) {
        logger.info("[ {} ]-[ {}:{} ]设置zlm",
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
        param.put("hook.timeoutSec","20");
        param.put("general.streamNoneReaderDelayMS",mediaServerItem.getStreamNoneReaderDelayMS());

        JSONObject responseJSON = zlmresTfulUtils.setServerConfig(mediaServerItem, param);

        if (responseJSON != null && responseJSON.getInteger("code") == 0) {
            logger.info("[ {} ]-[ {}:{} ]设置zlm成功",
                    mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
        }else {
            logger.info("[ {} ]-[ {}:{} ]设置zlm失败" + responseJSON.getString("msg"),
                    mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
        }
    }
}
