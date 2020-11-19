package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ZLMUtils {

    @Value("${media.rtp.udpPortRange}")
    private String udpPortRange;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    private int[] udpPortRangeArray = new int[2];

    private int currentPort = 0;

    public int getNewRTPPort(String ssrc) {
        String streamId = String.format("%08x", Integer.parseInt(ssrc)).toUpperCase();
        Map<String, Object> param = new HashMap<>();
        int newPort = getPortFromUdpPortRange();
        param.put("port", newPort);
        param.put("enable_tcp", 1);
        param.put("stream_id", streamId);
        JSONObject jsonObject = zlmresTfulUtils.openRtpServer(param);
        if (jsonObject != null && jsonObject.getInteger("code") == 0) {
            return newPort;
        } else {
            return getNewRTPPort(ssrc);
        }
    }

    private int getPortFromUdpPortRange() {
        if (currentPort == 0) {
            String[] udpPortRangeStrArray = udpPortRange.split(",");
            udpPortRangeArray[0] = Integer.parseInt(udpPortRangeStrArray[0]);
            udpPortRangeArray[1] = Integer.parseInt(udpPortRangeStrArray[1]);
        }

        if (currentPort == 0 || currentPort++ > udpPortRangeArray[1]) {
            currentPort = udpPortRangeArray[0];
            return udpPortRangeArray[0];
        } else {
            if (currentPort % 2 == 1) {
                currentPort++;
            }
            return currentPort++;
        }
    }
}
