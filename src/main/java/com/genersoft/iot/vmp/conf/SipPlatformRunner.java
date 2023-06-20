package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatformCatch;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.service.IPlatformService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统启动时控制上级平台重新注册
 * @author lin
 */
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
        List<ParentPlatform> parentPlatforms = storager.queryEnableParentPlatformList(true);

        for (ParentPlatform parentPlatform : parentPlatforms) {

            ParentPlatformCatch parentPlatformCatchOld = redisCatchStorage.queryPlatformCatchInfo(parentPlatform.getServerGBId());

            // 更新缓存
            ParentPlatformCatch parentPlatformCatch = new ParentPlatformCatch();
            parentPlatformCatch.setParentPlatform(parentPlatform);
            parentPlatformCatch.setId(parentPlatform.getServerGBId());
            redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
            if (parentPlatformCatchOld != null) {
                // 取消订阅
                sipCommanderForPlatform.unregister(parentPlatform, parentPlatformCatchOld.getSipTransactionInfo(), null, (eventResult)->{
                    platformService.login(parentPlatform);
                });
            }

            // 设置所有平台离线
            platformService.offline(parentPlatform, false);
        }
    }
}
