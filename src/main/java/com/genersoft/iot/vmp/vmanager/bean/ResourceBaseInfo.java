package com.genersoft.iot.vmp.vmanager.bean;

public class ResourceBaseInfo {
    private int total;
    private int online;

    public ResourceBaseInfo() {
    }

    public ResourceBaseInfo(int total, int online) {
        this.total = total;
        this.online = online;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }
}
