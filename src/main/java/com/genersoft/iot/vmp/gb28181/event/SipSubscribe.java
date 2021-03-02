package com.genersoft.iot.vmp.gb28181.event;

import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SipSubscribe {

    private Map<String, SipSubscribe.Event> errorSubscribes = new ConcurrentHashMap<>();

    private Map<String, SipSubscribe.Event> okSubscribes = new ConcurrentHashMap<>();

    public interface Event {
        void response(ResponseEvent event);
    }

    public void addErrorSubscribe(String key, SipSubscribe.Event event) {
        errorSubscribes.put(key, event);
    }

    public void addOkSubscribe(String key, SipSubscribe.Event event) {
        okSubscribes.put(key, event);
    }

    public SipSubscribe.Event getErrorSubscribe(String key) {
        return errorSubscribes.get(key);
    }

    public SipSubscribe.Event getOkSubscribe(String key) {
        return okSubscribes.get(key);
    }

    public int getErrorSubscribesSize(){
        return errorSubscribes.size();
    }
    public int getOkSubscribesSize(){
        return okSubscribes.size();
    }
}
