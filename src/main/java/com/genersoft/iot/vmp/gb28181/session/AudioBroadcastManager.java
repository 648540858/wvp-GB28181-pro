package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.gb28181.bean.AudioBroadcastCatch;
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
@Component
public class AudioBroadcastManager {

    public static Map<String, AudioBroadcastCatch> data = new ConcurrentHashMap<>();

    public void add(AudioBroadcastCatch audioBroadcastCatch) {
        this.update(audioBroadcastCatch);
    }

    public void update(AudioBroadcastCatch audioBroadcastCatch) {
        data.put(audioBroadcastCatch.getDeviceId() + audioBroadcastCatch.getChannelId(), audioBroadcastCatch);
    }

    public void del(String deviceId, String channelId) {
        data.remove(deviceId + channelId);
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
            if (key.equals(deviceId + channelId)) {
                return true;
            }
        }
        return false;
    }

    public AudioBroadcastCatch get(String deviceId, String channelId) {
        return data.get(deviceId + channelId);
    }
}
