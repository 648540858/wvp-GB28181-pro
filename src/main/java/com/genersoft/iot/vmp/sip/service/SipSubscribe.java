package com.genersoft.iot.vmp.sip.service;


import com.genersoft.iot.vmp.sip.bean.SipEvent;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lin
 */

public class SipSubscribe {

    private static SipSubscribe instance;

    private SipSubscribe() {}

    public static SipSubscribe getInstance() {
        if (instance == null) {
            synchronized (SipSubscribe.class) {
                if (instance == null) {
                    instance = new SipSubscribe();
                }
            }
        }

        return instance;
    }

    private Map<String, SipEvent> errorSubscribes = new ConcurrentHashMap<>();

    private Map<String, SipEvent> okSubscribes = new ConcurrentHashMap<>();

    private Map<String, Instant> okTimeSubscribes = new ConcurrentHashMap<>();

    private Map<String, Instant> errorTimeSubscribes = new ConcurrentHashMap<>();


    public void addErrorSubscribe(String key, SipEvent event) {
        errorSubscribes.put(key, event);
        errorTimeSubscribes.put(key, Instant.now());
    }

    public void addOkSubscribe(String key, SipEvent event) {
        okSubscribes.put(key, event);
        okTimeSubscribes.put(key, Instant.now());
    }

    public SipEvent getErrorSubscribe(String key) {
        return errorSubscribes.get(key);
    }

    public void removeErrorSubscribe(String key) {
        if(key == null){
            return;
        }
        errorSubscribes.remove(key);
        errorTimeSubscribes.remove(key);
    }

    public SipEvent getOkSubscribe(String key) {
        return okSubscribes.get(key);
    }

    public void removeOkSubscribe(String key) {
        if(key == null){
            return;
        }
        okSubscribes.remove(key);
        okTimeSubscribes.remove(key);
    }
    public int getErrorSubscribesSize(){
        return errorSubscribes.size();
    }
    public int getOkSubscribesSize(){
        return okSubscribes.size();
    }
}
