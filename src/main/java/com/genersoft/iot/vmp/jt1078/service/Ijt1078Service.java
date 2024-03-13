package com.genersoft.iot.vmp.jt1078.service;

import com.genersoft.iot.vmp.jt1078.bean.JTDevice;

public interface Ijt1078Service {
    JTDevice getDevice(String devId);

    void updateDevice(JTDevice deviceInDb);
}
