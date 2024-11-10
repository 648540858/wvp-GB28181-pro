package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author lin
 */
@Data
public class CatalogData {
    /**
     * 命令序列号
     */
    private int sn;
    private int total;
    private Instant time;
    private Device device;
    private String errorMsg;
    private Set<String> redisKeysForChannel = new HashSet<>();
    private Set<String> redisKeysForRegion = new HashSet<>();
    private Set<String> redisKeysForGroup = new HashSet<>();

    public enum CatalogDataStatus{
        ready, runIng, end
    }
    private CatalogDataStatus status;

}
