package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class VectorTileSource implements Delayed {

    /**
     * 抽稀的图层数据
     */
    private Map<String, byte[]> vectorTileMap = new ConcurrentHashMap<>();

    /**
     * 抽稀的原始数据
     */
    private List<CommonGBChannel> channelList = new ArrayList<>();

    private String id;

    /**
     * 创建时间， 大于6小时后删除
     */
    private long time;

    public VectorTileSource() {
        this.time = System.currentTimeMillis();
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(time + 6 * 60 * 60 * 1000 - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }
}
