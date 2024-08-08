package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

import java.time.Instant;
import java.util.List;

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
    private List<DeviceChannel> channelList;
    private List<Region> regionListList;
    private List<Group> groupListListList;
    private Instant lastTime;
    private Device device;
    private String errorMsg;

    public enum CatalogDataStatus{
        ready, runIng, end
    }
    private CatalogDataStatus status;
}
