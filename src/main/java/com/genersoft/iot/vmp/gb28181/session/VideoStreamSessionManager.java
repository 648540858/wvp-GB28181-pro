package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.utils.JsonUtil;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import gov.nist.javax.sip.message.SIPResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频流session管理器，管理视频预览、预览回放的通信句柄
 */
@Component
public class VideoStreamSessionManager {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 添加一个点播/回放的事务信息
     * 后续可以通过流Id/callID
     *
     * @param deviceId      设备ID
     * @param channelId     通道ID
     * @param callId        一次请求的CallID
     * @param stream        流名称
     * @param mediaServerId 所使用的流媒体ID
     * @param response      回复
     */
    public void put(String deviceId, String channelId, String callId, String stream, String ssrc, String mediaServerId, SIPResponse response, InviteSessionType type) {
        SsrcTransaction ssrcTransaction = new SsrcTransaction();
        ssrcTransaction.setDeviceId(deviceId);
        ssrcTransaction.setChannelId(channelId);
        ssrcTransaction.setStream(stream);
        ssrcTransaction.setSipTransactionInfo(new SipTransactionInfo(response));
        ssrcTransaction.setCallId(callId);
        ssrcTransaction.setSsrc(ssrc);
        ssrcTransaction.setMediaServerId(mediaServerId);
        ssrcTransaction.setType(type);

        redisTemplate.opsForValue().set(VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetting.getServerId()
                + "_" + deviceId + "_" + channelId + "_" + callId + "_" + stream, ssrcTransaction);
    }

    public SsrcTransaction getSsrcTransaction(String deviceId, String channelId, String callId, String stream) {

        if (ObjectUtils.isEmpty(deviceId)) {
            deviceId = "*";
        }
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "*";
        }
        if (ObjectUtils.isEmpty(callId)) {
            callId = "*";
        }
        if (ObjectUtils.isEmpty(stream)) {
            stream = "*";
        }
        String key = VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetting.getServerId() + "_" + deviceId + "_" + channelId + "_" + callId + "_" + stream;
        List<Object> scanResult = RedisUtil.scan(redisTemplate, key);
        if (scanResult.size() == 0) {
            return null;
        }
        return (SsrcTransaction) redisTemplate.opsForValue().get(scanResult.get(0));
    }

    /**
     * 更新缓存事务.
     *
     * @param inviteInfo 点播信息
     * @param ssrc 下级平台自定义 ssrc
     */
    public void updateSsrcTransactionForStream(InviteInfo inviteInfo, String ssrc) {
        String stream = String.format("%08x", Integer.parseInt(ssrc)).toUpperCase();

        SsrcTransaction ssrcTransactionInDb = getSsrcTransaction(inviteInfo.getDeviceId(), inviteInfo.getChannelId(),
                inviteInfo.getType().toString().toLowerCase(), inviteInfo.getStream());
        if (ObjectUtils.isEmpty(ssrcTransactionInDb)) {
            return;
        }

        remove(ssrcTransactionInDb.getDeviceId(), ssrcTransactionInDb.getChannelId(),
                ssrcTransactionInDb.getCallId(), ssrcTransactionInDb.getStream());
        String key = VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX
                + userSetting.getServerId()
                + "_" + ssrcTransactionInDb.getDeviceId()
                + "_" + ssrcTransactionInDb.getChannelId()
                + "_" + ssrcTransactionInDb.getCallId()
                + "_" + stream;
        ssrcTransactionInDb.setSsrc(ssrc);
        ssrcTransactionInDb.setStream(stream);
        redisTemplate.opsForValue().set(key, ssrcTransactionInDb);
    }

    public List<SsrcTransaction> getSsrcTransactionForAll(String deviceId, String channelId, String callId, String stream) {
        if (ObjectUtils.isEmpty(deviceId)) {
            deviceId = "*";
        }
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "*";
        }
        if (ObjectUtils.isEmpty(callId)) {
            callId = "*";
        }
        if (ObjectUtils.isEmpty(stream)) {
            stream = "*";
        }
        String key = VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetting.getServerId() + "_" + deviceId + "_" + channelId + "_" + callId + "_" + stream;
        List<Object> scanResult = RedisUtil.scan(redisTemplate, key);
        if (scanResult.size() == 0) {
            return null;
        }
        List<SsrcTransaction> result = new ArrayList<>();
        for (Object keyObj : scanResult) {
            result.add((SsrcTransaction) redisTemplate.opsForValue().get(keyObj));
        }
        return result;
    }

    public String getMediaServerId(String deviceId, String channelId, String stream) {
        SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId, null, stream);
        if (ssrcTransaction == null) {
            return null;
        }
        return ssrcTransaction.getMediaServerId();
    }

    public String getSSRC(String deviceId, String channelId, String stream) {
        SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId, null, stream);
        if (ssrcTransaction == null) {
            return null;
        }
        return ssrcTransaction.getSsrc();
    }

    public void remove(String deviceId, String channelId, String callId, String stream) {
        SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId, callId, stream);
        if (ssrcTransaction == null) {
            return;
        }
        redisTemplate.delete(VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetting.getServerId() + "_"
                + deviceId + "_" + channelId + "_" + ssrcTransaction.getCallId() + "_" + ssrcTransaction.getStream());
    }

    public void remove(String deviceId, String channelId, String stream) {
        remove(deviceId, channelId, null, stream);
    }

    public List<SsrcTransaction> getAllSsrc() {
        List<Object> ssrcTransactionKeys = RedisUtil.scan(redisTemplate, String.format("%s_*_*_*_*", VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetting.getServerId()));
        List<SsrcTransaction> result = new ArrayList<>();
        for (Object ssrcTransactionKey : ssrcTransactionKeys) {
            String key = (String) ssrcTransactionKey;
            SsrcTransaction ssrcTransaction = JsonUtil.redisJsonToObject(redisTemplate, key, SsrcTransaction.class);
            result.add(ssrcTransaction);
        }
        return result;
    }
}
