package com.genersoft.iot.vmp.jt1078.service.impl;

import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.dao.JTDeviceMapper;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class jt1078ServiceImpl implements Ijt1078Service {

    @Autowired
    private JTDeviceMapper jtDeviceMapper;


    @Override
    public JTDevice getDevice(String devId) {
        return jtDeviceMapper.getDevice(devId);
    }

    @Override
    public void updateDevice(JTDevice device) {
        jtDeviceMapper.updateDevice(device);
    }
}
