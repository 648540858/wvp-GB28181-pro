package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.utils.redis.JedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Set;


/**
 * 对配置文件进行校验
 */
@Component
@Order(value = 0)
public class ApplicationCheckRunner implements CommandLineRunner {

    private Logger logger = LoggerFactory.getLogger("ApplicationCheckRunner");
    @Autowired
    SipConfig sipConfig;
    @Autowired
    MediaConfig mediaConfig;
    @Autowired
    JedisUtil jedisUtil;

    @Override
    public void run(String... args) {
        String sipIp = sipConfig.getSipIp();
        if (sipIp.equals("localhost") || sipIp.equals("127.0.0.1")) {
            logger.error("sip.ip不能使用 {} ,请使用类似192.168.1.44这样的来自网卡的IP!!!", sipIp);
            System.exit(1);
        }
        String mediaIp = mediaConfig.getMediaIp();
        String[] mediaIpArr = mediaIp.split(",");
        mediaConfig.setMediaIpArr(mediaIpArr);

        for (String mId : mediaIpArr) {
            if (mId.equals("localhost") || mId.equals("127.0.0.1")) {
                logger.warn("mediaIp.ip使用localhost或127.0.0.1，将无法收到网络内其他设备的推流!!!");
            }
        }

        HashMap mediaServerSsrcMap = new HashMap<>(mediaIpArr.length);
        for (int i = 0; i < mediaIpArr.length; i++) {
            String mIp = mediaIpArr[i];
            SsrcConfig ssrcConfig = new SsrcConfig();
            Set<String> usedSet = jedisUtil.smembers(VideoManagerConstants.MEDIA_SSRC_USED_PREFIX + mIp);
            ssrcConfig.init(mIp, usedSet);
            mediaServerSsrcMap.put(mIp, ssrcConfig);
        }
        mediaConfig.setMediaServerSsrcMap(mediaServerSsrcMap);
    }
}
