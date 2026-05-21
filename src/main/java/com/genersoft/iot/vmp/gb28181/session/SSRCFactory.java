package com.genersoft.iot.vmp.gb28181.session;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SSRCFactory {

    private final ConcurrentHashMap<String, BitSet> usedMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ssrc-rebuild");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private UserSetting userSetting;

    private String domainPart;

    @PostConstruct
    public void init() {
        String sipDomain = sipConfig.getDomain();
        domainPart = sipDomain.length() >= 8 ? sipDomain.substring(3, 8) : sipDomain;
        scheduler.scheduleAtFixedRate(this::rebuild, 10, 30, TimeUnit.SECONDS);
    }

    public String getPlaySsrc(String mediaServerId) {
        String suffix = allocate(mediaServerId);
        return suffix != null ? "0" + suffix : null;
    }

    public String getPlayBackSsrc(String mediaServerId) {
        String suffix = allocate(mediaServerId);
        return suffix != null ? "1" + suffix : null;
    }

    public String getPlaySsrc(MediaServer mediaServer) {
        if (mediaServer.isRtpEnable() && userSetting.getSsrcRandom()) {
            return "0" + domainPart + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        }
        return getPlaySsrc(mediaServer.getId());
    }

    public String getPlayBackSsrc(MediaServer mediaServer) {
        if (mediaServer.isRtpEnable() && userSetting.getSsrcRandom()) {
            return "1" + domainPart + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        }
        return getPlayBackSsrc(mediaServer.getId());
    }

    private String allocate(String mediaServerId) {
        synchronized (lockMap.computeIfAbsent(mediaServerId, k -> new Object())) {
            BitSet bits = usedMap.computeIfAbsent(mediaServerId, k -> new BitSet(10000));
            int start = ThreadLocalRandom.current().nextInt(10000);
            int index = start;
            do {
                if (!bits.get(index)) {
                    bits.set(index);
                    return domainPart + String.format("%04d", index);
                }
                index = (index + 1) % 10000;
            } while (index != start);
            log.warn("[SSRC] 媒体节点 {} 的SSRC已用尽", mediaServerId);
            return null;
        }
    }

    void rebuild() {
        List<MediaServer> servers = mediaServerService.getAll();
        for (MediaServer server : servers) {
            if (server.isRtpEnable() && userSetting.getSsrcRandom()) {
                continue;
            }
            synchronized (lockMap.computeIfAbsent(server.getId(), k -> new Object())) {
                BitSet bits = new BitSet(10000);
                int count = 0;
                try {
                    ZLMResult<?> result = zlmresTfulUtils.getMediaList(server, null, null, "rtsp", null);
                    if (result != null && result.getCode() == 0 && result.getData() != null) {
                        List<JSONObject> list = (List<JSONObject>) result.getData();
                        for (JSONObject obj : list) {
                            if (obj.getIntValue("originType") != 3) continue;
                            String originUrl = obj.getString("originUrl");
                            if (originUrl == null) continue;
                            int idx = originUrl.lastIndexOf("/rtp/");
                            if (idx == -1) continue;
                            try {
                                int suffix = (int) (Long.parseLong(originUrl.substring(idx + 5), 16) % 10000);
                                bits.set(suffix);
                                count++;
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("[SSRC重建] 查询媒体节点 {} 失败: {}", server.getId(), e.getMessage());
                }
                usedMap.put(server.getId(), bits);
                if (count > 8000) {
                    log.info("[SSRC重建] 媒体节点 {} 的SSRC使用率已超过80%，请注意扩展服务提升性能", server.getId());
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("[SSRC重建] 节点 {} 已占用 {} 个SSRC", server.getId(), count);
                    }
                }
            }
        }
    }
}
