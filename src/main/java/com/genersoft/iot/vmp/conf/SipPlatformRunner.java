package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.PlatformCatch;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
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
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private ISIPCommanderForPlatform sipCommanderForPlatform;

    @Autowired
    private UserSetting userSetting;

    @Override
    public void run(String... args) throws Exception {
        // 获取所有启用的平台
        List<Platform> parentPlatforms = platformService.queryEnablePlatformList(userSetting.getServerId());

        for (Platform platform : parentPlatforms) {

            PlatformCatch platformCatchOld = redisCatchStorage.queryPlatformCatchInfo(platform.getServerGBId());

            // 更新缓存
            PlatformCatch platformCatch = new PlatformCatch();
            platformCatch.setPlatform(platform);
            platformCatch.setId(platform.getServerGBId());
            redisCatchStorage.updatePlatformCatchInfo(platformCatch);
            if (platformCatchOld != null) {
                // 取消订阅
                try {
                    log.info("[平台主动注销] {}({})", platform.getName(), platform.getServerGBId());
                    sipCommanderForPlatform.unregister(platform, platformCatchOld.getSipTransactionInfo(), null, (eventResult)->{
                        platformService.login(platform);
                    });
                } catch (Exception e) {
                    log.error("[命令发送失败] 国标级联 注销: {}", e.getMessage());
                    platformService.offline(platform, true);
                    continue;
                }
            }else {
                platformService.login(platform);
            }

            // 设置平台离线
            platformService.offline(platform, false);
        }
    }
}
