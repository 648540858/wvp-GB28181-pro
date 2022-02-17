package com.genersoft.iot.vmp.gb28181.event.subscribe.catalog;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class CatalogEvent  extends ApplicationEvent {
    public CatalogEvent(Object source) {
        super(source);
    }

    public static final String ON = "ON";         // 上线
    public static final String OFF = "OFF";       // 离线
    public static final String VLOST = "VLOST";   // 视频丢失
    public static final String DEFECT = "DEFECT"; // 故障
    public static final String ADD = "ADD";       // 增加
    public static final String DEL = "DEL";       // 删除
    public static final String UPDATE = "UPDATE";       // 更新

    private List<DeviceChannel> deviceChannels;
    private GbStream[] gbStreams;
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

    public GbStream[] getGbStreams() {
        return gbStreams;
    }

    public void setGbStreams(GbStream[] gbStreams) {
        this.gbStreams = gbStreams;
    }
}
