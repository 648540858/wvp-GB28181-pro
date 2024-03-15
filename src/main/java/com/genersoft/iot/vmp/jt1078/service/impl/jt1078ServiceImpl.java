package com.genersoft.iot.vmp.jt1078.service.impl;

import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.dao.JTDeviceMapper;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class jt1078ServiceImpl implements Ijt1078Service {

    @Autowired
    private JTDeviceMapper jtDeviceMapper;


    @Override
    public JTDevice getDevice(String terminalId) {
        return jtDeviceMapper.getDevice(terminalId);
    }

    @Override
    public void updateDevice(JTDevice device) {
        device.setUpdateTime(DateUtil.getNow());
        jtDeviceMapper.updateDevice(device);
    }

    @Override
    public PageInfo<JTDevice> getDeviceList(int page, int count, String query, Boolean online) {
        PageHelper.startPage(page, count);
        List<JTDevice> all = jtDeviceMapper.getDeviceList(query, online);
        return new PageInfo<>(all);
    }

    @Override
    public void addDevice(JTDevice device) {
        device.setCreateTime(DateUtil.getNow());
        device.setUpdateTime(DateUtil.getNow());
        jtDeviceMapper.addDevice(device);
    }

    @Override
    public void deleteDeviceByDeviceId(String deviceId) {
        jtDeviceMapper.deleteDeviceByTerminalId(deviceId);
    }

    @Override
    public void updateDeviceStatus(boolean connected, String terminalId) {
        jtDeviceMapper.updateDeviceStatus(connected, terminalId);
    }
}
