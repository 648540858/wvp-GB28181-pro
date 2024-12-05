package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lin
 */
@Data
public class CatalogData {
    /**
     * 命令序列号
     */
    private int sn;
    private Integer total;
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
