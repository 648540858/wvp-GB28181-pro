package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.AudioBroadcastCatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音广播消息管理类
 * @author lin
 */
@Slf4j
@Component
public class AudioBroadcastManager {

    @Autowired
    private SipConfig config;

    public static Map<Integer, AudioBroadcastCatch> data = new ConcurrentHashMap<>();


    public void update(AudioBroadcastCatch audioBroadcastCatch) {
        data.put(audioBroadcastCatch.getChannelId(), audioBroadcastCatch);
    }

    public void del(Integer channelId) {
        data.remove(channelId);

    }

    public List<AudioBroadcastCatch> getAll(){
        Collection<AudioBroadcastCatch> values = data.values();
        return new ArrayList<>(values);
    }


    public boolean exit(Integer channelId) {
        return data.containsKey(channelId);
    }

    public AudioBroadcastCatch get(Integer channelId) {
        return data.get(channelId);
    }

    public List<AudioBroadcastCatch> getByDeviceId(String deviceId) {
        List<AudioBroadcastCatch> audioBroadcastCatchList= new ArrayList<>();
        for (AudioBroadcastCatch broadcastCatch : data.values()) {
            if (broadcastCatch.getDeviceId().equals(deviceId)) {
                audioBroadcastCatchList.add(broadcastCatch);
            }
        }

        return audioBroadcastCatchList;
    }
}
