package com.genersoft.iot.vmp.gb28181.bean;

public interface PlatformKeepaliveCallback {
    public void run(String platformServerGbId, int failCount);
}
