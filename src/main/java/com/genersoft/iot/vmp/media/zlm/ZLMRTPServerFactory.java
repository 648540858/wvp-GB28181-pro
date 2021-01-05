package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ZLMRTPServerFactory {

    private Logger logger = LoggerFactory.getLogger("ZLMRTPServerFactory");

    @Value("${media.rtp.udpPortRange}")
    private String udpPortRange;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    private int[] udpPortRangeArray = new int[2];

    private int currentPort = 0;

    public int createRTPServer(String streamId) {
        Map<String, Object> param = new HashMap<>();
        int result = -1;
        int newPort = getPortFromUdpPortRange();
        param.put("port", newPort);
        param.put("enable_tcp", 1);
        param.put("stream_id", streamId);
        JSONObject jsonObject = zlmresTfulUtils.openRtpServer(param);
        System.out.println(jsonObject);

        if (jsonObject != null) {
            switch (jsonObject.getInteger("code")){
                case 0:
                    result= newPort;
                    break;
                case -300: // id已经存在
                    result = newPort;
                    break;
                case -400: // 端口占用
                    result= createRTPServer(streamId);
                    break;
                default:
                    logger.error("创建RTP Server 失败: " + jsonObject.getString("msg"));
                    break;
            }
        }else {
            //  检查ZLM状态
            logger.error("创建RTP Server 失败: 请检查ZLM服务");
        }
        return result;
    }

    public boolean closeRTPServer(String streamId) {
        boolean result = false;
        Map<String, Object> param = new HashMap<>();
        param.put("stream_id", streamId);
        JSONObject jsonObject = zlmresTfulUtils.closeRtpServer(param);
        if (jsonObject != null ) {
            if (jsonObject.getInteger("code") == 0) {
                result = jsonObject.getInteger("hit") == 1;
            }else {
                logger.error("关闭RTP Server 失败: " + jsonObject.getString("msg"));
            }
        }else {
            //  检查ZLM状态
            logger.error("关闭RTP Server 失败: 请检查ZLM服务");
        }
        return result;
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
