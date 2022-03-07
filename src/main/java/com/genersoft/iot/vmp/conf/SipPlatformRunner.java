package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatformCatch;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统启动时控制上级平台重新注册
 */
@Component
@Order(value=3)
public class SipPlatformRunner implements CommandLineRunner {

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private EventPublisher publisher;

    @Autowired
    private ISIPCommanderForPlatform sipCommanderForPlatform;


    @Override
    public void run(String... args) throws Exception {
        // 设置所有平台离线
        storager.outlineForAllParentPlatform();

        // 清理所有平台注册缓存
        redisCatchStorage.cleanPlatformRegisterInfos();

        // 停止所有推流
//        zlmrtpServerFactory.closeAllSendRtpStream();

        List<ParentPlatform> parentPlatforms = storager.queryEnableParentPlatformList(true);

        for (ParentPlatform parentPlatform : parentPlatforms) {
            redisCatchStorage.updatePlatformRegister(parentPlatform);

            redisCatchStorage.updatePlatformKeepalive(parentPlatform);

            ParentPlatformCatch parentPlatformCatch = new ParentPlatformCatch();

            parentPlatformCatch.setParentPlatform(parentPlatform);
            parentPlatformCatch.setId(parentPlatform.getServerGBId());
            redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);

            // 取消订阅
            sipCommanderForPlatform.unregister(parentPlatform, null, (eventResult)->{
                ParentPlatform platform = storager.queryParentPlatByServerGBId(parentPlatform.getServerGBId());
                sipCommanderForPlatform.register(platform, null, null);
            });

            // 发送平台未注册消息
            publisher.platformNotRegisterEventPublish(parentPlatform.getServerGBId());
        }
    }
}
