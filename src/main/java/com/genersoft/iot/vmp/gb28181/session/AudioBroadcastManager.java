package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.AudioBroadcastCatch;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 语音广播消息管理类
 * @author lin
 */
@Slf4j
@Component
public class AudioBroadcastManager {

    @Autowired
    private SipConfig config;

    public static Map<String, AudioBroadcastCatch> data = new ConcurrentHashMap<>();


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
