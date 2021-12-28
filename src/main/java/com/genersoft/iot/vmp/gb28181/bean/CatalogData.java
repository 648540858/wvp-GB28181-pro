package com.genersoft.iot.vmp.gb28181.bean;

import java.util.Date;
import java.util.List;

public class CatalogData {
    private int total;
    private List<DeviceChannel> channelList;
    private Date lastTime;
    private Device device;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<DeviceChannel> getChannelList() {
        return channelList;
    }

    public void setChannelList(List<DeviceChannel> channelList) {
        this.channelList = channelList;
    }

    public Date getLastTime() {
        return lastTime;
    }

    public void setLastTime(Date lastTime) {
        this.lastTime = lastTime;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }
}
