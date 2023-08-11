package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.HookType;
import com.genersoft.iot.vmp.media.zlm.dto.IHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * ZLMediaServer的hook事件订阅
 * @author lin
 */
@Component
public class ZlmHttpHookSubscribe {

    private final static Logger logger = LoggerFactory.getLogger(ZlmHttpHookSubscribe.class);

    @FunctionalInterface
    public interface Event{
        void response(MediaServerItem mediaServerItem, HookParam hookParam);
    }

    private Map<HookType, Map<IHookSubscribe, ZlmHttpHookSubscribe.Event>> allSubscribes = new ConcurrentHashMap<>();

    public void addSubscribe(IHookSubscribe hookSubscribe, ZlmHttpHookSubscribe.Event event) {
        if (hookSubscribe.getExpires() == null) {
            // 默认5分钟过期
            Instant expiresInstant = Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(5));
            hookSubscribe.setExpires(expiresInstant);
        }
        allSubscribes.computeIfAbsent(hookSubscribe.getHookType(), k -> new ConcurrentHashMap<>()).put(hookSubscribe, event);
    }

    public ZlmHttpHookSubscribe.Event sendNotify(HookType type, JSONObject hookResponse) {
        ZlmHttpHookSubscribe.Event event= null;
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
            List<Map.Entry<IHookSubscribe, ZlmHttpHookSubscribe.Event>> entriesToRemove = new ArrayList<>();
            for (Map.Entry<IHookSubscribe, ZlmHttpHookSubscribe.Event> entry : entries) {
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
                if (result){
                    entriesToRemove.add(entry);
                }
            }

            if (!CollectionUtils.isEmpty(entriesToRemove)) {
                for (Map.Entry<IHookSubscribe, ZlmHttpHookSubscribe.Event> entry : entriesToRemove) {
                    eventMap.remove(entry.getKey());
                }
                if (eventMap.size() == 0) {
                    allSubscribes.remove(hookSubscribe.getHookType());
                }
            }

        }
    }

    /**
     * 获取某个类型的所有的订阅
     * @param type
     * @return
     */
    public List<ZlmHttpHookSubscribe.Event> getSubscribes(HookType type) {
        Map<IHookSubscribe, Event> eventMap = allSubscribes.get(type);
        if (eventMap == null) {
            return null;
        }
        List<ZlmHttpHookSubscribe.Event> result = new ArrayList<>();
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

    /**
     * 对订阅数据进行过期清理
     */
//    @Scheduled(cron="0 0/5 * * * ?")   //每5分钟执行一次
    @Scheduled(fixedRate = 2 * 1000)
    public void execute(){
        Instant instant = Instant.now().minusMillis(TimeUnit.MINUTES.toMillis(5));
        int total = 0;
        for (HookType hookType : allSubscribes.keySet()) {
            Map<IHookSubscribe, Event> hookSubscribeEventMap = allSubscribes.get(hookType);
            if (hookSubscribeEventMap.size() > 0) {
                for (IHookSubscribe hookSubscribe : hookSubscribeEventMap.keySet()) {
                    if (hookSubscribe.getExpires().isBefore(instant)) {
                        // 过期的
                        hookSubscribeEventMap.remove(hookSubscribe);
                        total ++;
                    }
                }
            }
        }
    }
}
