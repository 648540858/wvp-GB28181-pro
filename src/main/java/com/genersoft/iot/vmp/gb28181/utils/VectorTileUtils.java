package com.genersoft.iot.vmp.gb28181.utils;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.VectorTileSource;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.List;
import java.util.Map;

public enum VectorTileUtils {
    INSTANCE;

    private Map<String, VectorTileSource> vectorTileMap = new ConcurrentReferenceHashMap<>();

    public void addVectorTile(String id, String key, byte[] content) {
        VectorTileSource vectorTileSource = vectorTileMap.get(id);
        if (vectorTileSource == null) {
            vectorTileSource = new VectorTileSource();
            vectorTileMap.put(id, vectorTileSource);
        }

        vectorTileSource.getVectorTileMap().put(key, content);
    }

    public byte[] getVectorTile(String id, String key) {
        if (!vectorTileMap.containsKey(id)) {
            return null;
        }
        return vectorTileMap.get(id).getVectorTileMap().get(key);
    }

    public void addSource(String id, List<CommonGBChannel> channelList) {
        VectorTileSource vectorTileSource = vectorTileMap.get(id);
        if (vectorTileSource == null) {
            vectorTileSource = new VectorTileSource();
            vectorTileMap.put(id, vectorTileSource);
        }
        vectorTileMap.get(id).getChannelList().addAll(channelList);
    }


    public void remove(String id) {
        vectorTileMap.remove(id);
    }

    public List<CommonGBChannel> getChannelList(String id) {
        if (!vectorTileMap.containsKey(id)) {
            return null;
        }
        return vectorTileMap.get(id).getChannelList();
    }

    public void save(String id) {
        if (!vectorTileMap.containsKey(id)) {
            return;
        }
        VectorTileSource vectorTileSource = vectorTileMap.get(id);
        if (vectorTileSource == null) {
            return;
        }
        vectorTileMap.remove(id);
        vectorTileMap.put("DEFAULT", vectorTileSource);
    }


}
