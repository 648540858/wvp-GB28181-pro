package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
//import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.service.IStreamProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(value=1)
public class ZLMRunner implements CommandLineRunner {

    private final static Logger logger = LoggerFactory.getLogger(ZLMRunner.class);

     @Autowired
     private IVideoManagerStorager storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Value("${media.ip}")
    private String mediaIp;

    @Value("${media.wanIp}")
    private String mediaWanIp;

    @Value("${media.hookIp}")
    private String mediaHookIp;

    @Value("${media.port}")
    private int mediaPort;

    @Value("${media.secret}")
    private String mediaSecret;

    @Value("${media.streamNoneReaderDelayMS}")
    private String streamNoneReaderDelayMS;

    @Value("${sip.ip}")
    private String sipIP;

    @Value("${server.port}")
    private String serverPort;

    @Value("${media.autoConfig}")
    private boolean autoConfig;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private ZLMMediaListManager zlmMediaListManager;

    @Autowired
    private ZLMHttpHookSubscribe hookSubscribe;

    @Autowired
    private IStreamProxyService streamProxyService;

    @Override
    public void run(String... strings) throws Exception {
        JSONObject subscribeKey = new JSONObject();
        // 订阅 zlm启动事件
        hookSubscribe.addSubscribe(ZLMHttpHookSubscribe.HookType.on_server_started,subscribeKey,(response)->{
            MediaServerConfig mediaServerConfig = JSONObject.toJavaObject(response, MediaServerConfig.class);
            zLmRunning(mediaServerConfig);
        });

        // 获取zlm信息
        logger.info("等待zlm接入...");
        MediaServerConfig mediaServerConfig = getMediaServerConfig();

        if (mediaServerConfig != null) {
            zLmRunning(mediaServerConfig);
        }
    }

    public MediaServerConfig getMediaServerConfig() {
        JSONObject responseJSON = zlmresTfulUtils.getMediaServerConfig();
        MediaServerConfig mediaServerConfig = null;
        if (responseJSON != null) {
            JSONArray data = responseJSON.getJSONArray("data");
            if (data != null && data.size() > 0) {
                mediaServerConfig = JSON.parseObject(JSON.toJSONString(data.get(0)), MediaServerConfig.class);

            }
        } else {
            logger.error("getMediaServerConfig失败, 1s后重试");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaServerConfig = getMediaServerConfig();
        }
        return mediaServerConfig;
    }

    private void saveZLMConfig() {
        logger.info("设置zlm...");
        if (StringUtils.isEmpty(mediaHookIp)) {
            mediaHookIp = sipIP;
        }

        String hookPrex = String.format("http://%s:%s/index/hook", mediaHookIp, serverPort);
        Map<String, Object> param = new HashMap<>();
        param.put("api.secret",mediaSecret); // -profile:v Baseline
        param.put("ffmpeg.cmd","%s -fflags nobuffer -rtsp_transport tcp -i %s -c:a aac -strict -2 -ar 44100 -ab 48k -c:v libx264  -f flv %s");
        param.put("hook.enable","1");
        param.put("hook.on_flow_report","");
        param.put("hook.on_play","");
        param.put("hook.on_http_access","");
        param.put("hook.on_publish",String.format("%s/on_publish", hookPrex));
        param.put("hook.on_record_mp4","");
        param.put("hook.on_record_ts","");
        param.put("hook.on_rtsp_auth","");
        param.put("hook.on_rtsp_realm","");
        param.put("hook.on_server_started",String.format("%s/on_server_started", hookPrex));
        param.put("hook.on_shell_login",String.format("%s/on_shell_login", hookPrex));
        param.put("hook.on_stream_changed",String.format("%s/on_stream_changed", hookPrex));
        param.put("hook.on_stream_none_reader",String.format("%s/on_stream_none_reader", hookPrex));
        param.put("hook.on_stream_not_found",String.format("%s/on_stream_not_found", hookPrex));
        param.put("hook.timeoutSec","20");
        param.put("general.streamNoneReaderDelayMS",streamNoneReaderDelayMS);

        JSONObject responseJSON = zlmresTfulUtils.setServerConfig(param);

        if (responseJSON != null && responseJSON.getInteger("code") == 0) {
            logger.info("设置zlm成功");
        }else {
            logger.info("设置zlm失败: " + responseJSON.getString("msg"));
        }
    }

    /**
     * zlm 连接成功或者zlm重启后
     */
    private void zLmRunning(MediaServerConfig mediaServerConfig){
        logger.info("zlm接入成功...");
        if (autoConfig) saveZLMConfig();
        MediaServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
        if (System.currentTimeMillis() - mediaInfo.getUpdateTime() < 50){
            logger.info("zlm刚刚更新，忽略这次更新");
            return;
        }
        mediaServerConfig.setLocalIP(mediaIp);
        mediaServerConfig.setWanIp(StringUtils.isEmpty(mediaWanIp)? mediaIp: mediaWanIp);
        redisCatchStorage.updateMediaInfo(mediaServerConfig);
        // 更新流列表
        zlmMediaListManager.updateMediaList();
        // 恢复流代理
        List<StreamProxyItem> streamProxyListForEnable = storager.getStreamProxyListForEnable(true);
        for (StreamProxyItem streamProxyDto : streamProxyListForEnable) {
            logger.info("恢复流代理，" + streamProxyDto.getApp() + "/" + streamProxyDto.getStream());
            streamProxyService.addStreamProxyToZlm(streamProxyDto);
        }
    }
}
