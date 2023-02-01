package com.genersoft.iot.vmp.gb28181.event.subscribe.catalog;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class CatalogEvent  extends ApplicationEvent {
    public CatalogEvent(Object source) {
        super(source);
    }

    /**
     * 上线
     */
    public static final String ON = "ON";

    /**
     * 离线
     */
    public static final String OFF = "OFF";

    /**
     * 视频丢失
     */
    public static final String VLOST = "VLOST";

    /**
     * 故障
     */
    public static final String DEFECT = "DEFECT";

    /**
     * 增加
     */
    public static final String ADD = "ADD";

    /**
     * 删除
     */
    public static final String DEL = "DEL";

    /**
     * 更新
     */
    public static final String UPDATE = "UPDATE";

    private List<DeviceChannel> deviceChannels;
    private List<GbStream> gbStreams;
    private String type;
    private String platformId;

    public List<DeviceChannel> getDeviceChannels() {
        return deviceChannels;
    }

    public void setDeviceChannels(List<DeviceChannel> deviceChannels) {
        this.deviceChannels = deviceChannels;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public List<GbStream> getGbStreams() {
        return gbStreams;
    }

    public void setGbStreams(List<GbStream> gbStreams) {
        this.gbStreams = gbStreams;
    }
}
