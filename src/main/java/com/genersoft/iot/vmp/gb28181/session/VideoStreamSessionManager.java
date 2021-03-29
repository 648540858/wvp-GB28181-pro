package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.SsrcConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.redis.JedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sip.ClientTransaction;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description:视频流session管理器，管理视频预览、预览回放的通信句柄
 * @author: swwheihei
 * @date: 2020年5月13日 下午4:03:02
 */
@Slf4j
@Component
public class VideoStreamSessionManager {
    /**
     * key: ssrc 播流会话句柄(streamId)和同步信源(SSRC)的对应关系
     * value: 流媒体服务器
     */
    private ConcurrentHashMap<String, ClientTransaction> sessionMap = new ConcurrentHashMap<>();
    private String ssrcPrefix;

    @Autowired
    private SipConfig sipConfig;
    @Autowired
    private MediaConfig mediaConfig;
    @Autowired
    private JedisUtil jedisUtil;
    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @PostConstruct
    public void init() {
        this.ssrcPrefix = sipConfig.getSipDomain().substring(3, 8);
    }

    /**
     * 获取视频预览的会话信息。
     */
    public StreamInfo createPlayStreamInfo(Device device, String channelId) {
        // SSRC值,第一位固定为0
        StreamInfo streamInfo = createStreamInfo(device, channelId, PlayTypeEnum.PLAY);
        // 会话句柄和ZLM服务器的对应关系，存到redis，防止服务器宕机数据丢失。
        redisCatchStorage.startPlay(streamInfo);
        return streamInfo;
    }

    /**
     * 获取录像回放的会话信息
     * 会话句柄和ZLM服务器的对应关系，存到redis，防止服务器宕机数据丢失
     */
    public StreamInfo createPlayBackStreamInfo(Device device, String channelId) {
        // SSRC值,第一位固定为1
        StreamInfo streamInfo = createStreamInfo(device, channelId, PlayTypeEnum.PLAY_BACK);
        // 会话句柄和ZLM服务器的对应关系，存到redis，防止服务器宕机数据丢失。
        redisCatchStorage.startPlayback(streamInfo);
        return streamInfo;
    }

    /**
     * 1、选举ZLM服务器
     * 2、分配SSRC
     * 3、生成streamId和播流RUL，如果此时未连接ZLM，会抛出运行时异常
     * 4、已分配SSRC存储到redis，防止服务器宕机后数据丢失。
     */
    private StreamInfo createStreamInfo(Device device, String channelId, PlayTypeEnum playType) {
        // 1、选举ZLM服务器
        SsrcConfig ssrcConfig = elect();
        List<String> isUsed = ssrcConfig.getIsUsed();
        List<String> notUsed = ssrcConfig.getNotUsed();

        // 2、分配SSRC
        String sn;
        int index = 0;
        if (notUsed.size() == 0) {
            throw new RuntimeException("ssrc已经用完");
        } else if (notUsed.size() == 1) {
            sn = notUsed.get(0);
        } else {
            index = new Random().nextInt(notUsed.size() - 1);
            sn = notUsed.get(index);
        }
        String ssrc = playType.getValue() + ssrcPrefix + sn;
        String mediaServerIp = ssrcConfig.getMediaServerIp();

        // 3、生成streamId和播流RUL，如果此时未连接ZLM，会抛出运行时异常
        StreamInfo streamInfo = initStreamInfo(device, channelId, ssrc, mediaServerIp);

        // 4、已分配SSRC存储到redis，防止服务器宕机后数据丢失。
        jedisUtil.sadd(VideoManagerConstants.MEDIA_SSRC_USED_PREFIX + mediaServerIp, sn);
        notUsed.remove(index);
        isUsed.add(sn);
        return streamInfo;
    }

    /**
     * 流媒体服务器选举算法
     *
     * @return
     */
    private SsrcConfig elect() {
        Set<Map.Entry<String, SsrcConfig>> entries = mediaConfig.getMediaServerSsrcMap().entrySet();
        SsrcConfig min = null;
        for (Map.Entry<String, SsrcConfig> e : entries) {
            SsrcConfig vo = e.getValue();
            if (null == min) {
                min = vo;
                continue;
            }
            if (vo.getNotUsed().size() > min.getNotUsed().size()) {
                min = vo;
            }
        }
        return min;
    }

