package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description:针对 ZLMediaServer的hook事件订阅
 * @author: pan
 * @date:   2020年12月2日 21:17:32
 */
@Component
public class ZLMHttpHookSubscribe {

    public enum HookType{
        on_flow_report,
        on_http_access,
        on_play,
        on_publish,
        on_record_mp4,
        on_rtsp_auth,
        on_rtsp_realm,
        on_shell_login,
        on_stream_changed,
        on_stream_none_reader,
        on_stream_not_found,
        on_server_started
    }

    public interface Event{
        void response(JSONObject response);
    }

    private Map<HookType, Map<JSONObject, ZLMHttpHookSubscribe.Event>> allSubscribes = new ConcurrentHashMap<>();

    public void addSubscribe(HookType type, JSONObject hookResponse, ZLMHttpHookSubscribe.Event event) {
        Map<JSONObject, Event> eventMap = allSubscribes.get(type);
        if (eventMap == null) {
            eventMap = new HashMap<JSONObject, Event>();
            allSubscribes.put(type,eventMap);
        }
        eventMap.put(hookResponse, event);
    }

    public ZLMHttpHookSubscribe.Event getSubscribe(HookType type, JSONObject hookResponse) {
        ZLMHttpHookSubscribe.Event event= null;
        Map<JSONObject, Event> eventMap = allSubscribes.get(type);
        if (eventMap == null) {
            return null;
        }
        for (JSONObject key : eventMap.keySet()) {
            Boolean result = null;
            for (String s : key.keySet()) {
                if (result == null) {
                    result = key.getString(s).equals(hookResponse.getString(s));
                }else {
                    result = result && key.getString(s).equals(hookResponse.getString(s));
                }

            }
            if (result) {
                event = eventMap.get(key);
            }
        }
        return event;
    }

    /**
     * 获取某个类型的所有的订阅
     * @param type
     * @return
     */
    public List<ZLMHttpHookSubscribe.Event> getSubscribes(HookType type) {
        ZLMHttpHookSubscribe.Event event= null;
        Map<JSONObject, Event> eventMap = allSubscribes.get(type);
        if (eventMap == null) {
            return null;
        }
        List<ZLMHttpHookSubscribe.Event> result = new ArrayList<>();
        for (JSONObject key : eventMap.keySet()) {
            result.add(eventMap.get(key));
        }
        return result;
    }


}
