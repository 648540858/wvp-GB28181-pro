package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description:针对 ZLMediaServer的hook事件订阅
 * @author: pan
 * @date:   2020年12月2日 21:17:32
 */
@Component
public class ZLMHttpHookSubscribe {

    private final static Logger logger = LoggerFactory.getLogger(ZLMHttpHookSubscribe.class);

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
                String string = hookResponse.getString(s);
                String string1 = key.getString(s);
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
}
