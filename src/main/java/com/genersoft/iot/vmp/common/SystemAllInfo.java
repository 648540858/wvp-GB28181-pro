package com.genersoft.iot.vmp.common;

import java.util.List;

public class SystemAllInfo {

    private List<Object> cpu;
    private List<Object> mem;
    private List<Object> net;

    private long netTotal;

    private Object disk;

    public List<Object> getCpu() {
        return cpu;
    }

    public void setCpu(List<Object> cpu) {
        this.cpu = cpu;
    }

    public List<Object> getMem() {
        return mem;
    }

    public void setMem(List<Object> mem) {
        this.mem = mem;
    }

    public List<Object> getNet() {
        return net;
    }

    public void setNet(List<Object> net) {
        this.net = net;
    }

    public Object getDisk() {
        return disk;
    }

    public void setDisk(Object disk) {
        this.disk = disk;
    }

    public long getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(long netTotal) {
        this.netTotal = netTotal;
    }
}
