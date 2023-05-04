package com.genersoft.iot.vmp.jt1078.config;

import com.genersoft.iot.vmp.jt1078.cmd.JT1078Template;
import com.genersoft.iot.vmp.jt1078.proc.response.*;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * curl http://localhost:18080/api/jt1078/start/live/18864197066/1
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:12
 * @email qingtaij@163.com
 */
@ConditionalOnProperty(value = "jt1078.enable", havingValue = "true")
@RestController
@RequestMapping("/api/jt1078")
public class JT1078Controller {

    @Resource
    JT1078Template jt1078Template;

    /**
     * jt1078Template 调用示例
     */
    @GetMapping("/start/live/{deviceId}/{channelId}")
    public WVPResult<?> startLive(@PathVariable String deviceId, @PathVariable String channelId) {
        J9101 j9101 = new J9101();
        j9101.setChannel(Integer.valueOf(channelId));
        j9101.setIp("192.168.1.1");
        j9101.setRate(1);
        j9101.setTcpPort(7618);
        j9101.setUdpPort(7618);
        j9101.setType(0);
        // TODO 分配ZLM,获取IP、端口
        String s = jt1078Template.startLive(deviceId, j9101, 6);
        // TODO 设备响应成功后,封装拉流结果集
        WVPResult<String> wvpResult = new WVPResult<>();
        wvpResult.setCode(200);
        wvpResult.setData(String.format("http://192.168.1.1/rtp/%s_%s.live.mp4", deviceId, channelId));
        return wvpResult;
    }

}

