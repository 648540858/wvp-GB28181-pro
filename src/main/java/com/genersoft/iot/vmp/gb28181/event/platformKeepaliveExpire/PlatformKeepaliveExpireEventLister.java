package com.genersoft.iot.vmp.gb28181.event.platformKeepaliveExpire;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatformCatch;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import javax.sip.message.Response;

/**
 * @description: 平台心跳超时事件
 * @author: panll
 * @date: 2020年11月5日 10:00
 */
@Component
public class PlatformKeepaliveExpireEventLister implements ApplicationListener<PlatformKeepaliveExpireEvent> {


    private final static Logger logger = LoggerFactory.getLogger(PlatformKeepaliveExpireEventLister.class);

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private ISIPCommanderForPlatform sipCommanderForPlatform;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Autowired
    private EventPublisher publisher;

    @Override
    public void onApplicationEvent(@NotNull PlatformKeepaliveExpireEvent event) {

        if (logger.isDebugEnabled()) {
            logger.debug("平台心跳到期事件事件触发，平台国标ID：" + event.getPlatformGbID());
        }
        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(event.getPlatformGbID());
        ParentPlatformCatch parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(event.getPlatformGbID());
        if (parentPlatformCatch == null) {
            return;
        }
        if (parentPlatform == null) {
            logger.debug("平台心跳到期事件事件触发，但平台已经删除!!! 平台国标ID：" + event.getPlatformGbID());
            return;
        }
        parentPlatformCatch.setParentPlatform(parentPlatform);
        // 发送心跳
        if (parentPlatformCatch.getKeepAliveReply() >= 3) {
            // 有3次未收到心跳回复, 设置平台状态为离线, 开始重新注册
            logger.warn("有3次未收到心跳回复,标记设置平台状态为离线, 并重新注册 平台国标ID：" + event.getPlatformGbID());
            storager.updateParentPlatformStatus(event.getPlatformGbID(), false);
            publisher.platformNotRegisterEventPublish(event.getPlatformGbID());
            parentPlatformCatch.setKeepAliveReply(0);
            redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
        }else {
            // 再次发送心跳
            String callId = sipCommanderForPlatform.keepalive(parentPlatform);

            parentPlatformCatch.setKeepAliveReply( parentPlatformCatch.getKeepAliveReply() + 1);
            // 存储心跳信息, 并设置状态为未回复, 如果多次过期仍未收到回复,则认为上级平台已经离线
            redisCatchStorage.updatePlatformKeepalive(parentPlatform);
            redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);

            sipSubscribe.addOkSubscribe(callId, (SipSubscribe.EventResult eventResult) ->{
                if (eventResult.statusCode == Response.OK) {
                    // 收到心跳响应信息,
                    parentPlatformCatch.setKeepAliveReply(0);
                    redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
                }
            } );
        }
    }
}