    /**
     * 生成streamId和播流RUL，如果此时未连接ZLM，会抛出运行时异常
     *
     * @param device
     * @param channelId
     * @param ssrc
     * @param mediaServerIp
     * @return
     */
    private StreamInfo initStreamInfo(Device device, String channelId, String ssrc, String mediaServerIp) {
        String streamId;
        if (ssrc.startsWith(PlayTypeEnum.PLAY.getValue()) && mediaConfig.getRtpEnable()) {
            streamId = String.format("gb_play_%s_%s", device.getDeviceId(), channelId);
        } else {
            streamId = String.format("%08x", Integer.parseInt(ssrc)).toUpperCase();
        }
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.setMediaServerIp(mediaServerIp);
        streamInfo.setSsrc(ssrc);
        streamInfo.setStreamId(streamId);
        streamInfo.setDeviceID(device.getDeviceId());
        streamInfo.setChannelId(channelId);
        MediaServerConfig mediaServerConfig = redisCatchStorage.getMediaInfo();
        if (null == mediaServerConfig) {
            throw new RuntimeException("点播时发现ZLM尚未连接...");
        }

        streamInfo.setFlv(String.format("http://%s:%s/rtp/%s.flv", mediaServerIp, mediaServerConfig.getHttpPort(), streamId));
        streamInfo.setWs_flv(String.format("ws://%s:%s/rtp/%s.flv", mediaServerIp, mediaServerConfig.getHttpPort(), streamId));

        streamInfo.setFmp4(String.format("http://%s:%s/rtp/%s.live.mp4", mediaServerIp, mediaServerConfig.getHttpPort(), streamId));
        streamInfo.setWs_fmp4(String.format("ws://%s:%s/rtp/%s.live.mp4", mediaServerIp, mediaServerConfig.getHttpPort(), streamId));

        streamInfo.setHls(String.format("http://%s:%s/rtp/%s/hls.m3u8", mediaServerIp, mediaServerConfig.getHttpPort(), streamId));
        streamInfo.setWs_hls(String.format("ws://%s:%s/rtp/%s/hls.m3u8", mediaServerIp, mediaServerConfig.getHttpPort(), streamId));

        streamInfo.setTs(String.format("http://%s:%s/rtp/%s.live.ts", mediaServerIp, mediaServerConfig.getHttpPort(), streamId));
        streamInfo.setWs_ts(String.format("ws://%s:%s/rtp/%s.live.ts", mediaServerIp, mediaServerConfig.getHttpPort(), streamId));

        streamInfo.setRtmp(String.format("rtmp://%s:%s/rtp/%s", mediaServerIp, mediaServerConfig.getRtmpPort(), streamId));
        streamInfo.setRtsp(String.format("rtsp://%s:%s/rtp/%s", mediaServerIp, mediaServerConfig.getRtspPort(), streamId));

        return streamInfo;
    }

    /**
     * 查找IPC通道播流使用流媒体服务器的IP
     *
     * @param channelId
     * @param streamId
     * @return
     */
    public String getMediaServerIp(String channelId, String streamId) {
        StreamInfo streamInfo = this.getStreamInfo(channelId, streamId);
        return null == streamInfo ? null : streamInfo.getMediaServerIp();
    }

    public StreamInfo getPlayStreamInfo(String channelId) {
        if (StringUtils.isBlank(channelId)) {
            log.error("getPlayStreamInfo channelId can not be null!!!");
            return null;
        }
        return redisCatchStorage.queryPlayByChannel(channelId);
    }

    public StreamInfo getPlayBackStreamInfo(String channelId) {
        if (StringUtils.isBlank(channelId)) {
            log.error("getPlayBackStreamInfo channelId can not be null!!!");
            return null;
        }
        return redisCatchStorage.queryPlaybackByChannel(channelId);
    }

    public StreamInfo getStreamInfo(String channelId, String streamId) {
        if (StringUtils.isBlank(channelId) || StringUtils.isBlank(streamId)) {
            log.error("getStreamInfo channelId and streamId can not be null!!!");
            return null;
        }
        StreamInfo streamInfo = getStreamInfo(channelId, streamId, PlayTypeEnum.PLAY);
        if (null == streamInfo) {
            streamInfo = getStreamInfo(channelId, streamId, PlayTypeEnum.PLAY_BACK);
        }
        return streamInfo;
    }

    private StreamInfo getStreamInfo(String channelId, String streamId, PlayTypeEnum playType) {
        if (StringUtils.isBlank(channelId) || StringUtils.isBlank(streamId)) {
            log.error("getStreamInfo channelId and streamId can not be null!!!");
            return null;
        }
        // TODO channelId
        if (null == playType || PlayTypeEnum.PLAY.equals(playType)) {
            return redisCatchStorage.queryPlayByStreamId(channelId, streamId);
        } else {
            return redisCatchStorage.queryPlaybackByStreamId(channelId, streamId);
        }
    }

    /**
     * 存储会话
     *
     * @param channelId
     * @param streamId
     * @param transaction
     */
    public void putClientTransaction(String channelId, String streamId, ClientTransaction transaction) {
        String streamKey = getStreamKey(channelId, streamId);
        sessionMap.put(streamKey, transaction);
    }

    public ClientTransaction getClientTransaction(String channelId, String streamId) {
        String streamKey = getStreamKey(channelId, streamId);
        return sessionMap.get(streamKey);
    }

    public void remove(String channelId, String streamId) {
        StreamInfo streamInfo = this.getStreamInfo(channelId, streamId);
        if (null == streamId) {
            return;
        }
        this.remove(streamInfo);
    }

    /**
     * 移除会话并释放ssrc，主要用完的ssrc一定要释放，否则会耗尽
     */
    public void remove(StreamInfo streamInfo) {
        String streamKey = getStreamKey(streamInfo.getChannelId(), streamInfo.getStreamId());
        // 移除会话
        sessionMap.remove(streamKey);

        String ssrc = streamInfo.getSsrc();
        String sn = ssrc.substring(6);
        String mediaServerIp = streamInfo.getMediaServerIp();
        // 释放ssrc，并从redis移除
        jedisUtil.srem(VideoManagerConstants.MEDIA_SSRC_USED_PREFIX + mediaServerIp, sn);
        SsrcConfig ssrcConfig = mediaConfig.getMediaServerSsrcMap().get(mediaServerIp);
        ssrcConfig.getIsUsed().remove(sn);
        ssrcConfig.getNotUsed().add(sn);

        // 会话句柄和ZLM服务器的对应关系，从redis移除
        if (ssrc.startsWith(PlayTypeEnum.PLAY.getValue())) {
            redisCatchStorage.stopPlay(streamInfo);
        } else {
            redisCatchStorage.stopPlayback(streamInfo);
        }
    }

    private static String getStreamKey(String channelId, String streamId) {
        return channelId + "_" + streamId;
    }
}
