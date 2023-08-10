package com.genersoft.iot.vmp.test;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

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

    @Autowired
    private VideoStreamSessionManager videoStreamSessionManager;

    @Test
    public void btest() {

        List<SsrcTransaction> ssrcTransactionForAll = videoStreamSessionManager.getSsrcTransactionForAll("34020000001320000001", null, null, null);
        System.out.println(JSON.toJSONString(ssrcTransactionForAll));
    }
}
