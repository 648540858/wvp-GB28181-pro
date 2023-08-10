package com.genersoft.iot.vmp.test;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author weidian
 * @date 2023/8/10
 */
@SpringBootTest
public class BaseTest {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    @Test
    public void atest() {
        Device device = deviceMapper.getDeviceByDeviceId("34020000001320000001");
        if (device == null) {
            return;
        }

        device.setOnLine(false);
        deviceChannelMapper.offlineByDeviceId("34020000001320000001");
        deviceMapper.update(device);
    }
}
