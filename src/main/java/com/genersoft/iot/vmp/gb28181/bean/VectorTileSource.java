package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class VectorTileSource {

    /**
     * 抽稀的图层数据
     */
    private Map<String, byte[]> vectorTileMap = new ConcurrentHashMap<>();

    /**
     * 抽稀的原始数据
     */
    private List<CommonGBChannel> channelList = new ArrayList<>();

    /**
     * 创建时间， 大于6小时后删除
     */
    private long time;

    public VectorTileSource() {
        this.time = System.currentTimeMillis();
    }
}
