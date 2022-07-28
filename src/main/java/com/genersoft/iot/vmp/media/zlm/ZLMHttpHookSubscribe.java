package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.HookType;
import com.genersoft.iot.vmp.media.zlm.dto.IHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @description:针对 ZLMediaServer的hook事件订阅
 * @author: pan
 * @date:   2020年12月2日 21:17:32
 */
@Component
public class ZLMHttpHookSubscribe {

    @FunctionalInterface
    public interface Event{
        void response(MediaServerItem mediaServerItem, JSONObject response);
    }

    private Map<HookType, Map<IHookSubscribe, ZLMHttpHookSubscribe.Event>> allSubscribes = new ConcurrentHashMap<>();

    public void addSubscribe(IHookSubscribe hookSubscribe, ZLMHttpHookSubscribe.Event event) {
        if (hookSubscribe.getExpires() == null) {
            // 默认5分钟过期
            Instant expiresInstant = Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(5));
            hookSubscribe.setExpires(expiresInstant);
        }
        allSubscribes.computeIfAbsent(hookSubscribe.getHookType(), k -> new ConcurrentHashMap<>()).put(hookSubscribe, event);
    }

    public ZLMHttpHookSubscribe.Event sendNotify(HookType type, JSONObject hookResponse) {
        ZLMHttpHookSubscribe.Event event= null;
        Map<IHookSubscribe, Event> eventMap = allSubscribes.get(type);
        if (eventMap == null) {
            return null;
        }
        for (IHookSubscribe key : eventMap.keySet()) {
            Boolean result = null;
            for (String s : key.getContent().keySet()) {
                if (result == null) {
                    result = key.getContent().getString(s).equals(hookResponse.getString(s));
                }else {
                    if (key.getContent().getString(s) == null) {
                        continue;
                    }
                    result = result && key.getContent().getString(s).equals(hookResponse.getString(s));
                }
            }
            if (null != result && result) {
                event = eventMap.get(key);
            }
        }
        return event;
    }

    public void removeSubscribe(IHookSubscribe hookSubscribe) {
        Map<IHookSubscribe, Event> eventMap = allSubscribes.get(hookSubscribe.getHookType());
        if (eventMap == null) {
            return;
        }

        Set<Map.Entry<IHookSubscribe, Event>> entries = eventMap.entrySet();
        if (entries.size() > 0) {
            List<Map.Entry<IHookSubscribe, ZLMHttpHookSubscribe.Event>> entriesToRemove = new ArrayList<>();
            for (Map.Entry<IHookSubscribe, ZLMHttpHookSubscribe.Event> entry : entries) {
                JSONObject content = entry.getKey().getContent();
                if (content == null || content.size() == 0) {
                    entriesToRemove.add(entry);
                    continue;
                }
                Boolean result = null;
                for (String s : content.keySet()) {
                    if (result == null) {
                        result = content.getString(s).equals(hookSubscribe.getContent().getString(s));
                    }else {
                        if (content.getString(s) == null) {
                            continue;
                        }
                        result = result && content.getString(s).equals(hookSubscribe.getContent().getString(s));
                    }
                }
                if (null != result && result){
                    entriesToRemove.add(entry);
                }
            }

            if (!CollectionUtils.isEmpty(entriesToRemove)) {
                for (Map.Entry<IHookSubscribe, ZLMHttpHookSubscribe.Event> entry : entriesToRemove) {
                    entries.remove(entry);
                }
            }

        }
    }

    /**
     * 获取某个类型的所有的订阅
     * @param type
     * @return
     */
    public List<ZLMHttpHookSubscribe.Event> getSubscribes(HookType type) {
        Map<IHookSubscribe, Event> eventMap = allSubscribes.get(type);
        if (eventMap == null) {
            return null;
        }
        List<ZLMHttpHookSubscribe.Event> result = new ArrayList<>();
        for (IHookSubscribe key : eventMap.keySet()) {
            result.add(eventMap.get(key));
        }
        return result;
    }

    public List<IHookSubscribe> getAll(){
        ArrayList<IHookSubscribe> result = new ArrayList<>();
        Collection<Map<IHookSubscribe, Event>> values = allSubscribes.values();
        for (Map<IHookSubscribe, Event> value : values) {
            result.addAll(value.keySet());
        }
        return result;
    }


}
