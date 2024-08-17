package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.PlatformCatch;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统启动时控制上级平台重新注册
 * @author lin
 */
@Slf4j
@Component
@Order(value=13)
public class SipPlatformRunner implements CommandLineRunner {

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private ISIPCommanderForPlatform sipCommanderForPlatform;

    @Override
    public void run(String... args) throws Exception {
        // 获取所有启用的平台
        List<Platform> parentPlatforms = platformService.queryEnablePlatformList();

        for (Platform platform : parentPlatforms) {

            PlatformCatch parentPlatformCatchOld = redisCatchStorage.queryPlatformCatchInfo(platform.getServerGBId());

            // 更新缓存
            PlatformCatch parentPlatformCatch = new PlatformCatch();
            parentPlatformCatch.setPlatform(platform);
            parentPlatformCatch.setId(platform.getServerGBId());
            redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
            if (parentPlatformCatchOld != null) {
                // 取消订阅
                try {
                    log.info("[平台主动注销] {}({})", platform.getName(), platform.getServerGBId());
                    sipCommanderForPlatform.unregister(platform, parentPlatformCatchOld.getSipTransactionInfo(), null, (eventResult)->{
                        platformService.login(platform);
                    });
                } catch (Exception e) {
                    log.error("[命令发送失败] 国标级联 注销: {}", e.getMessage());
                    platformService.offline(platform, true);
                    continue;
                }
            }

            // 设置平台离线
            platformService.offline(platform, false);
        }
    }
}
