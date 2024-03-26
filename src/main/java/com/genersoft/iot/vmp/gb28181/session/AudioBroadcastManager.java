package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.AudioBroadcastCatch;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.InviteStreamType;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.event.MediaDepartureEvent;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 语音广播消息管理类
 * @author lin
 */
@Component
public class AudioBroadcastManager {

    private final static Logger logger = LoggerFactory.getLogger(AudioBroadcastManager.class);

    @Autowired
    private SipConfig config;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IDeviceService deviceService;

    public static Map<String, AudioBroadcastCatch> data = new ConcurrentHashMap<>();

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServerByStream(event.getStream());
        if (!sendRtpItems.isEmpty()) {
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                if (sendRtpItem != null && sendRtpItem.getApp().equals(event.getApp())) {
                    String platformId = sendRtpItem.getPlatformId();
                    Device device = deviceService.getDevice(platformId);
                    try {
                        if (device != null) {
                            cmder.streamByeCmd(device, sendRtpItem.getChannelId(), event.getStream(), sendRtpItem.getCallId());
                            if (sendRtpItem.getPlayType().equals(InviteStreamType.BROADCAST)
                                    || sendRtpItem.getPlayType().equals(InviteStreamType.TALK)) {
                                AudioBroadcastCatch audioBroadcastCatch = get(sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
                                if (audioBroadcastCatch != null) {
                                    // 来自上级平台的停止对讲
                                    logger.info("[停止对讲] 来自上级，平台：{}, 通道：{}", sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
                                    del(sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
                                }
                            }
                        }
                    } catch (SipException | InvalidArgumentException | ParseException |
                             SsrcTransactionNotFoundException e) {
                        logger.error("[命令发送失败] 发送BYE: {}", e.getMessage());
                    }
                }
            }
        }
    }

    public void update(AudioBroadcastCatch audioBroadcastCatch) {
        if (SipUtils.isFrontEnd(audioBroadcastCatch.getDeviceId())) {
            audioBroadcastCatch.setChannelId(audioBroadcastCatch.getDeviceId());
            data.put(audioBroadcastCatch.getDeviceId(), audioBroadcastCatch);
        }else {
            data.put(audioBroadcastCatch.getDeviceId() + audioBroadcastCatch.getChannelId(), audioBroadcastCatch);
        }
    }

    public void del(String deviceId, String channelId) {
        if (SipUtils.isFrontEnd(deviceId)) {
            data.remove(deviceId);
        }else {
            data.remove(deviceId + channelId);
        }

    }

    public void delByDeviceId(String deviceId) {
        for (String key : data.keySet()) {
            if (key.startsWith(deviceId)) {
                data.remove(key);
            }
        }
    }

    public List<AudioBroadcastCatch> getAll(){
        Collection<AudioBroadcastCatch> values = data.values();
        return new ArrayList<>(values);
    }


    public boolean exit(String deviceId, String channelId) {
        for (String key : data.keySet()) {
            if (SipUtils.isFrontEnd(deviceId)) {
                return key.equals(deviceId);
            }else {
                return key.equals(deviceId + channelId);
            }
        }
        return false;
    }

    public AudioBroadcastCatch get(String deviceId, String channelId) {
        AudioBroadcastCatch audioBroadcastCatch;
        if (SipUtils.isFrontEnd(deviceId)) {
            audioBroadcastCatch = data.get(deviceId);
        }else {
            audioBroadcastCatch = data.get(deviceId + channelId);
        }
        if (audioBroadcastCatch == null) {
            Stream<AudioBroadcastCatch> allAudioBroadcastCatchStreamForDevice = data.values().stream().filter(
                    audioBroadcastCatchItem -> Objects.equals(audioBroadcastCatchItem.getDeviceId(), deviceId));
            List<AudioBroadcastCatch> audioBroadcastCatchList = allAudioBroadcastCatchStreamForDevice.collect(Collectors.toList());
            if (audioBroadcastCatchList.size() == 1 && Objects.equals(config.getId(), channelId)) {
                audioBroadcastCatch = audioBroadcastCatchList.get(0);
            }
        }

        return audioBroadcastCatch;
    }

    public List<AudioBroadcastCatch> get(String deviceId) {
        List<AudioBroadcastCatch> audioBroadcastCatchList= new ArrayList<>();
        if (SipUtils.isFrontEnd(deviceId)) {
            if (data.get(deviceId) != null) {
                audioBroadcastCatchList.add(data.get(deviceId));
            }
        }else {
            for (String key : data.keySet()) {
                if (key.startsWith(deviceId)) {
                    audioBroadcastCatchList.add(data.get(key));
                }
            }
        }

        return audioBroadcastCatchList;
    }
}
