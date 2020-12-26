package com.genersoft.iot.vmp.gb28181.event;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import javax.sip.message.Request;
import java.util.EventObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SipSubscribe {

    private final static Logger logger = LoggerFactory.getLogger(SipSubscribe.class);

    private Map<String, SipSubscribe.Event> allSubscribes = new ConcurrentHashMap<>();

    public interface Event {
        void response(ResponseEvent event);
    }

    public void addSubscribe(String key, SipSubscribe.Event event) {
        allSubscribes.put(key, event);
    }

    public SipSubscribe.Event getSubscribe(String key) {
        return allSubscribes.get(key);
    }

    public int getSize(){
        return allSubscribes.size();
    }
}
