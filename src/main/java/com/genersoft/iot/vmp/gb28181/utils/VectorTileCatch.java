package com.genersoft.iot.vmp.gb28181.utils;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.VectorTileSource;
import com.genersoft.iot.vmp.utils.DelayQueueConsumer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.DelayQueue;

@Slf4j
@Component
public class VectorTileCatch {

    private final Map<String, VectorTileSource> vectorTileMap = new ConcurrentReferenceHashMap<>();
    private final DelayQueue<VectorTileSource> delayQueue = new DelayQueue<>();

    private Thread consumerThread;

    @PostConstruct
    public void start() {
        consumerThread = DelayQueueConsumer.start("vector-tile-consumer", delayQueue,
                vectorTileSource -> vectorTileMap.remove(vectorTileSource.getId()));
    }

    @PreDestroy
    public void stop() {
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }

    public void addVectorTile(String id, String key, byte[] content) {
        VectorTileSource vectorTileSource = vectorTileMap.get(id);
        if (vectorTileSource == null) {
            vectorTileSource = new VectorTileSource();
            vectorTileSource.setId(id);
            vectorTileMap.put(id, vectorTileSource);
            delayQueue.offer(vectorTileSource);
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
            vectorTileSource.setId(id);
            vectorTileMap.put(id, vectorTileSource);
            delayQueue.offer(vectorTileSource);
        }
        vectorTileMap.get(id).getChannelList().addAll(channelList);
    }


    public void remove(String id) {
        VectorTileSource vectorTileSource = vectorTileMap.get(id);
        if (vectorTileSource != null) {
            delayQueue.remove(vectorTileSource);
        }
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
        delayQueue.remove(vectorTileSource);
        vectorTileMap.put("DEFAULT", vectorTileSource);
    }

}
