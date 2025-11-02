package com.genersoft.iot.vmp.gb28181.utils;

import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Map;

public enum VectorTileUtils {
    INSTANCE;

    private Map<String, byte[]> vectorTileMap = new ConcurrentReferenceHashMap<>();

    public void addVectorTile(String key, byte[] content) {
        vectorTileMap.put(key, content);
    }

    public byte[] getVectorTile(String key) {
        return vectorTileMap.get(key);
    }
}
