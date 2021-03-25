package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Order(value = 1)
public class ZLMRunner implements CommandLineRunner {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Value("${server.port}")
    private String serverPort;
    @Autowired
    MediaConfig mediaConfig;
    @Autowired
    SipConfig sipConfig;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Override
    public void run(String... strings) {
        String[] mediaIpArr = mediaConfig.getMediaIpArr();
        for (String mediaIp : mediaIpArr) {
            // 获取zlm信息
            log.info("等待zlm {} 接入...", mediaIp);
            MediaServerConfig mediaServerConfig = getMediaServerConfig(mediaIp);
            if (mediaServerConfig != null) {
                log.info("zlm {} 接入成功...", mediaIp);
                if (mediaConfig.getAutoConfig()) {
                    // 自动配置zlm
                    saveZLMConfig(mediaIp);
                    // 配置后，从zlm重新获取流媒体服务器信息
                    mediaServerConfig = getMediaServerConfig(mediaIp);
                }
                // TODO 这里需要把多台zlm服务器的配置，分别存储到redis，不能用同一个key，否则会覆盖
                redisCatchStorage.updateMediaInfo(mediaServerConfig);
            }
        }
    }

    public MediaServerConfig getMediaServerConfig(String mediaIp) {
        JSONObject responseJSON = zlmresTfulUtils.getMediaServerConfig(mediaIp);
        MediaServerConfig mediaServerConfig = null;
        if (responseJSON != null) {
            JSONArray data = responseJSON.getJSONArray("data");
            if (data != null && data.size() > 0) {
                mediaServerConfig = JSON.parseObject(JSON.toJSONString(data.get(0)), MediaServerConfig.class);
            }
        } else {
            log.error("getMediaServerConfig失败, 1s后重试");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaServerConfig = getMediaServerConfig(mediaIp);
        }
        return mediaServerConfig;
    }

    private void saveZLMConfig(String mediaIp) {
        log.info("设置zlm {} ...", mediaIp);
        String mediaHookIp = mediaConfig.getMediaHookIp();
        if (StringUtils.isEmpty(mediaHookIp)) {
            mediaHookIp = sipConfig.getSipIp();
        }
        String hookPrex = String.format("http://%s:%s/index/hook", mediaHookIp, serverPort);
        Map<String, Object> param = new HashMap<>();
        param.put("api.secret", mediaConfig.getMediaSecret()); // -profile:v Baseline
        param.put("ffmpeg.cmd", "%s -fflags nobuffer -rtsp_transport tcp -i %s -c:a aac -strict -2 -ar 44100 -ab 48k -c:v libx264  -f flv %s");
        param.put("hook.enable", "1");
        param.put("hook.on_flow_report", "");
        param.put("hook.on_play", "");
        param.put("hook.on_http_access", "");
        param.put("hook.on_publish", String.format("%s/on_publish", hookPrex));
        param.put("hook.on_record_mp4", "");
        param.put("hook.on_record_ts", "");
        param.put("hook.on_rtsp_auth", "");
        param.put("hook.on_rtsp_realm", "");
        param.put("hook.on_server_started", String.format("%s/on_server_started", hookPrex));
        param.put("hook.on_shell_login", String.format("%s/on_shell_login", hookPrex));
        param.put("hook.on_stream_changed", String.format("%s/on_stream_changed", hookPrex));
        param.put("hook.on_stream_none_reader", String.format("%s/on_stream_none_reader", hookPrex));
        param.put("hook.on_stream_not_found", String.format("%s/on_stream_not_found", hookPrex));
        param.put("hook.timeoutSec", "20");
        param.put("general.streamNoneReaderDelayMS", mediaConfig.getStreamNoneReaderDelayMS());

        JSONObject responseJSON = zlmresTfulUtils.setServerConfig(mediaIp, param);

        if (responseJSON != null && responseJSON.getInteger("code") == 0) {
            log.info("设置zlm {} 成功", mediaIp);
        } else {
            log.info("设置zlm {} 失败: {}", mediaIp, responseJSON);
        }
    }
}
