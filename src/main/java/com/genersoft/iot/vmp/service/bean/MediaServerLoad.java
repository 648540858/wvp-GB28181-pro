package com.genersoft.iot.vmp.service.bean;

public class MediaServerLoad {

    private String id;
    private int push;
    private int proxy;
    private int gbReceive;
    private int gbSend;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPush() {
        return push;
    }

    public void setPush(int push) {
        this.push = push;
    }

    public int getProxy() {
        return proxy;
    }

    public void setProxy(int proxy) {
        this.proxy = proxy;
    }

    public int getGbReceive() {
        return gbReceive;
    }

    public void setGbReceive(int gbReceive) {
        this.gbReceive = gbReceive;
    }

    public int getGbSend() {
        return gbSend;
    }

    public void setGbSend(int gbSend) {
        this.gbSend = gbSend;
    }
}
