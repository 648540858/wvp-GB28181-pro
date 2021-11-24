package com.genersoft.iot.vmp.vmanager.gb28181.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName: InfoCseqCache
 * @Description: INFO类型的Sip中cseq的缓存
 */
public class InfoCseqCache {

    public static Map<String, Long> CSEQCACHE = new ConcurrentHashMap<>();

}